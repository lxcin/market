package com.market.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.payment.entity.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefundMapper extends BaseMapper<Refund> {

    /** 处理中 -> 退款成功（幂等） */
    int markSuccess(@Param("refundNo") String refundNo, @Param("refundTradeNo") String refundTradeNo);

    /** 处理中 -> 退款失败 */
    int markFailed(@Param("refundNo") String refundNo);
}
