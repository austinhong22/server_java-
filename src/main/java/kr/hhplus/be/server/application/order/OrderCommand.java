package kr.hhplus.be.server.application.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OrderCommand {
    private Long userId;
    private List<Long> productIds;
    private List<OrderItemCommand> orderItems;
    private Long totalAmount;
    private Long couponId;
}
