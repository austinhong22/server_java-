package kr.hhplus.be.server.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.infrastructure.platform.DataPlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 완료 이벤트 리스너
 * 주문이 완료되면 데이터 플랫폼에 주문 정보를 전송합니다.
 * 
 * 설계 고려사항:
 * 1. 비동기 처리: 데이터 플랫폼 전송은 주문 처리 성능에 영향을 주지 않도록 비동기로 처리
 * 2. 트랜잭션 완료 후 처리: AFTER_COMMIT으로 설정하여 주문이 확실히 완료된 후에만 전송
 * 3. 실패 허용: 데이터 플랫폼 전송 실패가 주문 처리에 영향을 주지 않도록 예외를 잡아서 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDataPlatformEventListener {

    private final DataPlatformClient dataPlatformClient;
    private final ObjectMapper objectMapper;

    /**
     * 주문 완료 이벤트를 수신하여 데이터 플랫폼에 전송합니다.
     * 
     * @param event 주문 완료 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            // 주문 정보를 JSON으로 직렬화
            String orderData = objectMapper.writeValueAsString(event);
            
            // 데이터 플랫폼에 전송
            dataPlatformClient.sendOrderData(orderData);
            
            log.info("데이터 플랫폼에 주문 정보 전송 완료: orderId={}, userId={}, finalAmount={}", 
                    event.getOrderId(), event.getUserId(), event.getFinalAmount());
        } catch (JsonProcessingException e) {
            log.error("주문 정보 직렬화 실패: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
        } catch (Exception e) {
            // 데이터 플랫폼 전송 실패는 주문 처리에 영향을 주지 않도록 로그만 남기고 예외를 던지지 않음
            log.error("데이터 플랫폼에 주문 정보 전송 실패: orderId={}, error={}", 
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
