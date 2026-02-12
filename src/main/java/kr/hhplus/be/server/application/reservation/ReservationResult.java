package kr.hhplus.be.server.application.reservation;

import lombok.Getter;

@Getter
public class ReservationResult {
    private final Long reservationId;
    private final Long totalAmount;
    private final boolean success;

    private ReservationResult(Long reservationId, Long totalAmount, boolean success) {
        this.reservationId = reservationId;
        this.totalAmount = totalAmount;
        this.success = success;
    }

    public static ReservationResult success(Long reservationId, Long totalAmount) {
        return new ReservationResult(reservationId, totalAmount, true);
    }

    public static ReservationResult failure(String message) {
        throw new RuntimeException(message);
    }
}
