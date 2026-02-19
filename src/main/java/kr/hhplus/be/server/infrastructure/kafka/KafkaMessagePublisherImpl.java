package kr.hhplus.be.server.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.infrastructure.kafka.message.OrderCompletedMessage;
import kr.hhplus.be.server.infrastructure.kafka.message.ReservationConfirmedMessage;
import kr.hhplus.be.server.infrastructure.kafka.message.RestaurantSearchMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaMessagePublisherImpl implements KafkaMessagePublisher {

    @Value("${app.kafka.topics.order-events:order-events}")
    private String orderEventsTopic;

    @Value("${app.kafka.topics.reservation-events:reservation-events}")
    private String reservationEventsTopic;

    @Value("${app.kafka.topics.restaurant-search-events:restaurant-search-events}")
    private String restaurantSearchEventsTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishOrderCompleted(OrderCompletedMessage message) {
        publish(orderEventsTopic, String.valueOf(message.orderId()), message);
    }

    @Override
    public void publishReservationConfirmed(ReservationConfirmedMessage message) {
        publish(reservationEventsTopic, String.valueOf(message.reservationId()), message);
    }

    @Override
    public void publishRestaurantSearch(RestaurantSearchMessage message) {
        String key = message.userId() != null ? String.valueOf(message.userId()) : message.keyword();
        publish(restaurantSearchEventsTopic, key, message);
    }

    private void publish(String topic, String key, Object message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, key, payload).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Kafka 메시지 발행 실패: topic={}, key={}, error={}", topic, key, throwable.getMessage(), throwable);
                    return;
                }
                log.info("Kafka 메시지 발행 성공: topic={}, key={}, offset={}",
                        topic, key, result.getRecordMetadata().offset());
            });
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 직렬화 실패: topic={}, key={}, error={}", topic, key, e.getMessage(), e);
        }
    }
}
