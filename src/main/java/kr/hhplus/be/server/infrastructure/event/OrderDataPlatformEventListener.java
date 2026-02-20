package kr.hhplus.be.server.infrastructure.event;

import java.time.LocalDateTime;

import kr.hhplus.be.server.infrastructure.kafka.KafkaMessagePublisher;
import kr.hhplus.be.server.infrastructure.kafka.message.OrderCompletedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 완료 이벤트 리스너
 * 주문 완료 이벤트를 수신하여 Kafka 토픽으로 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDataPlatformEventListener {

    private final KafkaMessagePublisher kafkaMessagePublisher;

    /**
     * 주문 완료 이벤트를 수신하여 Kafka로 전달합니다.
     *
     * @param event 주문 완료 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        OrderCompletedMessage message = new OrderCompletedMessage(
                event.getOrderId(),
                event.getUserId(),
                event.getFinalAmount(),
                event.getTotalAmount(),
                event.getDiscountAmount(),
                LocalDateTime.now()
        );
        kafkaMessagePublisher.publishOrderCompleted(message);
        log.info("주문 완료 이벤트를 Kafka로 발행 요청: orderId={}, userId={}",
                event.getOrderId(), event.getUserId());
    }
}
