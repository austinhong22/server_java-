package kr.hhplus.be.server.service.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis 기반 상품 랭킹 서비스 구현체
 * Redis Sorted Set을 사용하여 상품별 주문 수량 랭킹을 관리합니다.
 * 
 * 설계:
 * - Key: "product:ranking:order_count"
 * - Score: 주문 수량 (총 주문된 수량)
 * - Member: productId (String)
 * 
 * Redis Sorted Set의 장점:
 * 1. 자동 정렬: Score 기준으로 자동 정렬되어 랭킹 조회가 빠름
 * 2. 원자적 연산: ZINCRBY를 사용하여 동시성 문제 없이 수량 증가 가능
 * 3. 효율적인 조회: ZREVRANGE로 상위 N개 조회가 O(log(N) + M) 시간 복잡도
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRankingService implements RankingService {

    private static final String RANKING_KEY = "product:ranking:order_count";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void incrementProductOrderCount(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            log.warn("유효하지 않은 파라미터: productId={}, quantity={}", productId, quantity);
            return;
        }

        try {
            // Redis Sorted Set의 ZINCRBY를 사용하여 원자적으로 수량 증가
            // 기존 값이 없으면 0에서 시작하여 quantity만큼 증가
            Double newScore = redisTemplate.opsForZSet().incrementScore(
                    RANKING_KEY,
                    String.valueOf(productId),
                    quantity
            );

            log.debug("상품 랭킹 업데이트: productId={}, quantity={}, newScore={}", 
                    productId, quantity, newScore);
        } catch (Exception e) {
            log.error("상품 랭킹 업데이트 실패: productId={}, quantity={}, error={}", 
                    productId, quantity, e.getMessage(), e);
            // 랭킹 업데이트 실패는 주문 처리에 영향을 주지 않도록 예외를 던지지 않음
        }
    }

    @Override
    public List<Long> getTopRankingProductIds(int topCount) {
        if (topCount <= 0) {
            return List.of();
        }

        try {
            // ZREVRANGE: Score 내림차순으로 상위 N개 조회
            // 0부터 topCount-1까지 조회
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(RANKING_KEY, 0, topCount - 1);

            if (tuples == null || tuples.isEmpty()) {
                log.debug("랭킹 데이터가 없습니다.");
                return List.of();
            }

            return tuples.stream()
                    .map(tuple -> Long.parseLong(tuple.getValue()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("랭킹 조회 실패: topCount={}, error={}", topCount, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public Long getProductOrderCount(Long productId) {
        if (productId == null) {
            return 0L;
        }

        try {
            Double score = redisTemplate.opsForZSet().score(RANKING_KEY, String.valueOf(productId));
            return score == null ? 0L : score.longValue();
        } catch (Exception e) {
            log.error("상품 주문 수량 조회 실패: productId={}, error={}", productId, e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    public void clearRanking() {
        try {
            redisTemplate.delete(RANKING_KEY);
            log.info("랭킹 데이터 초기화 완료");
        } catch (Exception e) {
            log.error("랭킹 데이터 초기화 실패: error={}", e.getMessage(), e);
        }
    }
}
