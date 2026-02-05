package kr.hhplus.be.server.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.infrastructure.message.MockMessageProducer;
import kr.hhplus.be.server.infrastructure.outbox.Outbox;
import kr.hhplus.be.server.infrastructure.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisherImpl implements EventPublisher {

    private static final String ORDER_COMPLETED_EVENT_TYPE = "ORDER_COMPLETED";
    private static final String MESSAGE_TOPIC = "order-events";

    private final OutboxRepository outboxRepository;
    private final MockMessageProducer messageProducer;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publishOrderCompleted(Order order) {
        try {
            // 주문 정보를 JSON으로 직렬화
            OrderCompletedEvent event = OrderCompletedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUser().getId())
                    .finalAmount(order.getFinalAmount())
                    .totalAmount(order.getTotalAmount())
                    .discountAmount(order.getDiscountAmount())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            // Outbox에 저장 (트랜잭션 내에서)
            Outbox outbox = Outbox.builder()
                    .eventType(ORDER_COMPLETED_EVENT_TYPE)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);

            // 메시지 전송 시도
            try {
                messageProducer.send(MESSAGE_TOPIC, payload);
                outbox.markAsSent();
                outboxRepository.save(outbox);
                log.info("데이터 플랫폼에 주문 정보 전송 성공: orderId={}, finalAmount={}", order.getId(), order.getFinalAmount());
            } catch (Exception e) {
                // 전송 실패 시 Outbox에 실패 상태 저장 (나중에 재시도 가능)
                outbox.markAsFailed(e.getMessage());
                outboxRepository.save(outbox);
                log.error("데이터 플랫폼에 주문 정보 전송 실패: orderId={}, error={}", order.getId(), e.getMessage());
                // 예외를 다시 던지지 않음 (주문 완료는 이미 처리되었으므로)
            }
        } catch (JsonProcessingException e) {
            log.error("주문 이벤트 직렬화 실패: orderId={}, error={}", order.getId(), e.getMessage());
            // 직렬화 실패는 심각한 오류이므로 예외를 던짐
            throw new RuntimeException("주문 이벤트 직렬화 실패", e);
        }
    }
}
