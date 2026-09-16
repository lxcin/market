package com.market.module.comment.controller;

import com.market.common.response.Result;
import com.market.common.util.SecurityUtil;
import com.market.module.comment.entity.CommentVO;
import com.market.module.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/book/{bookId}")
    public Result<List<CommentVO>> listByBook(@PathVariable Long bookId) {
        return Result.success(commentService.listByBook(bookId));
    }

    @GetMapping("/my")
    public Result<List<CommentVO>> myComments() {
        return Result.success(commentService.listByUser(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{parentId}/replies")
    public Result<List<CommentVO>> replies(@PathVariable Long parentId) {
        return Result.success(commentService.listReplies(parentId));
    }

    @PostMapping
    public Result<CommentVO> add(@Valid @RequestBody CommentCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(commentService.addComment(userId, request.getBookId(),
                request.getContent(), request.getRate(), request.getParentId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(SecurityUtil.getCurrentUserId(), id);
        return Result.success();
    }
}
