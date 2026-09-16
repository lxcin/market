package com.market.module.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
