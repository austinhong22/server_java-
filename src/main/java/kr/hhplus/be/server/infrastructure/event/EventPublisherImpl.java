package kr.hhplus.be.server.infrastructure.event;

import kr.hhplus.be.server.domain.order.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventPublisherImpl implements EventPublisher {

    @Override
    public void publishOrderCompleted(Order order) {
        // 데이터 플랫폼에 주문 정보 전송 (Mock)
        log.info("데이터 플랫폼에 주문 정보 전송: orderId={}, finalAmount={}", order.getId(), order.getFinalAmount());
        // 실제로는 외부 데이터 플랫폼 API 호출
    }
}
