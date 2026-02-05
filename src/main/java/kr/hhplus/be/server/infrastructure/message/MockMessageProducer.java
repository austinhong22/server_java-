package kr.hhplus.be.server.infrastructure.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 메시지 큐(Kafka 등) 대신 사용하는 Mock 메시지 프로듀서
 * 테스트 가능하도록 설계
 */
@Slf4j
@Component
public class MockMessageProducer {

    private boolean shouldFail = false;
    private RuntimeException failureException = null;

    /**
     * 메시지 전송 실패를 시뮬레이션하기 위한 설정
     */
    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    /**
     * 특정 예외를 발생시키도록 설정
     */
    public void setFailureException(RuntimeException exception) {
        this.failureException = exception;
        this.shouldFail = true;
    }

    /**
     * 실패 시뮬레이션 초기화
     */
    public void reset() {
        this.shouldFail = false;
        this.failureException = null;
    }

    /**
     * 메시지 전송
     * @param topic 토픽명
     * @param message 메시지 내용
     * @throws RuntimeException shouldFail이 true인 경우 발생
     */
    public void send(String topic, String message) {
        if (shouldFail) {
            if (failureException != null) {
                throw failureException;
            }
            throw new RuntimeException("메시지 전송 실패 (시뮬레이션)");
        }

        log.info("메시지 전송 성공 - topic: {}, message: {}", topic, message);
    }
}
