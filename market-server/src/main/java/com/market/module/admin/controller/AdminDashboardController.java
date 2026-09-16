package com.market.module.admin.controller;

import com.market.common.response.Result;
import com.market.module.order.mapper.OrderMapper;
import com.market.module.product.mapper.BookMapper;
import com.market.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final OrderMapper orderMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", userMapper.selectCount(null));
        result.put("totalBooks", bookMapper.selectCount(null));
        result.put("totalOrders", orderMapper.selectCount(null));

        java.time.LocalDateTime todayStart = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        result.put("todayOrders", orderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.market.module.order.entity.Order>()
                        .ge(com.market.module.order.entity.Order::getCreatedAt, todayStart)));

        BigDecimal totalRevenue = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.market.module.order.entity.Order>()
                        .eq(com.market.module.order.entity.Order::getStatus, 1))
                .stream()
                .map(com.market.module.order.entity.Order::getPayAmount)
                .filter(payAmount -> payAmount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalRevenue", totalRevenue);

        return Result.success(result);
    }
}
