package kr.hhplus.be.server.service.product;

import kr.hhplus.be.server.api.product.dto.ProductResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 단위 테스트")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        product1 = Product.builder()
                .name("상품1")
                .price(1000L)
                .stock(10)
                .build();

        product2 = Product.builder()
                .name("상품2")
                .price(2000L)
                .stock(5)
                .build();
    }

    @Test
    @DisplayName("전체 상품 조회 성공")
    void getAllProductsSuccess() {
        // given
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(products);

        // when
        List<ProductResponse> result = productService.getAllProducts();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("상품1");
        assertThat(result.get(1).getName()).isEqualTo("상품2");
    }

    @Test
    @DisplayName("상품 조회 성공")
    void getProductSuccess() {
        // given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        // when
        ProductResponse result = productService.getProduct(1L);

        // then
        assertThat(result.getId()).isEqualTo(product1.getId());
        assertThat(result.getName()).isEqualTo("상품1");
        assertThat(result.getPrice()).isEqualTo(1000L);
        assertThat(result.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("상품 조회 실패 - 상품을 찾을 수 없음")
    void getProductNotFound() {
        // given
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품을 찾을 수 없습니다.");
    }
}
