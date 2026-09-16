package com.market.module.outbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.outbox.entity.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxMapper extends BaseMapper<OutboxMessage> {
}
