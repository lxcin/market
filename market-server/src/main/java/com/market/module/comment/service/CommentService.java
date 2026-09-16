package com.market.module.comment.service;

import com.market.module.comment.entity.CommentVO;

import java.util.List;

public interface CommentService {

    List<CommentVO> listByBook(Long bookId);

    List<CommentVO> listByUser(Long userId);

    List<CommentVO> listReplies(Long parentId);

    CommentVO addComment(Long userId, Long bookId, String content, Integer rate, Long parentId);

    void deleteComment(Long userId, Long commentId);
}
