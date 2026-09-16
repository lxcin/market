package com.market.module.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.market.module.order.entity.OrderVO;

public interface OrderService {

    OrderVO createOrder(Long userId, String idempotentKey, Long addressId, Long userCouponId);

    OrderVO directBuy(Long userId, String idempotentKey, Long bookId, Integer quantity,
                      Long addressId, Long userCouponId);

    OrderVO getOrder(Long orderId, Long userId, boolean admin);

    Page<OrderVO> pageUserOrders(Long userId, Integer page, Integer size);

    void cancelOrder(Long orderId, Long userId);

    /** 系统超时关单（释放预占库存 + 退还优惠券），CAS 幂等 */
    void cancelTimeoutOrder(Long orderId);
}
