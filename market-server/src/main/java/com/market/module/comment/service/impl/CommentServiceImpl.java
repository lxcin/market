package com.market.module.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.exception.BusinessException;
import com.market.module.comment.entity.Comment;
import com.market.module.comment.entity.CommentVO;
import com.market.module.comment.mapper.CommentMapper;
import com.market.module.comment.service.CommentService;
import com.market.module.product.entity.Book;
import com.market.module.product.mapper.BookMapper;
import com.market.module.user.entity.User;
import com.market.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;

    @Override
    public List<CommentVO> listByBook(Long bookId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getBookId, bookId)
                        .orderByDesc(Comment::getCreatedAt));
        return buildTree(comments);
    }

    @Override
    public List<CommentVO> listByUser(Long userId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, userId)
                        .orderByDesc(Comment::getCreatedAt));
        return toVOList(comments);
    }

    @Override
    public List<CommentVO> listReplies(Long parentId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getParentId, parentId)
                        .orderByAsc(Comment::getCreatedAt));
        return toVOList(comments);
    }

    @Override
    public CommentVO addComment(Long userId, Long bookId, String content, Integer rate, Long parentId) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("评论内容不能为空");
        }
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("回复的评论不存在");
            }
        }

        int safeRate = rate == null ? 5 : Math.min(Math.max(rate, 1), 5);

        Comment comment = new Comment();
        comment.setBookId(bookId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setRate(safeRate);
        comment.setParentId(parentId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        User user = userMapper.selectById(userId);
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setBookId(comment.getBookId());
        vo.setUserId(comment.getUserId());
        vo.setUsername(user != null ? user.getUsername() : null);
        vo.setContent(comment.getContent());
        vo.setRate(comment.getRate());
        vo.setParentId(comment.getParentId());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new BusinessException("无权删除该评论");
        }
        commentMapper.deleteById(commentId);
    }

    private List<CommentVO> buildTree(List<Comment> comments) {
        Map<Long, CommentVO> voMap = toVOList(comments).stream()
                .collect(Collectors.toMap(CommentVO::getId, Function.identity()));

        List<CommentVO> roots = new ArrayList<>();
        for (CommentVO vo : voMap.values()) {
            if (vo.getParentId() == null) {
                roots.add(vo);
            } else {
                CommentVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    parent.getReplies().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    private List<CommentVO> toVOList(List<Comment> comments) {
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> usernameMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername));

        return comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setBookId(comment.getBookId());
            vo.setUserId(comment.getUserId());
            vo.setUsername(usernameMap.get(comment.getUserId()));
            vo.setContent(comment.getContent());
            vo.setRate(comment.getRate());
            vo.setParentId(comment.getParentId());
            vo.setCreatedAt(comment.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }
}
