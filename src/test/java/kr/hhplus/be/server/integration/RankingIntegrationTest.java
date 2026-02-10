package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.api.product.dto.ProductResponse;
import kr.hhplus.be.server.application.order.OrderCommand;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.application.order.OrderUseCase;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.service.product.PopularProductService;
import kr.hhplus.be.server.service.product.RankingService;
import kr.hhplus.be.server.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 랭킹 시스템 통합 테스트
 * 주문 완료 후 Redis 랭킹이 업데이트되는지 확인합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RankingIntegrationTest {

    @Autowired
    private OrderUseCase orderUseCase;

    @Autowired
    private PopularProductService popularProductService;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserService userService;

    private User user;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        user = User.builder()
                .balance(0L)
                .build();
        user = userRepository.save(user);

        product1 = Product.builder()
                .name("상품1")
                .price(10000L)
                .stock(100)
                .build();
        product1 = productRepository.save(product1);

        product2 = Product.builder()
                .name("상품2")
                .price(20000L)
                .stock(100)
                .build();
        product2 = productRepository.save(product2);

        product3 = Product.builder()
                .name("상품3")
                .price(30000L)
                .stock(100)
                .build();
        product3 = productRepository.save(product3);

        // 사용자 포인트 충전
        userService.chargePoint(user.getId(), 1000000L);

        // 랭킹 데이터 초기화
        rankingService.clearRanking();
    }

    @Test
    @DisplayName("주문 완료 시 상품 랭킹이 업데이트된다")
    void orderCompletedUpdatesRanking() throws InterruptedException {
        // given
        OrderCommand command1 = new OrderCommand(
                user.getId(),
                List.of(product1.getId()),
                List.of(new OrderItemCommand(product1.getId(), 5)),
                50000L,
                null
        );

        OrderCommand command2 = new OrderCommand(
                user.getId(),
                List.of(product2.getId()),
                List.of(new OrderItemCommand(product2.getId(), 10)),
                200000L,
                null
        );

        OrderCommand command3 = new OrderCommand(
                user.getId(),
                List.of(product1.getId()),
                List.of(new OrderItemCommand(product1.getId(), 3)),
                30000L,
                null
        );

        // when
        orderUseCase.execute(command1);
        orderUseCase.execute(command2);
        orderUseCase.execute(command3);

        // 비동기 이벤트 처리 대기
        Thread.sleep(1000);

        // then
        assertThat(rankingService.getProductOrderCount(product1.getId())).isEqualTo(8L); // 5 + 3
        assertThat(rankingService.getProductOrderCount(product2.getId())).isEqualTo(10L);
        assertThat(rankingService.getProductOrderCount(product3.getId())).isEqualTo(0L);
    }

    @Test
    @DisplayName("인기 상품 조회 시 주문 수량이 많은 순서대로 반환된다")
    void getPopularProductsReturnsInOrder() throws InterruptedException {
        // given
        // 상품1: 5개 주문
        orderUseCase.execute(new OrderCommand(
                user.getId(),
                List.of(product1.getId()),
                List.of(new OrderItemCommand(product1.getId(), 5)),
                50000L,
                null
        ));

        // 상품2: 10개 주문
        orderUseCase.execute(new OrderCommand(
                user.getId(),
                List.of(product2.getId()),
                List.of(new OrderItemCommand(product2.getId(), 10)),
                200000L,
                null
        ));

        // 상품3: 7개 주문
        orderUseCase.execute(new OrderCommand(
                user.getId(),
                List.of(product3.getId()),
                List.of(new OrderItemCommand(product3.getId(), 7)),
                210000L,
                null
        ));

        // 비동기 이벤트 처리 대기
        Thread.sleep(1000);

        // when
        List<ProductResponse> popularProducts = popularProductService.getPopularProducts();

        // then
        assertThat(popularProducts).hasSize(3);
        assertThat(popularProducts.get(0).getName()).isEqualTo("상품2"); // 10개
        assertThat(popularProducts.get(1).getName()).isEqualTo("상품3"); // 7개
        assertThat(popularProducts.get(2).getName()).isEqualTo("상품1"); // 5개
    }
}
