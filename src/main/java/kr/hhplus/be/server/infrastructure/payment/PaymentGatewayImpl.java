package kr.hhplus.be.server.infrastructure.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentGatewayImpl implements PaymentGateway {

    @Override
    public void processPayment(Long userId, Long amount) {
        // 외부 결제 게이트웨이 연동 (Mock)
        log.info("결제 게이트웨이를 통한 결제 처리: userId={}, amount={}", userId, amount);
        // 실제로는 외부 API 호출
    }
}
