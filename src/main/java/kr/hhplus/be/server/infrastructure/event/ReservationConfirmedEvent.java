package kr.hhplus.be.server.infrastructure.event;

import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class ReservationConfirmedEvent extends ApplicationEvent {
    private final Long reservationId;
    private final Long userId;
    private final String concertName;
    private final LocalDateTime concertDate;
    private final Integer seatCount;
    private final Long totalAmount;

    @Builder
    public ReservationConfirmedEvent(Object source, Long reservationId, Long userId, String concertName, LocalDateTime concertDate, Integer seatCount, Long totalAmount) {
        super(source);
        this.reservationId = reservationId;
        this.userId = userId;
        this.concertName = concertName;
        this.concertDate = concertDate;
        this.seatCount = seatCount;
        this.totalAmount = totalAmount;
    }
}
