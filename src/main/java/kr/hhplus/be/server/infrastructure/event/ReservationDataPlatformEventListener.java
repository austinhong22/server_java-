package kr.hhplus.be.server.infrastructure.event;

import java.time.LocalDateTime;

import kr.hhplus.be.server.infrastructure.kafka.KafkaMessagePublisher;
import kr.hhplus.be.server.infrastructure.kafka.message.ReservationConfirmedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 예약 완료 이벤트 리스너
 * 예약 완료 이벤트를 수신하여 Kafka 토픽으로 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationDataPlatformEventListener {

    private final KafkaMessagePublisher kafkaMessagePublisher;

    /**
     * 예약 완료 이벤트를 수신하여 Kafka로 전달합니다.
     *
     * @param event 예약 완료 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationConfirmed(ReservationConfirmedEvent event) {
        ReservationConfirmedMessage message = new ReservationConfirmedMessage(
                event.getReservationId(),
                event.getUserId(),
                event.getConcertName(),
                event.getConcertDate(),
                event.getSeatCount(),
                event.getTotalAmount(),
                LocalDateTime.now()
        );
        kafkaMessagePublisher.publishReservationConfirmed(message);
        log.info("예약 완료 이벤트를 Kafka로 발행 요청: reservationId={}, userId={}",
                event.getReservationId(), event.getUserId());
    }
}
