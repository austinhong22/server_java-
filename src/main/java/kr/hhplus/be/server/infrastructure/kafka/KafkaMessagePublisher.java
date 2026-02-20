package kr.hhplus.be.server.infrastructure.kafka;

import kr.hhplus.be.server.infrastructure.kafka.message.OrderCompletedMessage;
import kr.hhplus.be.server.infrastructure.kafka.message.ReservationConfirmedMessage;
import kr.hhplus.be.server.infrastructure.kafka.message.RestaurantSearchMessage;

public interface KafkaMessagePublisher {

    void publishOrderCompleted(OrderCompletedMessage message);

    void publishReservationConfirmed(ReservationConfirmedMessage message);

    void publishRestaurantSearch(RestaurantSearchMessage message);
}
