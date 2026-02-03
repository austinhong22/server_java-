package kr.hhplus.be.server.application.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemCommand {
    private Long productId;
    private Integer quantity;
}
