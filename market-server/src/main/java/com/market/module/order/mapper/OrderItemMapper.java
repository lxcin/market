package com.market.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /** 各图书当前被"待支付订单"预占的数量（用于库存锁定对账） */
    @Select("SELECT oi.book_id AS bookId, SUM(oi.quantity) AS qty "
            + "FROM t_order_item oi JOIN t_order o ON oi.order_id = o.id "
            + "WHERE o.status = 0 AND o.deleted = 0 GROUP BY oi.book_id")
    List<Map<String, Object>> sumPendingQuantityByBook();
}
