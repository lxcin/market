package com.market.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.market.common.response.Result;
import com.market.module.order.entity.Order;
import com.market.module.order.entity.OrderItem;
import com.market.module.order.entity.OrderItemVO;
import com.market.module.order.entity.OrderStatus;
import com.market.module.order.entity.OrderVO;
import com.market.module.order.mapper.OrderItemMapper;
import com.market.module.order.mapper.OrderMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @GetMapping("/page")
    public Result<Page<Order>> page(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "20") Integer size,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Order::getOrderNo, keyword).or().like(Order::getReceiverName, keyword));
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return Result.success(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> getDetail(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList()));
        return Result.success(vo);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        Order existing = orderMapper.selectById(id);
        if (existing == null) {
            return Result.error("订单不存在");
        }
        OrderStatus from = OrderStatus.of(existing.getStatus());
        OrderStatus to = OrderStatus.of(request.getStatus());
        if (!from.canTransitionTo(to)) {
            return Result.error("非法状态流转: " + from.getDesc() + " -> " + to.getDesc());
        }
        Order order = new Order();
        order.setId(id);
        order.setStatus(request.getStatus());
        orderMapper.updateById(order);
        return Result.success();
    }
}
