package kr.hhplus.be.server.service.product;

import java.util.List;

/**
 * 상품 랭킹 서비스 인터페이스
 * Redis Sorted Set을 사용하여 상품별 주문 수량 랭킹을 관리합니다.
 */
public interface RankingService {

    /**
     * 상품의 주문 수량을 증가시킵니다.
     * 
     * @param productId 상품 ID
     * @param quantity 증가시킬 수량
     */
    void incrementProductOrderCount(Long productId, Integer quantity);

    /**
     * 상위 N개의 인기 상품 ID를 조회합니다.
     * 
     * @param topCount 조회할 상위 개수
     * @return 상품 ID 목록 (주문 수량 내림차순)
     */
    List<Long> getTopRankingProductIds(int topCount);

    /**
     * 특정 상품의 주문 수량을 조회합니다.
     * 
     * @param productId 상품 ID
     * @return 주문 수량 (없으면 0)
     */
    Long getProductOrderCount(Long productId);

    /**
     * 랭킹 데이터를 초기화합니다.
     */
    void clearRanking();
}
