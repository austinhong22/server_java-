package kr.hhplus.be.server.service.product;

import kr.hhplus.be.server.api.product.dto.ProductResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 인기 상품 서비스
 * Redis 기반 랭킹 시스템을 사용하여 가장 많이 주문한 상품을 조회합니다.
 * 
 * 기존 DB 기반 조회 방식에서 Redis 기반으로 변경한 이유:
 * 1. 성능: Redis Sorted Set을 사용하여 O(log(N) + M) 시간 복잡도로 빠른 조회
 * 2. 실시간성: 주문 완료 즉시 랭킹이 업데이트되어 실시간 랭킹 제공
 * 3. 확장성: DB 부하를 줄이고 수평 확장이 용이
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularProductService {

    private static final int TOP_COUNT = 5;

    private final RankingService rankingService;
    private final ProductRepository productRepository;

    /**
     * 가장 많이 주문한 상위 상품을 조회합니다.
     * Redis Sorted Set에서 상위 N개의 상품 ID를 조회한 후, 상품 정보를 조회하여 반환합니다.
     * 
     * @return 인기 상품 목록 (주문 수량 내림차순)
     */
    public List<ProductResponse> getPopularProducts() {
        // Redis에서 상위 N개의 상품 ID 조회
        List<Long> productIds = rankingService.getTopRankingProductIds(TOP_COUNT);

        if (productIds.isEmpty()) {
            log.debug("랭킹 데이터가 없습니다.");
            return List.of();
        }

        // 상품 정보 조회
        List<Product> products = productRepository.findAllByIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 랭킹 순서대로 정렬하여 반환 (Redis에서 이미 정렬되어 있음)
        return productIds.stream()
                .map(productMap::get)
                .filter(product -> product != null)
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }
}
