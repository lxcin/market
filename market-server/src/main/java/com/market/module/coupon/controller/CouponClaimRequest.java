package com.market.module.coupon.controller;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponClaimRequest {

    @NotNull(message = "优惠券模板ID不能为空")
    private Long templateId;
}
