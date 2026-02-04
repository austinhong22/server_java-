package kr.hhplus.be.server.infrastructure.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderCompletedEvent {
    private Long orderId;
    private Long userId;
    private Long finalAmount;
    private Long totalAmount;
    private Long discountAmount;
}
