package kr.hhplus.be.server.infrastructure.platform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 데이터 플랫폼에 데이터를 전송하는 Mock API 클라이언트
 * 실제 환경에서는 외부 데이터 플랫폼 API를 호출합니다.
 */
@Slf4j
@Component
public class DataPlatformClientImpl implements DataPlatformClient {

    private static final String ORDER_ENDPOINT = "/api/data-platform/orders";
    private static final String RESERVATION_ENDPOINT = "/api/data-platform/reservations";

    @Value("${data-platform.base-url:http://localhost:8080}")
    private String baseUrl;

    private final HttpClient httpClient;

    public DataPlatformClientImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void sendOrderData(String orderData) {
        try {
            String url = baseUrl + ORDER_ENDPOINT;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(orderData))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("데이터 플랫폼에 주문 정보 전송 성공: status={}, data={}", response.statusCode(), orderData);
            } else {
                log.warn("데이터 플랫폼에 주문 정보 전송 실패: status={}, response={}", response.statusCode(), response.body());
                throw new RuntimeException("데이터 플랫폼 전송 실패: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("데이터 플랫폼에 주문 정보 전송 중 오류 발생: data={}, error={}", orderData, e.getMessage(), e);
            throw new RuntimeException("데이터 플랫폼 전송 중 오류 발생", e);
        }
    }

    @Override
    public void sendReservationData(String reservationData) {
        try {
            String url = baseUrl + RESERVATION_ENDPOINT;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(reservationData))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("데이터 플랫폼에 예약 정보 전송 성공: status={}, data={}", response.statusCode(), reservationData);
            } else {
                log.warn("데이터 플랫폼에 예약 정보 전송 실패: status={}, response={}", response.statusCode(), response.body());
                throw new RuntimeException("데이터 플랫폼 전송 실패: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("데이터 플랫폼에 예약 정보 전송 중 오류 발생: data={}, error={}", reservationData, e.getMessage(), e);
            throw new RuntimeException("데이터 플랫폼 전송 중 오류 발생", e);
        }
    }
}
