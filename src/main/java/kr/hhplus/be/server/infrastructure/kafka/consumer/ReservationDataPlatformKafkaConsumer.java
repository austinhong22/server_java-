package kr.hhplus.be.server.infrastructure.kafka.consumer;

import kr.hhplus.be.server.infrastructure.platform.DataPlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class ReservationDataPlatformKafkaConsumer {

    private final DataPlatformClient dataPlatformClient;

    @KafkaListener(
            topics = "${app.kafka.topics.reservation-events:reservation-events}",
            groupId = "${app.kafka.consumer-groups.reservation-data-platform:reservation-data-platform-consumer}"
    )
    public void consumeReservationEvent(String payload) {
        try {
            dataPlatformClient.sendReservationData(payload);
            log.info("예약 이벤트 소비 후 데이터 플랫폼 전송 완료");
        } catch (Exception e) {
            log.error("예약 이벤트 소비 처리 실패: error={}", e.getMessage(), e);
        }
    }
}
