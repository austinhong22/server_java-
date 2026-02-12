package kr.hhplus.be.server.api.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ReservationRequest(
        Long userId,
        String concertName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime concertDate,
        Integer seatCount,
        Long totalAmount
) {
}
