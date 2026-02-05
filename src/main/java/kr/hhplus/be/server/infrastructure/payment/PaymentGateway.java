package kr.hhplus.be.server.infrastructure.payment;

public interface PaymentGateway {
    void processPayment(Long userId, Long amount);
    void processPayment(Long userId, Long amount, String idempotencyKey);
}
