package kr.hhplus.be.server.infrastructure.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 기반 분산락 구현체
 * SET NX EX 명령을 사용하여 락을 획득하고, Lua 스크립트를 사용하여 안전하게 락을 해제합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock implements DistributedLock {

	private static final String LOCK_PREFIX = "lock:";
	private static final String LOCK_SCRIPT = 
		"if redis.call('get', KEYS[1]) == ARGV[1] then " +
		"  return redis.call('del', KEYS[1]) " +
		"else " +
		"  return 0 " +
		"end";

	private final RedisTemplate<String, String> redisTemplate;
	private final DefaultRedisScript<Long> unlockScript;

	public RedisDistributedLock(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.unlockScript = new DefaultRedisScript<>();
		this.unlockScript.setScriptText(LOCK_SCRIPT);
		this.unlockScript.setResultType(Long.class);
	}

	@Override
	public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
		String fullLockKey = LOCK_PREFIX + lockKey;
		String lockValue = UUID.randomUUID().toString();
		long startTime = System.currentTimeMillis();

		try {
			// 락 획득 시도
			while (!tryLock(fullLockKey, lockValue, leaseTime)) {
				if (System.currentTimeMillis() - startTime >= waitTime) {
					throw new LockAcquisitionException("락 획득 시간 초과: " + lockKey);
				}
				try {
					Thread.sleep(50); // 50ms 대기 후 재시도
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new LockAcquisitionException("락 획득 중 인터럽트 발생: " + lockKey, e);
				}
			}

			log.debug("락 획득 성공: {}", fullLockKey);
			return supplier.get();
		} finally {
			unlock(fullLockKey, lockValue);
			log.debug("락 해제: {}", fullLockKey);
		}
	}

	@Override
	public void executeWithLock(String lockKey, long waitTime, long leaseTime, Runnable runnable) {
		executeWithLock(lockKey, waitTime, leaseTime, () -> {
			runnable.run();
			return null;
		});
	}

	/**
	 * 락 획득 시도
	 * SET key value NX EX seconds 명령을 사용하여 원자적으로 락을 설정합니다.
	 *
	 * @param key 락 키
	 * @param value 락 값 (UUID)
	 * @param leaseTime 락 유지 시간 (밀리초)
	 * @return 락 획득 성공 여부
	 */
	private boolean tryLock(String key, String value, long leaseTime) {
		Boolean result = redisTemplate.opsForValue().setIfAbsent(
			key,
			value,
			leaseTime,
			TimeUnit.MILLISECONDS
		);
		return Boolean.TRUE.equals(result);
	}

	/**
	 * 락 해제
	 * Lua 스크립트를 사용하여 자신이 획득한 락만 해제합니다.
	 * 이는 락 만료 후 다른 프로세스가 락을 획득한 경우를 방지합니다.
	 *
	 * @param key 락 키
	 * @param value 락 값 (UUID)
	 */
	private void unlock(String key, String value) {
		try {
			Long result = redisTemplate.execute(
				unlockScript,
				Collections.singletonList(key),
				value
			);
			if (result == null || result == 0) {
				log.warn("락 해제 실패 (이미 만료되었거나 다른 프로세스가 획득): {}", key);
			}
		} catch (Exception e) {
			log.error("락 해제 중 오류 발생: {}", key, e);
		}
	}
}
