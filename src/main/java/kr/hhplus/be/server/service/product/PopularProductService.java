package kr.hhplus.be.server.service.product;

import kr.hhplus.be.server.api.product.dto.ProductResponse;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularProductService {

    private static final int DAYS = 3;
    private static final int TOP_COUNT = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public List<ProductResponse> getPopularProducts() {
        // 최근 3일간 판매량이 높은 상품 조회
        LocalDateTime startDate = LocalDateTime.now().minusDays(DAYS);
        List<Object[]> topSellingProducts = orderRepository.findTopSellingProducts(startDate);

        // 상위 5개만 선택
        List<Long> productIds = topSellingProducts.stream()
                .limit(TOP_COUNT)
                .map(result -> (Long) result[0])
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return List.of();
        }

        // 상품 정보 조회
        List<Product> products = productRepository.findAllByIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 판매량 순서대로 정렬하여 반환
        return productIds.stream()
                .map(productMap::get)
                .filter(product -> product != null)
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }
}
