package kr.hhplus.be.server.domain.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.status = 'ACTIVE'")
    Long countActiveCoupons();

    List<Coupon> findByUserIdAndStatus(Long userId, CouponStatus status);

    Optional<Coupon> findByIdAndUserId(Long id, Long userId);
}
