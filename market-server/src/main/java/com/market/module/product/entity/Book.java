package com.market.module.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String author;

    private String isbn;

    private String publisher;

    private LocalDate publishDate;

    private Long categoryId;

    private BigDecimal price;

    private String coverImage;

    private String description;

    private String detail;

    private Integer sales = 0;

    private Double rate = 0.0;

    private Integer status = 1;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
