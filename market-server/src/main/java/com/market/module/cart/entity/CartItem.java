package com.market.module.cart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_cart")
public class CartItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    private Integer quantity = 1;

    private Integer checked = 1;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
