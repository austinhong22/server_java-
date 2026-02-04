package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.common.BaseEntity;
import kr.hhplus.be.server.domain.user.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer discountRate;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Builder
    public Coupon(User user, String name, Integer discountRate, LocalDateTime expiredAt) {
        this.user = user;
        this.name = name;
        this.discountRate = discountRate;
        this.expiredAt = expiredAt;
        this.status = CouponStatus.ACTIVE;
    }

    public void use() {
        if (this.status != CouponStatus.ACTIVE) {
            throw new IllegalArgumentException("사용할 수 없는 쿠폰입니다.");
        }
        if (isExpired()) {
            throw new IllegalArgumentException("만료된 쿠폰입니다.");
        }
        this.status = CouponStatus.USED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiredAt);
    }

    public boolean isUsable() {
        return this.status == CouponStatus.ACTIVE && !isExpired();
    }

    public Long calculateDiscount(Long totalAmount) {
        if (!isUsable()) {
            return 0L;
        }
        return totalAmount * this.discountRate / 100;
    }
}
