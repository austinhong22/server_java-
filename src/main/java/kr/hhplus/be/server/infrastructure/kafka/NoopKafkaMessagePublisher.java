package kr.hhplus.be.server.infrastructure.kafka;

import kr.hhplus.be.server.infrastructure.kafka.message.OrderCompletedMessage;
import kr.hhplus.be.server.infrastructure.kafka.message.ReservationConfirmedMessage;
import kr.hhplus.be.server.infrastructure.kafka.message.RestaurantSearchMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoopKafkaMessagePublisher implements KafkaMessagePublisher {

    @Override
    public void publishOrderCompleted(OrderCompletedMessage message) {
        log.debug("Kafka 비활성화 상태 - 주문 이벤트 무시: orderId={}", message.orderId());
    }

    @Override
    public void publishReservationConfirmed(ReservationConfirmedMessage message) {
        log.debug("Kafka 비활성화 상태 - 예약 이벤트 무시: reservationId={}", message.reservationId());
    }

    @Override
    public void publishRestaurantSearch(RestaurantSearchMessage message) {
        log.debug("Kafka 비활성화 상태 - 검색 이벤트 무시: keyword={}", message.keyword());
    }
}
