package com.market.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("t_order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long bookId;

    private String bookTitle;

    private String bookCover;

    private Integer quantity;

    private BigDecimal price;
}
