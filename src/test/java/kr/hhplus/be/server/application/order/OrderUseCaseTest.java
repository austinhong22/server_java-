package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.infrastructure.event.EventPublisher;
import kr.hhplus.be.server.infrastructure.payment.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderUseCase 단위 테스트")
class OrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private OrderUseCase orderUseCase;

    private User user;
    private Product product1;
    private Product product2;
    private Coupon coupon;

    @BeforeEach
    void setUp() throws Exception {
        user = User.builder()
                .balance(10000L)
                .build();
        setField(user, "id", 1L);
        user = spy(user);

        product1 = Product.builder()
                .name("상품1")
                .price(1000L)
                .stock(10)
                .build();
        setField(product1, "id", 1L);
        product1 = spy(product1);

        product2 = Product.builder()
                .name("상품2")
                .price(2000L)
                .stock(5)
                .build();
        setField(product2, "id", 2L);
        product2 = spy(product2);

        coupon = Coupon.builder()
                .user(user)
                .name("할인 쿠폰")
                .discountRate(10)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .build();
        setField(coupon, "id", 1L);
        coupon = spy(coupon);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("주문 성공 - 쿠폰 없이")
    void orderSuccessWithoutCoupon() {
        // given
        OrderItemCommand item1 = new OrderItemCommand(1L, 2);
        OrderItemCommand item2 = new OrderItemCommand(2L, 1);
        OrderCommand command = new OrderCommand(
                1L,
                Arrays.asList(1L, 2L),
                Arrays.asList(item1, item2),
                4000L, // 1000 * 2 + 2000 * 1
                null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findAllByIds(anyList())).thenReturn(Arrays.asList(product1, product2));
        when(productRepository.decreaseStockIfAvailable(1L, 2)).thenReturn(1);
        when(productRepository.decreaseStockIfAvailable(2L, 1)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(userRepository.deductBalanceIfAvailable(1L, 4000L)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            try {
                setField(order, "id", 1L);
            } catch (Exception e) {
                // ignore
            }
            return order;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(paymentGateway).processPayment(anyLong(), anyLong());
        doNothing().when(eventPublisher).publishOrderCompleted(any(Order.class));

        // when
        OrderResult result = orderUseCase.execute(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalAmount()).isEqualTo(4000L);

        verify(userRepository).findById(1L);
        verify(productRepository).findAllByIds(Arrays.asList(1L, 2L));
        verify(productRepository).decreaseStockIfAvailable(1L, 2);
        verify(productRepository).decreaseStockIfAvailable(2L, 1);
        verify(userRepository).deductBalanceIfAvailable(1L, 4000L);
        verify(paymentGateway).processPayment(1L, 4000L);
        verify(eventPublisher).publishOrderCompleted(any(Order.class));
    }

    @Test
    @DisplayName("주문 성공 - 쿠폰 사용")
    void orderSuccessWithCoupon() {
        // given
        OrderItemCommand item1 = new OrderItemCommand(1L, 2);
        OrderCommand command = new OrderCommand(
                1L,
                Arrays.asList(1L),
                Arrays.asList(item1),
                2000L,
                1L // 쿠폰 ID
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findAllByIds(anyList())).thenReturn(Arrays.asList(product1));
        when(productRepository.decreaseStockIfAvailable(1L, 2)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(coupon));
        when(userRepository.deductBalanceIfAvailable(1L, 1800L)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            try {
                setField(order, "id", 1L);
            } catch (Exception e) {
                // ignore
            }
            return order;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(paymentGateway).processPayment(anyLong(), anyLong());
        doNothing().when(eventPublisher).publishOrderCompleted(any(Order.class));

        // when
        OrderResult result = orderUseCase.execute(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalAmount()).isEqualTo(1800L); // 2000 - 200 (10% 할인)

        verify(coupon).use();
        verify(couponRepository).save(coupon);
    }

    @Test
    @DisplayName("주문 실패 - 사용자를 찾을 수 없음")
    void orderFailUserNotFound() {
        // given
        OrderCommand command = new OrderCommand(
                1L,
                Arrays.asList(1L),
                Arrays.asList(new OrderItemCommand(1L, 1)),
                1000L,
                null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findById(1L);
        verify(productRepository, never()).findAllByIds(anyList());
    }

    @Test
    @DisplayName("주문 실패 - 잔액 부족")
    void orderFailInsufficientBalance() {
        // given
        User poorUser = User.builder().balance(1000L).build();
        poorUser = spy(poorUser);
        OrderItemCommand item1 = new OrderItemCommand(1L, 2);
        OrderCommand command = new OrderCommand(
                1L,
                Arrays.asList(1L),
                Arrays.asList(item1),
                2000L,
                null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(poorUser));
        when(productRepository.findAllByIds(anyList())).thenReturn(Arrays.asList(product1));
        when(productRepository.decreaseStockIfAvailable(1L, 2)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(userRepository.deductBalanceIfAvailable(1L, 2000L)).thenReturn(0); // 잔액 부족

        // when & then
        assertThatThrownBy(() -> orderUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액이 부족합니다.");

        verify(userRepository).findById(1L);
        verify(userRepository).deductBalanceIfAvailable(1L, 2000L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 실패 - 재고 부족")
    void orderFailInsufficientStock() {
        // given
        Product outOfStockProduct = Product.builder()
                .name("품절 상품")
                .price(1000L)
                .stock(0)
                .build();
        try {
            setField(outOfStockProduct, "id", 1L);
        } catch (Exception e) {
            // ignore
        }
        outOfStockProduct = spy(outOfStockProduct);

        OrderItemCommand item1 = new OrderItemCommand(1L, 1);
        OrderCommand command = new OrderCommand(
                1L,
                Arrays.asList(1L),
                Arrays.asList(item1),
                1000L,
                null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findAllByIds(anyList())).thenReturn(Arrays.asList(outOfStockProduct));
        when(productRepository.decreaseStockIfAvailable(1L, 1)).thenReturn(0); // 재고 부족

        // when & then
        assertThatThrownBy(() -> orderUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("재고가 부족합니다");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).deductBalanceIfAvailable(anyLong(), anyLong());
    }

    @Test
    @DisplayName("주문 실패 - 결제 게이트웨이 오류")
    void orderFailPaymentGatewayError() {
        // given
        OrderItemCommand item1 = new OrderItemCommand(1L, 1);
        OrderCommand command = new OrderCommand(
                1L,
                Arrays.asList(1L),
                Arrays.asList(item1),
                1000L,
                null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findAllByIds(anyList())).thenReturn(Arrays.asList(product1));
        when(productRepository.decreaseStockIfAvailable(1L, 1)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            try {
                setField(order, "id", 1L);
            } catch (Exception e) {
                // ignore
            }
            return order;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("결제 게이트웨이 오류"))
                .when(paymentGateway).processPayment(anyLong(), anyLong());

        // when & then
        assertThatThrownBy(() -> orderUseCase.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("결제 처리 중 오류가 발생했습니다");

        verify(paymentGateway).processPayment(anyLong(), anyLong());
        verify(userRepository, never()).deductBalanceIfAvailable(anyLong(), anyLong());
        verify(eventPublisher, never()).publishOrderCompleted(any(Order.class));
    }
}
