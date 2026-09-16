package com.market.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long couponTemplateId;

    private Integer status = 0;

    private Long orderId;

    private LocalDateTime usedTime;

    private LocalDateTime createdAt;
}
