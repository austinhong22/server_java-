package kr.hhplus.be.server.application.reservation;

import java.time.LocalDateTime;

public record ReservationCommand(
        Long userId,
        String concertName,
        LocalDateTime concertDate,
        Integer seatCount,
        Long totalAmount
) {
}
