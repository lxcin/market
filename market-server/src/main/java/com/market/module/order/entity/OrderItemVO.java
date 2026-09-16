package com.market.module.order.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemVO {

    private Long bookId;

    private String bookTitle;

    private String bookCover;

    private Integer quantity;

    private BigDecimal price;
}
