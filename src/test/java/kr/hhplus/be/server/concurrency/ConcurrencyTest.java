package kr.hhplus.be.server.concurrency;

import kr.hhplus.be.server.application.order.OrderCommand;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.application.order.OrderUseCase;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.CouponStatus;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.service.coupon.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("동시성 제어 테스트")
class ConcurrencyTest {

    @Autowired
    private OrderUseCase orderUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    private Product product;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // 상품 생성 (재고 100개)
        product = Product.builder()
                .name("테스트 상품")
                .price(1000L)
                .stock(100)
                .build();
        product = productRepository.save(product);

        // 사용자 생성
        user1 = User.builder()
                .balance(100000L)
                .build();
        user1 = userRepository.save(user1);

        user2 = User.builder()
                .balance(100000L)
                .build();
        user2 = userRepository.save(user2);
    }

    @Test
    @DisplayName("재고 감소 동시성 제어 테스트 - 다수 사용자가 동시에 같은 상품 주문")
    void testStockDecreaseConcurrency() throws InterruptedException {
        // given: 재고 100개, 150명이 동시에 1개씩 주문
        int threadCount = 150;
        int orderQuantity = 1;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        // when: 동시에 주문 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    User user = (index % 2 == 0) ? user1 : user2;
                    OrderCommand command = new OrderCommand(
                            user.getId(),
                            List.of(product.getId()),
                            List.of(new OrderItemCommand(product.getId(), orderQuantity)),
                            1000L,
                            null
                    );
                    orderUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then: 재고가 음수가 되지 않아야 함
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isGreaterThanOrEqualTo(0);
        assertThat(updatedProduct.getStock()).isLessThanOrEqualTo(100);

        // 성공한 주문 수 + 남은 재고 = 초기 재고
        assertThat(successCount.get() + updatedProduct.getStock()).isEqualTo(100);

        // 주문 수 확인
        List<Order> orders = orderRepository.findAll();
        assertThat(orders.size()).isEqualTo(successCount.get());
        assertThat(orders.size()).isLessThanOrEqualTo(100);

        System.out.println("성공한 주문 수: " + successCount.get());
        System.out.println("실패한 주문 수: " + failureCount.get());
        System.out.println("최종 재고: " + updatedProduct.getStock());
    }

    @Test
    @DisplayName("잔액 차감 동시성 제어 테스트 - 동일 유저가 두 번 결제 요청")
    void testBalanceDeductionConcurrency() throws InterruptedException {
        // given: 잔액 10000원, 동일 유저가 동시에 6000원씩 2번 결제 시도
        Long initialBalance = 10000L;
        Long orderAmount = 6000L;
        user1 = userRepository.findById(user1.getId()).orElseThrow();
        // 잔액 업데이트
        user1.chargePoint(initialBalance - user1.getBalance());
        user1 = userRepository.save(user1);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when: 동시에 주문 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    OrderCommand command = new OrderCommand(
                            user1.getId(),
                            List.of(product.getId()),
                            List.of(new OrderItemCommand(product.getId(), 1)),
                            orderAmount,
                            null
                    );
                    orderUseCase.execute(command);
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

        // then: 잔액이 음수가 되지 않아야 함
        User updatedUser = userRepository.findById(user1.getId()).orElseThrow();
        assertThat(updatedUser.getBalance()).isGreaterThanOrEqualTo(0L);
        assertThat(updatedUser.getBalance()).isLessThanOrEqualTo(initialBalance);

        // 성공한 주문은 최대 1개 (잔액이 6000원이므로 1개만 가능)
        assertThat(successCount.get()).isLessThanOrEqualTo(1);
        assertThat(updatedUser.getBalance()).isEqualTo(initialBalance - (successCount.get() * orderAmount));

        System.out.println("성공한 주문 수: " + successCount.get());
        System.out.println("실패한 주문 수: " + failureCount.get());
        System.out.println("최종 잔액: " + updatedUser.getBalance());
    }

    @Test
    @DisplayName("쿠폰 발급 동시성 제어 테스트 - 선착순 쿠폰 발급 요청 몰림")
    void testCouponIssueConcurrency() throws InterruptedException {
        // given: 쿠폰 발급 제한 100개, 150명이 동시에 쿠폰 발급 요청
        int threadCount = 150;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when: 동시에 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    User user = (index % 2 == 0) ? user1 : user2;
                    couponService.issueCoupon(user.getId());
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

        // then: 쿠폰 발급 개수가 100개를 초과하지 않아야 함
        List<Coupon> activeCoupons = couponRepository.findAll().stream()
                .filter(c -> c.getStatus() == CouponStatus.ACTIVE)
                .toList();
        assertThat(activeCoupons.size()).isLessThanOrEqualTo(100);
        assertThat(successCount.get()).isLessThanOrEqualTo(100);

        System.out.println("성공한 쿠폰 발급 수: " + successCount.get());
        System.out.println("실패한 쿠폰 발급 수: " + failureCount.get());
        System.out.println("실제 발급된 쿠폰 수: " + activeCoupons.size());
    }

    @Test
    @DisplayName("복합 동시성 테스트 - 재고, 잔액, 쿠폰 동시 제어")
    void testComplexConcurrency() throws InterruptedException {
        // given: 재고 50개, 잔액 50000원, 쿠폰 발급 제한 30개
        product = productRepository.save(Product.builder()
                .name("테스트 상품")
                .price(1000L)
                .stock(50)
                .build());

        user1 = userRepository.save(User.builder()
                .balance(50000L)
                .build());

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger orderSuccessCount = new AtomicInteger(0);
        AtomicInteger couponSuccessCount = new AtomicInteger(0);

        // when: 동시에 주문 및 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 주문 요청
                    OrderCommand command = new OrderCommand(
                            user1.getId(),
                            List.of(product.getId()),
                            List.of(new OrderItemCommand(product.getId(), 1)),
                            1000L,
                            null
                    );
                    orderUseCase.execute(command);
                    orderSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    // 주문 실패는 예상됨
                }

                try {
                    // 쿠폰 발급 요청
                    couponService.issueCoupon(user1.getId());
                    couponSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    // 쿠폰 발급 실패는 예상됨
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then: 모든 제약 조건이 지켜져야 함
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        User updatedUser = userRepository.findById(user1.getId()).orElseThrow();
        List<Coupon> activeCoupons = couponRepository.findAll().stream()
                .filter(c -> c.getStatus() == CouponStatus.ACTIVE)
                .toList();

        assertThat(updatedProduct.getStock()).isGreaterThanOrEqualTo(0);
        assertThat(updatedProduct.getStock()).isLessThanOrEqualTo(50);
        assertThat(updatedUser.getBalance()).isGreaterThanOrEqualTo(0L);
        assertThat(activeCoupons.size()).isLessThanOrEqualTo(100);

        System.out.println("성공한 주문 수: " + orderSuccessCount.get());
        System.out.println("성공한 쿠폰 발급 수: " + couponSuccessCount.get());
        System.out.println("최종 재고: " + updatedProduct.getStock());
        System.out.println("최종 잔액: " + updatedUser.getBalance());
        System.out.println("발급된 쿠폰 수: " + activeCoupons.size());
    }
}
