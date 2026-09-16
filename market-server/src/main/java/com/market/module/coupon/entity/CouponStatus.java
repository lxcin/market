package com.market.module.coupon.entity;

import lombok.Getter;

@Getter
public enum CouponStatus {

    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期");

    private final Integer code;
    private final String desc;

    CouponStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    public boolean canTransitionTo(CouponStatus target) {
        if (target == null) {
            return false;
        }
        if (this == UNUSED) {
            return target == USED || target == EXPIRED;
        }
        return false;
    }
}
