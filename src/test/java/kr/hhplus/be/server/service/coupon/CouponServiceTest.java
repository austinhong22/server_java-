package kr.hhplus.be.server.service.coupon;

import kr.hhplus.be.server.api.coupon.dto.CouponResponse;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.CouponStatus;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService 단위 테스트")
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .balance(10000L)
                .build();
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCouponSuccess() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(couponRepository.countActiveCouponsWithLock()).thenReturn(50L);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon coupon = invocation.getArgument(0);
            return coupon;
        });

        // when
        CouponResponse result = couponService.issueCoupon(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("선착순 할인 쿠폰");
        assertThat(result.getDiscountRate()).isEqualTo(10);
        verify(couponRepository).countActiveCouponsWithLock();
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 사용자를 찾을 수 없음")
    void issueCouponUserNotFound() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰 발급 한도 초과")
    void issueCouponLimitExceeded() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(couponRepository.countActiveCouponsWithLock()).thenReturn(100L); // 최대 한도

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰 발급 한도에 도달했습니다.");
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 성공")
    void getUserCouponsSuccess() {
        // given
        Long userId = 1L;
        Coupon coupon1 = Coupon.builder()
                .user(user)
                .name("쿠폰1")
                .discountRate(10)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .build();
        Coupon coupon2 = Coupon.builder()
                .user(user)
                .name("쿠폰2")
                .discountRate(20)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .build();

        when(couponRepository.findByUserIdAndStatus(userId, CouponStatus.ACTIVE))
                .thenReturn(Arrays.asList(coupon1, coupon2));

        // when
        List<CouponResponse> result = couponService.getUserCoupons(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("쿠폰1");
        assertThat(result.get(1).getName()).isEqualTo("쿠폰2");
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 - 쿠폰 없음")
    void getUserCouponsEmpty() {
        // given
        Long userId = 1L;
        when(couponRepository.findByUserIdAndStatus(userId, CouponStatus.ACTIVE))
                .thenReturn(Arrays.asList());

        // when
        List<CouponResponse> result = couponService.getUserCoupons(userId);

        // then
        assertThat(result).isEmpty();
    }
}
