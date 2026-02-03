package kr.hhplus.be.server.api.coupon.dto;

import kr.hhplus.be.server.domain.coupon.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String name;
    private Integer discountRate;
    private LocalDateTime expiredAt;
    private String status;

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getExpiredAt(),
                coupon.getStatus().name()
        );
    }
}
