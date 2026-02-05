package kr.hhplus.be.server.infrastructure.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
