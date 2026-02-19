package kr.hhplus.be.server.service.product;

import kr.hhplus.be.server.api.product.dto.ProductResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.infrastructure.kafka.KafkaMessagePublisher;
import kr.hhplus.be.server.infrastructure.kafka.message.RestaurantSearchMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaMessagePublisher kafkaMessagePublisher;

    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return ProductResponse.from(product);
    }

    public List<ProductResponse> searchProducts(String keyword, Long userId) {
        if (!StringUtils.hasText(keyword)) {
            throw new IllegalArgumentException("검색어를 입력해 주세요.");
        }

        String normalizedKeyword = keyword.trim();
        List<Product> products = productRepository.findByNameContainingIgnoreCase(normalizedKeyword);
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());

        RestaurantSearchMessage searchMessage = new RestaurantSearchMessage(
                userId,
                normalizedKeyword,
                responses.size(),
                LocalDateTime.now()
        );
        kafkaMessagePublisher.publishRestaurantSearch(searchMessage);

        return responses;
    }
}
