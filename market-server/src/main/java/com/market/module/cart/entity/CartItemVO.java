package com.market.module.cart.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemVO {

    private Long bookId;

    private String title;

    private String author;

    private String coverImage;

    private BigDecimal price;

    private Integer quantity;

    private Boolean checked;
}
