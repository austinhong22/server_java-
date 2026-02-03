package kr.hhplus.be.server.api.coupon;

import kr.hhplus.be.server.api.coupon.dto.CouponIssueRequest;
import kr.hhplus.be.server.api.coupon.dto.CouponResponse;
import kr.hhplus.be.server.service.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/issue")
    public ResponseEntity<CouponResponse> issueCoupon(@RequestBody CouponIssueRequest request) {
        CouponResponse response = couponService.issueCoupon(request.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<CouponResponse>> getUserCoupons(@PathVariable Long userId) {
        List<CouponResponse> coupons = couponService.getUserCoupons(userId);
        return ResponseEntity.ok(coupons);
    }
}
