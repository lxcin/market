package com.market.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /** 待支付 -> 支付成功（幂等：重复回调不会再次生效） */
    int markPaid(@Param("paymentNo") String paymentNo, @Param("tradeNo") String tradeNo);

    /** 待支付 -> 支付失败 */
    int markFailed(@Param("paymentNo") String paymentNo);

    /** 支付成功 -> 已退款 */
    int markRefunded(@Param("paymentNo") String paymentNo);
}
