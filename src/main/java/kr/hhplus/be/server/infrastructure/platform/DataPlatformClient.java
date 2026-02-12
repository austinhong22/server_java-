package kr.hhplus.be.server.infrastructure.platform;

/**
 * 데이터 플랫폼에 데이터를 전송하는 클라이언트 인터페이스
 */
public interface DataPlatformClient {
    /**
     * 주문 정보를 데이터 플랫폼에 전송
     * @param orderData 주문 데이터 (JSON 문자열)
     */
    void sendOrderData(String orderData);

    /**
     * 예약 정보를 데이터 플랫폼에 전송
     * @param reservationData 예약 데이터 (JSON 문자열)
     */
    void sendReservationData(String reservationData);
}
