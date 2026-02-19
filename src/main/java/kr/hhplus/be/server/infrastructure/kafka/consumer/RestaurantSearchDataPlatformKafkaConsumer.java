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
public class RestaurantSearchDataPlatformKafkaConsumer {

    private final DataPlatformClient dataPlatformClient;

    @KafkaListener(
            topics = "${app.kafka.topics.restaurant-search-events:restaurant-search-events}",
            groupId = "${app.kafka.consumer-groups.restaurant-search-data-platform:restaurant-search-data-platform-consumer}"
    )
    public void consumeSearchEvent(String payload) {
        try {
            dataPlatformClient.sendSearchData(payload);
            log.info("맛집 검색 이벤트 소비 후 데이터 플랫폼 전송 완료");
        } catch (Exception e) {
            log.error("맛집 검색 이벤트 소비 처리 실패: error={}", e.getMessage(), e);
        }
    }
}
