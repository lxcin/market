package com.market.module.order.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateRequest {

    @NotBlank(message = "幂等键不能为空")
    private String idempotentKey;

    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    private Long userCouponId;
}
