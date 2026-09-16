package com.market.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.market.common.annotation.RepeatSubmit;
import com.market.common.response.Result;
import com.market.common.util.SecurityUtil;
import com.market.module.order.entity.OrderVO;
import com.market.module.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @RepeatSubmit(interval = 3)
    @PostMapping("/create")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(orderService.createOrder(userId, request.getIdempotentKey(),
                request.getAddressId(), request.getUserCouponId()));
    }

    @RepeatSubmit(interval = 3)
    @PostMapping("/direct-buy")
    public Result<OrderVO> directBuy(@Valid @RequestBody DirectBuyRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(orderService.directBuy(userId, request.getIdempotentKey(),
                request.getBookId(), request.getQuantity(),
                request.getAddressId(), request.getUserCouponId()));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> getDetail(@PathVariable Long id) {
        return Result.success(orderService.getOrder(id, SecurityUtil.getCurrentUserId(), SecurityUtil.isAdmin()));
    }

    @GetMapping("/page")
    public Result<Page<OrderVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(orderService.pageUserOrders(userId, page, size));
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        orderService.cancelOrder(id, userId);
        return Result.success();
    }
}
