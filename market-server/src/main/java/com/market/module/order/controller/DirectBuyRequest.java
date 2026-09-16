package com.market.module.order.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DirectBuyRequest {

    @NotBlank(message = "幂等键不能为空")
    private String idempotentKey;

    @NotNull(message = "商品ID不能为空")
    private Long bookId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    private Long userCouponId;
}
