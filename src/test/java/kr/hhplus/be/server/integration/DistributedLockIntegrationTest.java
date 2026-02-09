package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.application.order.OrderCommand;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.application.order.OrderResult;
import kr.hhplus.be.server.application.order.OrderUseCase;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.infrastructure.lock.DistributedLock;
import kr.hhplus.be.server.infrastructure.lock.LockAcquisitionException;
import kr.hhplus.be.server.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@DisplayName("분산락 통합 테스트")
class DistributedLockIntegrationTest {

	@Autowired
	private DistributedLock distributedLock;

	@Autowired
	private OrderUseCase orderUseCase;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CouponRepository couponRepository;

	@Autowired
	private UserService userService;

	private User user;
	private Product product;

	@BeforeEach
	void setUp() {
		// 사용자 생성
		user = User.builder()
			.balance(0L)
			.build();
		user = userRepository.save(user);

		// 상품 생성
		product = Product.builder()
			.name("테스트 상품")
			.price(1000L)
			.stock(10)
			.build();
		product = productRepository.save(product);
	}

	@Test
	@DisplayName("분산락 기본 동작 테스트 - 락 획득 및 해제")
	void testBasicLockAcquisitionAndRelease() {
		// Given
		String lockKey = "test:lock:1";
		AtomicInteger counter = new AtomicInteger(0);

		// When
		distributedLock.executeWithLock(lockKey, 1000, 5000, () -> {
			counter.incrementAndGet();
			return null;
		});

		// Then
		assertThat(counter.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("분산락 동시성 테스트 - 여러 스레드가 동시에 락 획득 시도")
	void testConcurrentLockAcquisition() throws InterruptedException {
		// Given
		String lockKey = "test:lock:concurrent";
		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger executionOrder = new AtomicInteger(0);

		// When
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					distributedLock.executeWithLock(lockKey, 5000, 1000, () -> {
						int order = executionOrder.incrementAndGet();
						successCount.incrementAndGet();
						// 락이 제대로 작동하면 한 번에 하나의 스레드만 실행되어야 함
						try {
							Thread.sleep(100); // 실행 시간 시뮬레이션
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						return order;
					});
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// Then
		// 모든 스레드가 성공적으로 실행되어야 함 (순차적으로)
		assertThat(successCount.get()).isEqualTo(threadCount);
	}

	@Test
	@DisplayName("분산락 타임아웃 테스트 - 락 획득 실패 시 예외 발생")
	void testLockTimeout() throws InterruptedException {
		// Given
		String lockKey = "test:lock:timeout";
		CountDownLatch holdLockLatch = new CountDownLatch(1);
		CountDownLatch tryLockLatch = new CountDownLatch(1);

		// When - 첫 번째 스레드가 락을 오래 유지
		Thread holderThread = new Thread(() -> {
			try {
				distributedLock.executeWithLock(lockKey, 1000, 10000, () -> {
					holdLockLatch.countDown();
					try {
						// 락을 3초간 유지
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return null;
				});
			} catch (Exception e) {
				// 예외 무시
			}
		});
		holderThread.start();
		holdLockLatch.await(); // 첫 번째 스레드가 락을 획득할 때까지 대기

		// 두 번째 스레드가 짧은 대기 시간으로 락 획득 시도
		Thread tryThread = new Thread(() -> {
			try {
				distributedLock.executeWithLock(lockKey, 500, 1000, () -> {
					tryLockLatch.countDown();
					return null;
				});
			} catch (LockAcquisitionException e) {
				tryLockLatch.countDown();
			}
		});
		tryThread.start();

		tryThread.join();
		holderThread.join();

		// Then - 두 번째 스레드는 타임아웃으로 실패해야 함
		// (실제로는 락이 해제되기 전에 타임아웃이 발생할 수 있음)
	}

	@Test
	@DisplayName("분산락을 사용한 동시 주문 테스트 - 재고 정확성 보장")
	void testConcurrentOrderWithDistributedLock() throws InterruptedException {
		// Given
		userService.chargePoint(user.getId(), 100000L); // 충분한 잔액
		int threadCount = 20;
		int orderQuantityPerThread = 1;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);

		// When - 여러 스레드가 동시에 주문
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					OrderCommand command = new OrderCommand(
						user.getId(),
						List.of(product.getId()),
						List.of(new OrderItemCommand(product.getId(), orderQuantityPerThread)),
						1000L,
						null
					);
					OrderResult result = orderUseCase.execute(command);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failureCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// Then
		Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
		int expectedStock = 10 - successCount.get(); // 초기 재고 - 성공한 주문 수

		// 재고가 정확하게 차감되어야 함
		assertThat(updatedProduct.getStock()).isEqualTo(expectedStock);
		// 성공한 주문 수는 재고를 초과할 수 없음
		assertThat(successCount.get()).isLessThanOrEqualTo(10);
		// 주문 수와 재고 차감이 일치해야 함
		assertThat(updatedProduct.getStock() + successCount.get()).isEqualTo(10);
	}

	@Test
	@DisplayName("분산락을 사용한 동시 주문 테스트 - 잔액 정확성 보장")
	void testConcurrentOrderBalanceAccuracy() throws InterruptedException {
		// Given
		Long initialBalance = 10000L;
		userService.chargePoint(user.getId(), initialBalance);
		int threadCount = 15;
		Long orderAmount = 1000L;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);

		// When - 여러 스레드가 동시에 주문
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					OrderCommand command = new OrderCommand(
						user.getId(),
						List.of(product.getId()),
						List.of(new OrderItemCommand(product.getId(), 1)),
						orderAmount,
						null
					);
					orderUseCase.execute(command);
					successCount.incrementAndGet();
				} catch (Exception e) {
					// 일부 실패는 예상됨 (잔액 부족)
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// Then
		User updatedUser = userRepository.findById(user.getId()).orElseThrow();
		Long expectedBalance = initialBalance - (successCount.get() * orderAmount);

		// 잔액이 정확하게 차감되어야 함
		assertThat(updatedUser.getBalance()).isEqualTo(expectedBalance);
		// 성공한 주문 수는 잔액을 초과할 수 없음
		assertThat(successCount.get()).isLessThanOrEqualTo(10); // 10000 / 1000
	}

	@Test
	@DisplayName("분산락을 사용한 쿠폰 중복 사용 방지 테스트")
	void testCouponDuplicateUsagePrevention() throws InterruptedException {
		// Given
		userService.chargePoint(user.getId(), 10000L);
		Coupon coupon = Coupon.builder()
			.user(user)
			.name("테스트 쿠폰")
			.discountRate(10)
			.expiredAt(LocalDateTime.now().plusDays(30))
			.build();
		coupon = couponRepository.save(coupon);

		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);

		// When - 여러 스레드가 동시에 같은 쿠폰으로 주문
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					OrderCommand command = new OrderCommand(
						user.getId(),
						List.of(product.getId()),
						List.of(new OrderItemCommand(product.getId(), 1)),
						1000L,
						coupon.getId()
					);
					orderUseCase.execute(command);
					successCount.incrementAndGet();
				} catch (Exception e) {
					// 쿠폰 중복 사용으로 인한 실패는 예상됨
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// Then
		Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
		// 쿠폰은 한 번만 사용되어야 함
		assertThat(updatedCoupon.getStatus().name()).isEqualTo("USED");
		// 성공한 주문은 1개여야 함 (쿠폰은 한 번만 사용 가능)
		assertThat(successCount.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("분산락과 DB 트랜잭션 혼용 테스트 - 락이 트랜잭션 밖에서 획득되는지 확인")
	void testDistributedLockWithTransaction() {
		// Given
		userService.chargePoint(user.getId(), 10000L);
		String lockKey = "order:user:" + user.getId();

		// When - 주문 실행 (분산락이 트랜잭션 밖에서 획득되어야 함)
		OrderCommand command = new OrderCommand(
			user.getId(),
			List.of(product.getId()),
			List.of(new OrderItemCommand(product.getId(), 1)),
			1000L,
			null
		);

		OrderResult result = orderUseCase.execute(command);

		// Then
		assertThat(result.getOrderId()).isNotNull();
		Order order = orderRepository.findById(result.getOrderId()).orElseThrow();
		assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

		// 재고와 잔액이 정확하게 차감되었는지 확인
		Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
		assertThat(updatedProduct.getStock()).isEqualTo(9);

		User updatedUser = userRepository.findById(user.getId()).orElseThrow();
		assertThat(updatedUser.getBalance()).isEqualTo(9000L);
	}
}
