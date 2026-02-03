package kr.hhplus.be.server.api.order;

import kr.hhplus.be.server.api.order.dto.OrderRequest;
import kr.hhplus.be.server.api.order.dto.OrderResponse;
import kr.hhplus.be.server.application.order.OrderCommand;
import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.application.order.OrderResult;
import kr.hhplus.be.server.application.order.OrderUseCase;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderUseCase orderUseCase;
    private final ProductRepository productRepository;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        List<OrderItemCommand> orderItems = request.getOrderItems().stream()
                .map(item -> new OrderItemCommand(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());

        List<Long> productIds = request.getOrderItems().stream()
                .map(OrderRequest.OrderItemRequest::getProductId)
                .collect(Collectors.toList());

        // 상품 정보를 조회하여 총 금액 계산
        List<Product> products = productRepository.findAllByIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Long totalAmount = request.getOrderItems().stream()
                .mapToLong(item -> {
                    Product product = productMap.get(item.getProductId());
                    if (product == null) {
                        throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + item.getProductId());
                    }
                    return product.getPrice() * item.getQuantity();
                })
                .sum();

        OrderCommand command = new OrderCommand(
                request.getUserId(),
                productIds,
                orderItems,
                totalAmount,
                request.getCouponId()
        );

        OrderResult result = orderUseCase.execute(command);

        OrderResponse response = new OrderResponse(
                result.getOrderId(),
                result.getFinalAmount()
        );

        return ResponseEntity.ok(response);
    }
}
