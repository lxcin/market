package com.market.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer type;

    private BigDecimal thresholdAmount = BigDecimal.ZERO;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal discountRate = new BigDecimal("1.00");

    private Integer totalCount;

    private Integer remainingCount;

    private Integer perUserLimit = 1;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status = 1;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
