package com.market.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.market.common.response.Result;
import com.market.module.coupon.entity.CouponTemplate;
import com.market.module.coupon.mapper.CouponTemplateMapper;
import com.market.module.seckill.service.SeckillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/coupon")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponTemplateMapper couponTemplateMapper;
    private final SeckillService seckillService;

    @GetMapping("/page")
    public Result<Page<CouponTemplate>> page(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "20") Integer size) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CouponTemplate::getCreatedAt);
        return Result.success(couponTemplateMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CouponTemplateRequest request) {
        CouponTemplate template = new CouponTemplate();
        BeanUtils.copyProperties(request, template);
        template.setRemainingCount(request.getTotalCount());
        couponTemplateMapper.insert(template);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CouponTemplateRequest request) {
        CouponTemplate template = new CouponTemplate();
        BeanUtils.copyProperties(request, template);
        template.setId(id);
        template.setRemainingCount(request.getTotalCount());
        couponTemplateMapper.updateById(template);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        CouponTemplate template = new CouponTemplate();
        template.setId(id);
        template.setStatus(request.getStatus());
        couponTemplateMapper.updateById(template);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponTemplateMapper.deleteById(id);
        return Result.success();
    }

    /** 预热秒杀库存到 Redis */
    @PostMapping("/{id}/warmup")
    public Result<java.util.Map<String, Object>> warmup(@PathVariable Long id) {
        return Result.success(seckillService.warmup(id, true));
    }

    @Data
    public static class CouponTemplateRequest {

        @NotBlank(message = "优惠券名称不能为空")
        private String name;

        @NotNull(message = "优惠券类型不能为空")
        private Integer type;

        private BigDecimal thresholdAmount = BigDecimal.ZERO;

        private BigDecimal discountAmount = BigDecimal.ZERO;

        private BigDecimal discountRate = new BigDecimal("1.00");

        @NotNull(message = "发行总量不能为空")
        private Integer totalCount;

        private Integer perUserLimit = 1;

        private LocalDateTime startTime;

        private LocalDateTime endTime;
    }
}
