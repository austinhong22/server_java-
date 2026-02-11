package kr.hhplus.be.server.service.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 랭킹 서비스 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class RedisRankingServiceTest {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // 테스트 전 랭킹 데이터 초기화
        rankingService.clearRanking();
    }

    @Test
    @DisplayName("상품 주문 수량을 증가시킬 수 있다")
    void incrementProductOrderCount() {
        // given
        Long productId = 1L;
        Integer quantity = 5;

        // when
        rankingService.incrementProductOrderCount(productId, quantity);

        // then
        Long orderCount = rankingService.getProductOrderCount(productId);
        assertThat(orderCount).isEqualTo(5L);
    }

    @Test
    @DisplayName("상품 주문 수량을 여러 번 증가시킬 수 있다")
    void incrementProductOrderCountMultipleTimes() {
        // given
        Long productId = 1L;

        // when
        rankingService.incrementProductOrderCount(productId, 3);
        rankingService.incrementProductOrderCount(productId, 2);
        rankingService.incrementProductOrderCount(productId, 5);

        // then
        Long orderCount = rankingService.getProductOrderCount(productId);
        assertThat(orderCount).isEqualTo(10L);
    }

    @Test
    @DisplayName("여러 상품의 주문 수량을 관리할 수 있다")
    void manageMultipleProducts() {
        // given
        Long productId1 = 1L;
        Long productId2 = 2L;
        Long productId3 = 3L;

        // when
        rankingService.incrementProductOrderCount(productId1, 10);
        rankingService.incrementProductOrderCount(productId2, 5);
        rankingService.incrementProductOrderCount(productId3, 15);

        // then
        assertThat(rankingService.getProductOrderCount(productId1)).isEqualTo(10L);
        assertThat(rankingService.getProductOrderCount(productId2)).isEqualTo(5L);
        assertThat(rankingService.getProductOrderCount(productId3)).isEqualTo(15L);
    }

    @Test
    @DisplayName("상위 랭킹 상품을 조회할 수 있다")
    void getTopRankingProductIds() {
        // given
        rankingService.incrementProductOrderCount(1L, 10);
        rankingService.incrementProductOrderCount(2L, 20);
        rankingService.incrementProductOrderCount(3L, 15);
        rankingService.incrementProductOrderCount(4L, 5);
        rankingService.incrementProductOrderCount(5L, 25);

        // when
        List<Long> topProducts = rankingService.getTopRankingProductIds(3);

        // then
        assertThat(topProducts).hasSize(3);
        assertThat(topProducts.get(0)).isEqualTo(5L); // 25개
        assertThat(topProducts.get(1)).isEqualTo(2L); // 20개
        assertThat(topProducts.get(2)).isEqualTo(3L); // 15개
    }

    @Test
    @DisplayName("랭킹 데이터가 없으면 빈 리스트를 반환한다")
    void getTopRankingProductIdsWhenEmpty() {
        // when
        List<Long> topProducts = rankingService.getTopRankingProductIds(5);

        // then
        assertThat(topProducts).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 상품의 주문 수량은 0을 반환한다")
    void getProductOrderCountWhenNotExists() {
        // when
        Long orderCount = rankingService.getProductOrderCount(999L);

        // then
        assertThat(orderCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("랭킹 데이터를 초기화할 수 있다")
    void clearRanking() {
        // given
        rankingService.incrementProductOrderCount(1L, 10);
        rankingService.incrementProductOrderCount(2L, 20);
        assertThat(rankingService.getProductOrderCount(1L)).isEqualTo(10L);

        // when
        rankingService.clearRanking();

        // then
        assertThat(rankingService.getProductOrderCount(1L)).isEqualTo(0L);
        assertThat(rankingService.getTopRankingProductIds(5)).isEmpty();
    }
}
