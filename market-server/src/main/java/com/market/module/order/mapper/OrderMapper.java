package com.market.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    int payOrderCas(@Param("id") Long id);

    int cancelOrderCas(@Param("id") Long id);
}
