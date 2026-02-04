package kr.hhplus.be.server.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private Long finalAmount;
}
