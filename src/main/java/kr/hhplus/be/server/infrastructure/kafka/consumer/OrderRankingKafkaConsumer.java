package kr.hhplus.be.server.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.infrastructure.kafka.message.OrderCompletedMessage;
import kr.hhplus.be.server.service.product.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderRankingKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final RankingService rankingService;

    @Transactional(readOnly = true)
    @KafkaListener(
            topics = "${app.kafka.topics.order-events:order-events}",
            groupId = "${app.kafka.consumer-groups.order-ranking:order-ranking-consumer}"
    )
    public void consumeOrderEvent(String payload) {
        try {
            OrderCompletedMessage message = objectMapper.readValue(payload, OrderCompletedMessage.class);
            Order order = orderRepository.findById(message.orderId())
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + message.orderId()));

            order.getOrderItems().forEach(orderItem -> rankingService.incrementProductOrderCount(
                    orderItem.getProduct().getId(),
                    orderItem.getQuantity()
            ));

            log.info("주문 이벤트 기반 랭킹 업데이트 완료: orderId={}, itemCount={}",
                    order.getId(), order.getOrderItems().size());
        } catch (Exception e) {
            log.error("주문 이벤트 기반 랭킹 업데이트 실패: error={}", e.getMessage(), e);
        }
    }
}
