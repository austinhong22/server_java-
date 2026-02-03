package kr.hhplus.be.server.application.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResult {
    private Long orderId;
    private Long finalAmount;
    private boolean success;

    public static OrderResult success(Long orderId, Long finalAmount) {
        return new OrderResult(orderId, finalAmount, true);
    }

    public static OrderResult failure(String message) {
        throw new RuntimeException(message);
    }
}
