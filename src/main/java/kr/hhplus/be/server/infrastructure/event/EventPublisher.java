package kr.hhplus.be.server.infrastructure.event;

import kr.hhplus.be.server.domain.order.Order;

public interface EventPublisher {
    void publishOrderCompleted(Order order);
}
