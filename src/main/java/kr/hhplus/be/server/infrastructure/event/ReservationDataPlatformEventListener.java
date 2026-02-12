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
 * 예약 완료 이벤트 리스너
 * 예약이 완료되면 데이터 플랫폼에 예약 정보를 전송합니다.
 * 
 * 설계 고려사항:
 * 1. 비동기 처리: 데이터 플랫폼 전송은 예약 처리 성능에 영향을 주지 않도록 비동기로 처리
 * 2. 트랜잭션 완료 후 처리: AFTER_COMMIT으로 설정하여 예약이 확실히 완료된 후에만 전송
 * 3. 실패 허용: 데이터 플랫폼 전송 실패가 예약 처리에 영향을 주지 않도록 예외를 잡아서 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationDataPlatformEventListener {

    private final DataPlatformClient dataPlatformClient;
    private final ObjectMapper objectMapper;

    /**
     * 예약 완료 이벤트를 수신하여 데이터 플랫폼에 전송합니다.
     * 
     * @param event 예약 완료 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationConfirmed(ReservationConfirmedEvent event) {
        try {
            // 예약 정보를 JSON으로 직렬화
            String reservationData = objectMapper.writeValueAsString(event);
            
            // 데이터 플랫폼에 전송
            dataPlatformClient.sendReservationData(reservationData);
            
            log.info("데이터 플랫폼에 예약 정보 전송 완료: reservationId={}, userId={}, concertName={}, totalAmount={}", 
                    event.getReservationId(), event.getUserId(), event.getConcertName(), event.getTotalAmount());
        } catch (JsonProcessingException e) {
            log.error("예약 정보 직렬화 실패: reservationId={}, error={}", event.getReservationId(), e.getMessage(), e);
        } catch (Exception e) {
            // 데이터 플랫폼 전송 실패는 예약 처리에 영향을 주지 않도록 로그만 남기고 예외를 던지지 않음
            log.error("데이터 플랫폼에 예약 정보 전송 실패: reservationId={}, error={}", 
                    event.getReservationId(), e.getMessage(), e);
        }
    }
}
