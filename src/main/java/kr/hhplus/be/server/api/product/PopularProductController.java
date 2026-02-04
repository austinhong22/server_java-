package kr.hhplus.be.server.api.product;

import kr.hhplus.be.server.api.product.dto.ProductResponse;
import kr.hhplus.be.server.service.product.PopularProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/popular")
@RequiredArgsConstructor
public class PopularProductController {

    private final PopularProductService popularProductService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getPopularProducts() {
        List<ProductResponse> products = popularProductService.getPopularProducts();
        return ResponseEntity.ok(products);
    }
}
