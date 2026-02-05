package kr.hhplus.be.server.domain.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.status = 'ACTIVE'")
    Long countActiveCoupons();

    /**
     * SELECT FOR UPDATE를 사용한 활성 쿠폰 개수 조회 (동시성 제어)
     * 비관적 락을 사용하여 동시 발급 시 초과 발급 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.status = 'ACTIVE'")
    Long countActiveCouponsWithLock();

    List<Coupon> findByUserIdAndStatus(Long userId, CouponStatus status);

    Optional<Coupon> findByIdAndUserId(Long id, Long userId);
}
