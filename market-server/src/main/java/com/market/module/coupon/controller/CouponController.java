package com.market.module.coupon.controller;

import com.market.common.annotation.RepeatSubmit;
import com.market.common.response.Result;
import com.market.common.util.SecurityUtil;
import com.market.module.coupon.entity.CouponTemplate;
import com.market.module.coupon.entity.UserCoupon;
import com.market.module.coupon.service.CouponService;
import com.market.module.seckill.service.SeckillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final SeckillService seckillService;

    @GetMapping("/template/list")
    public Result<List<CouponTemplate>> listTemplates() {
        return Result.success(couponService.listAvailableTemplates());
    }

    @RepeatSubmit(interval = 2)
    @PostMapping("/claim/{templateId}")
    public Result<Map<String, Object>> claim(@PathVariable Long templateId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(seckillService.claim(userId, templateId));
    }

    @GetMapping("/seckill/stock/{templateId}")
    public Result<Map<String, Object>> seckillStock(@PathVariable Long templateId) {
        return Result.success(seckillService.stockInfo(templateId));
    }

    @GetMapping("/my")
    public Result<List<UserCoupon>> myCoupons() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(couponService.listUserCoupons(userId));
    }

    @PostMapping("/preview")
    public Result<Map<String, Object>> preview(@Valid @RequestBody CouponPreviewRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(couponService.previewDiscount(userId, request.getUserCouponId(), request.getOrderAmount()));
    }

    @GetMapping("/best-fit")
    public Result<List<Map<String, Object>>> bestFit(@RequestParam @NotNull BigDecimal orderAmount) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(couponService.findBestCoupons(userId, orderAmount));
    }

    @Data
    public static class BestFitRequest {
        @NotNull
        private BigDecimal orderAmount;
    }
}
