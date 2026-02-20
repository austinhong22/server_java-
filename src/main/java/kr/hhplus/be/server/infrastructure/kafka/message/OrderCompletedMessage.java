package kr.hhplus.be.server.infrastructure.kafka.message;

import java.time.LocalDateTime;

public record OrderCompletedMessage(
        Long orderId,
        Long userId,
        Long finalAmount,
        Long totalAmount,
        Long discountAmount,
        LocalDateTime occurredAt
) {
}
