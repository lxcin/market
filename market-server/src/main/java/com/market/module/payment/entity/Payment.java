package com.market.module.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_payment")
public class Payment {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;
    public static final int STATUS_CLOSED = 3;
    public static final int STATUS_REFUNDED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String channel;

    private BigDecimal amount;

    private Integer status = STATUS_PENDING;

    private String tradeNo;

    private LocalDateTime notifyTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
