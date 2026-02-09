package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.infrastructure.payment.PaymentGateway;
import kr.hhplus.be.server.infrastructure.event.EventPublisher;
import kr.hhplus.be.server.infrastructure.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderUseCase {

	private static final long LOCK_WAIT_TIME = 5000; // 5초
	private static final long LOCK_LEASE_TIME = 10000; // 10초

	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final PaymentRepository paymentRepository;
	private final CouponRepository couponRepository;
	private final PaymentGateway paymentGateway;
	private final EventPublisher eventPublisher;
	private final DistributedLock distributedLock;

	/**
	 * 주문 및 결제 처리
	 * 분산락을 사용하여 동시성 제어를 수행합니다.
	 * 
	 * 분산락과 DB 트랜잭션 혼용 시 주의사항:
	 * 1. 분산락은 트랜잭션 밖에서 획득해야 합니다 (트랜잭션 내에서 획득하면 커밋 전에 락이 해제될 수 있음)
	 * 2. 락의 범위를 최소화하여 성능 저하를 방지합니다
	 * 3. 락 해제는 finally 블록에서 보장됩니다
	 * 
	 * 락 키 전략:
	 * - 사용자별 주문 락: order:user:{userId} - 동일 사용자의 중복 주문 방지
	 * - 상품별 재고 락: product:stock:{productId} - 재고 차감 시 동시성 제어
	 * - 사용자별 잔액 락: user:balance:{userId} - 잔액 차감 시 동시성 제어
	 * - 쿠폰별 사용 락: coupon:use:{couponId} - 쿠폰 중복 사용 방지
	 */
	public OrderResult execute(OrderCommand command) {
		// 사용자별 주문 락을 사용하여 동일 사용자의 중복 주문을 방지
		// 여러 상품을 주문하는 경우에도 사용자 단위로 직렬화하여 데드락 방지
		String userOrderLockKey = "order:user:" + command.getUserId();
		
		return distributedLock.executeWithLock(
			userOrderLockKey,
			LOCK_WAIT_TIME,
			LOCK_LEASE_TIME,
			() -> executeOrder(command)
		);
	}

	@Transactional
	private OrderResult executeOrder(OrderCommand command) {
        // 1. 사용자 조회
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 상품 조회 및 재고 확인
        List<Product> products = productRepository.findAllByIds(command.getProductIds());
        if (products.size() != command.getProductIds().size()) {
            throw new IllegalArgumentException("일부 상품을 찾을 수 없습니다.");
        }

        // 3. 재고 차감 및 주문 항목 생성
        List<OrderItem> orderItems = createOrderItems(products, command.getOrderItems());

        // 4. 쿠폰 할인 계산
        Long discountAmount = calculateDiscount(command.getCouponId(), command.getTotalAmount());

        // 5. 최종 금액 계산
        Long finalAmount = command.getTotalAmount() - discountAmount;

        // 6. 주문 생성
        Order order = Order.builder()
                .user(user)
                .totalAmount(command.getTotalAmount())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();

        orderItems.forEach(order::addOrderItem);
        orderRepository.save(order);

        // 7. 결제 처리
        Payment payment = Payment.builder()
                .order(order)
                .amount(finalAmount)
                .build();
        paymentRepository.save(payment);

		try {
			// 8. 결제 게이트웨이를 통한 결제 처리
			paymentGateway.processPayment(user.getId(), finalAmount);

			// 9. 사용자별 잔액 락을 사용하여 잔액 차감 시 동시성 제어
			String balanceLockKey = "user:balance:" + command.getUserId();
			distributedLock.executeWithLock(
				balanceLockKey,
				LOCK_WAIT_TIME,
				LOCK_LEASE_TIME,
				() -> {
					// 조건부 UPDATE를 사용한 사용자 잔액 차감 (동시성 제어)
					// 분산락과 DB 트랜잭션을 함께 사용하여 이중 보호
					int updatedRows = userRepository.deductBalanceIfAvailable(command.getUserId(), finalAmount);
					if (updatedRows == 0) {
						throw new IllegalArgumentException("잔액이 부족합니다.");
					}
				}
			);

			// 10. 쿠폰 사용 처리
			if (command.getCouponId() != null) {
				// 쿠폰별 사용 락을 사용하여 쿠폰 중복 사용 방지
				String couponLockKey = "coupon:use:" + command.getCouponId();
				distributedLock.executeWithLock(
					couponLockKey,
					LOCK_WAIT_TIME,
					LOCK_LEASE_TIME,
					() -> {
						Coupon coupon = couponRepository.findByIdAndUserId(command.getCouponId(), command.getUserId())
							.orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
						coupon.use();
						couponRepository.save(coupon);
					}
				);
			}

			// 11. 주문 완료 처리
			order.complete();
			payment.complete();
			orderRepository.save(order);
			paymentRepository.save(payment);

			// 12. 데이터 플랫폼에 주문 정보 전송
			eventPublisher.publishOrderCompleted(order);

			return OrderResult.success(order.getId(), finalAmount);
		} catch (Exception e) {
			// 결제 실패 시 롤백
			payment.fail();
			paymentRepository.save(payment);
			order.cancel();
			orderRepository.save(order);
			throw new RuntimeException("결제 처리 중 오류가 발생했습니다.", e);
		}
	}

	/**
	 * 주문 항목 생성 및 재고 차감
	 * 각 상품별로 분산락을 사용하여 재고 차감 시 동시성 제어를 수행합니다.
	 * 상품 ID를 정렬하여 락을 획득함으로써 데드락을 방지합니다.
	 */
	private List<OrderItem> createOrderItems(List<Product> products, List<OrderItemCommand> orderItemCommands) {
		// 데드락 방지를 위해 상품 ID를 정렬하여 락 획득 순서를 보장
		List<OrderItemCommand> sortedCommands = orderItemCommands.stream()
			.sorted((a, b) -> Long.compare(a.getProductId(), b.getProductId()))
			.collect(Collectors.toList());

		return sortedCommands.stream()
			.map(itemCommand -> {
				Product product = products.stream()
					.filter(p -> p.getId().equals(itemCommand.getProductId()))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

				// 상품별 재고 락을 사용하여 재고 차감 시 동시성 제어
				String stockLockKey = "product:stock:" + product.getId();
				
				return distributedLock.executeWithLock(
					stockLockKey,
					LOCK_WAIT_TIME,
					LOCK_LEASE_TIME,
					() -> {
						// 조건부 UPDATE를 사용한 재고 차감 (동시성 제어)
						// 분산락과 DB 트랜잭션을 함께 사용하여 이중 보호
						int updatedRows = productRepository.decreaseStockIfAvailable(
							product.getId(),
							itemCommand.getQuantity()
						);

						if (updatedRows == 0) {
							throw new IllegalArgumentException("재고가 부족합니다: " + product.getName());
						}

						// 업데이트된 상품 정보 조회
						Product updatedProduct = productRepository.findById(product.getId())
							.orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

						return OrderItem.builder()
							.product(updatedProduct)
							.quantity(itemCommand.getQuantity())
							.price(updatedProduct.getPrice())
							.build();
					}
				);
			})
			.collect(Collectors.toList());
	}

	private Long calculateDiscount(Long couponId, Long totalAmount) {
		if (couponId == null) {
			return 0L;
		}

		Coupon coupon = couponRepository.findById(couponId)
			.orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

		return coupon.calculateDiscount(totalAmount);
	}
}
