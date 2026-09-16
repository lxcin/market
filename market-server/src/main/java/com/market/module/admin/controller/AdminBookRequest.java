package com.market.module.admin.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdminBookRequest {

    @NotBlank(message = "书名不能为空")
    private String title;

    @NotBlank(message = "作者不能为空")
    private String author;

    private String isbn;

    private String publisher;

    private LocalDate publishDate;

    private Long categoryId;

    private BigDecimal price;

    private String coverImage;

    private String description;

    private String detail;

    private Integer status;
}
