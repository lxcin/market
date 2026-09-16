package com.market.module.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模拟第三方网关的对账单记录（仅用于演示对账逻辑）。
 */
@Data
@TableName("t_gateway_bill")
public class GatewayBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;

    private String tradeNo;

    private BigDecimal amount;

    private String status;

    private LocalDateTime billTime;
}
