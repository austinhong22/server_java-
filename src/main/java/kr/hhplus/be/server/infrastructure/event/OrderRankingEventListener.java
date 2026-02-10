package kr.hhplus.be.server.infrastructure.event;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.service.product.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 완료 이벤트 리스너
 * 주문이 완료되면 상품별 주문 수량을 Redis 랭킹에 반영합니다.
 * 
 * 설계 고려사항:
 * 1. 비동기 처리: 랭킹 업데이트는 주문 처리 성능에 영향을 주지 않도록 비동기로 처리
 * 2. 트랜잭션 완료 후 처리: AFTER_COMMIT으로 설정하여 주문이 확실히 완료된 후에만 랭킹 업데이트
 * 3. 실패 허용: 랭킹 업데이트 실패가 주문 처리에 영향을 주지 않도록 예외를 잡아서 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRankingEventListener {

    private final OrderRepository orderRepository;
    private final RankingService rankingService;

    /**
     * 주문 완료 이벤트를 수신하여 상품 랭킹을 업데이트합니다.
     * 
     * @param order 완료된 주문
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(Order order) {
        try {
            // 주문이 완료 상태인지 확인
            if (!order.isCompleted()) {
                log.debug("주문이 완료되지 않아 랭킹 업데이트를 건너뜁니다: orderId={}", order.getId());
                return;
            }

            // 주문 항목을 다시 조회 (트랜잭션 외부에서 접근하므로 LAZY 로딩을 위해)
            Order completedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + order.getId()));

            // 각 주문 항목의 상품별 주문 수량을 랭킹에 반영
            completedOrder.getOrderItems().forEach(orderItem -> {
                Long productId = orderItem.getProduct().getId();
                Integer quantity = orderItem.getQuantity();
                
                rankingService.incrementProductOrderCount(productId, quantity);
                
                log.debug("상품 랭킹 업데이트: orderId={}, productId={}, quantity={}", 
                        order.getId(), productId, quantity);
            });

            log.info("주문 완료 랭킹 업데이트 완료: orderId={}, orderItemsCount={}", 
                    order.getId(), completedOrder.getOrderItems().size());
        } catch (Exception e) {
            // 랭킹 업데이트 실패는 주문 처리에 영향을 주지 않도록 로그만 남기고 예외를 던지지 않음
            log.error("주문 완료 랭킹 업데이트 실패: orderId={}, error={}", 
                    order.getId(), e.getMessage(), e);
        }
    }
}
