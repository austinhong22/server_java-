package kr.hhplus.be.server.infrastructure.lock;

import java.util.function.Supplier;

/**
 * 분산락 인터페이스
 * Redis 기반 분산 환경에서 동시성 제어를 위한 락을 제공합니다.
 */
public interface DistributedLock {

	/**
	 * 락을 획득하고 작업을 실행합니다.
	 * 락 획득에 실패하면 LockAcquisitionException을 발생시킵니다.
	 *
	 * @param lockKey 락 키
	 * @param waitTime 락 획득 대기 시간 (밀리초)
	 * @param leaseTime 락 유지 시간 (밀리초)
	 * @param supplier 실행할 작업
	 * @return 작업 결과
	 * @throws LockAcquisitionException 락 획득 실패 시
	 */
	<T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier);

	/**
	 * 락을 획득하고 작업을 실행합니다. (반환값 없음)
	 * 락 획득에 실패하면 LockAcquisitionException을 발생시킵니다.
	 *
	 * @param lockKey 락 키
	 * @param waitTime 락 획득 대기 시간 (밀리초)
	 * @param leaseTime 락 유지 시간 (밀리초)
	 * @param runnable 실행할 작업
	 * @throws LockAcquisitionException 락 획득 실패 시
	 */
	void executeWithLock(String lockKey, long waitTime, long leaseTime, Runnable runnable);
}
