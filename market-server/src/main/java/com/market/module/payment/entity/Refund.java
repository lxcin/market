package com.market.module.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_refund")
public class Refund {

    public static final int STATUS_PROCESSING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private String paymentNo;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private String reason;

    private Integer status = STATUS_PROCESSING;

    private String refundTradeNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
