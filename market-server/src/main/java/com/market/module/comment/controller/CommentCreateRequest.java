package com.market.module.comment.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateRequest {

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Integer rate = 5;

    private Long parentId;
}
