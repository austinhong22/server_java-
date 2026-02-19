package kr.hhplus.be.server.infrastructure.event;

import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderCompletedEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long userId;
    private final Long finalAmount;
    private final Long totalAmount;
    private final Long discountAmount;

    @Builder
    public OrderCompletedEvent(Object source, Long orderId, Long userId, Long finalAmount, Long totalAmount, Long discountAmount) {
        super(source);
        this.orderId = orderId;
        this.userId = userId;
        this.finalAmount = finalAmount;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
    }
}
