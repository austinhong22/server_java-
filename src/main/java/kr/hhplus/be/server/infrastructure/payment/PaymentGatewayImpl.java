package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayImpl implements PaymentGateway {

    private final PaymentRepository paymentRepository;

    @Override
    public void processPayment(Long userId, Long amount) {
        processPayment(userId, amount, null);
    }

    @Override
    public void processPayment(Long userId, Long amount, String idempotencyKey) {
        // idempotency_key가 있는 경우 중복 요청 확인
        if (idempotencyKey != null) {
            Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPayment.isPresent() && existingPayment.get().isCompleted()) {
                log.info("중복 결제 요청 무시: userId={}, amount={}, idempotencyKey={}", 
                        userId, amount, idempotencyKey);
                return; // 이미 처리된 요청이므로 무시
            }
        }

        // 외부 결제 게이트웨이 연동 (Mock)
        log.info("결제 게이트웨이를 통한 결제 처리: userId={}, amount={}, idempotencyKey={}", 
                userId, amount, idempotencyKey);
        // 실제로는 외부 API 호출
    }
}
