package kr.hhplus.be.server.api.reservation.dto;

public record ReservationResponse(
        Long reservationId,
        Long totalAmount
) {
}
