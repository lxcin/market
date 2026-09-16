package com.market.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.payment.entity.GatewayBill;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GatewayBillMapper extends BaseMapper<GatewayBill> {
}
