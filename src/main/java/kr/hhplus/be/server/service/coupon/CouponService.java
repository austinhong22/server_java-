package kr.hhplus.be.server.service.coupon;

import kr.hhplus.be.server.api.coupon.dto.CouponResponse;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.CouponStatus;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private static final int MAX_COUPON_COUNT = 100;
    private static final int DISCOUNT_RATE = 10;
    private static final int EXPIRY_DAYS = 30;

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    public CouponResponse issueCoupon(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 선착순 쿠폰 발급 제한 확인 (비관적 락)
        Long activeCouponCount = couponRepository.countActiveCoupons();
        if (activeCouponCount >= MAX_COUPON_COUNT) {
            throw new IllegalStateException("쿠폰 발급 한도에 도달했습니다.");
        }

        // 쿠폰 생성
        Coupon coupon = Coupon.builder()
                .user(user)
                .name("선착순 할인 쿠폰")
                .discountRate(DISCOUNT_RATE)
                .expiredAt(LocalDateTime.now().plusDays(EXPIRY_DAYS))
                .build();

        couponRepository.save(coupon);

        return CouponResponse.from(coupon);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getUserCoupons(Long userId) {
        List<Coupon> coupons = couponRepository.findByUserIdAndStatus(userId, CouponStatus.ACTIVE);
        return coupons.stream()
                .map(CouponResponse::from)
                .collect(Collectors.toList());
    }
}
