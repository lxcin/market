package com.market.module.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.comment.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
