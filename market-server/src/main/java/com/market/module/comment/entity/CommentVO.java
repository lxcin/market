package com.market.module.comment.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentVO {

    private Long id;

    private Long bookId;

    private Long userId;

    private String username;

    private String content;

    private Integer rate;

    private Long parentId;

    private LocalDateTime createdAt;

    private List<CommentVO> replies = new ArrayList<>();
}
