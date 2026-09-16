package com.market.module.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.module.order.entity.Order;
import com.market.module.order.mapper.OrderMapper;
import com.market.module.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时关单兜底扫描：正常情况下由延时队列精确关单，
 * 这里低频扫描作为安全网，防止延时消息丢失导致订单永不关闭。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Value("${order.pay-timeout-seconds:1800}")
    private long payTimeoutSeconds;

    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    public void scanTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusSeconds(payTimeoutSeconds);
        List<Order> timeoutOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 0)
                        .lt(Order::getCreatedAt, deadline));
        for (Order order : timeoutOrders) {
            try {
                orderService.cancelTimeoutOrder(order.getId());
            } catch (Exception e) {
                log.error("兜底关单失败 orderId={}", order.getId(), e);
            }
        }
    }
}
