package kr.hhplus.be.server.infrastructure.kafka.message;

import java.time.LocalDateTime;

public record ReservationConfirmedMessage(
        Long reservationId,
        Long userId,
        String concertName,
        LocalDateTime concertDate,
        Integer seatCount,
        Long totalAmount,
        LocalDateTime occurredAt
) {
}
