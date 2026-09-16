package com.market.module.coupon.controller;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponPreviewRequest {

    @NotNull(message = "用户优惠券ID不能为空")
    private Long userCouponId;

    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;
}
