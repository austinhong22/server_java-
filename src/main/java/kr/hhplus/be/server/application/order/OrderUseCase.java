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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    public OrderResult execute(OrderCommand command) {
        // 1. 사용자 조회 (비관적 락)
        User user = userRepository.findByIdWithLock(command.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 상품 조회 및 재고 확인 (비관적 락)
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

        // 6. 잔액 확인
        if (!user.hasEnoughBalance(finalAmount)) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        // 7. 주문 생성
        Order order = Order.builder()
                .user(user)
                .totalAmount(command.getTotalAmount())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();

        orderItems.forEach(order::addOrderItem);
        orderRepository.save(order);

        // 8. 결제 처리
        Payment payment = Payment.builder()
                .order(order)
                .amount(finalAmount)
                .build();
        paymentRepository.save(payment);

        try {
            // 9. 결제 게이트웨이를 통한 결제 처리
            paymentGateway.processPayment(user.getId(), finalAmount);

            // 10. 사용자 잔액 차감
            user.deductBalance(finalAmount);
            userRepository.save(user);

            // 11. 쿠폰 사용 처리
            if (command.getCouponId() != null) {
                Coupon coupon = couponRepository.findByIdAndUserId(command.getCouponId(), command.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
                coupon.use();
                couponRepository.save(coupon);
            }

            // 12. 주문 완료 처리
            order.complete();
            payment.complete();
            orderRepository.save(order);
            paymentRepository.save(payment);

            // 13. 데이터 플랫폼에 주문 정보 전송
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

    private List<OrderItem> createOrderItems(List<Product> products, List<OrderItemCommand> orderItemCommands) {
        return orderItemCommands.stream()
                .map(itemCommand -> {
                    Product product = products.stream()
                            .filter(p -> p.getId().equals(itemCommand.getProductId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

                    // 재고 확인 및 차감
                    Product lockedProduct = productRepository.findByIdWithLock(product.getId())
                            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

                    if (!lockedProduct.hasStock(itemCommand.getQuantity())) {
                        throw new IllegalArgumentException("재고가 부족합니다: " + lockedProduct.getName());
                    }

                    lockedProduct.decreaseStock(itemCommand.getQuantity());
                    productRepository.save(lockedProduct);

                    return OrderItem.builder()
                            .product(lockedProduct)
                            .quantity(itemCommand.getQuantity())
                            .price(lockedProduct.getPrice())
                            .build();
                })
                .toList();
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
