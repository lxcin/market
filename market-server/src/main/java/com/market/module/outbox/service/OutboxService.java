package com.market.module.outbox.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.module.outbox.entity.OutboxMessage;
import com.market.module.outbox.mapper.OutboxMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 本地消息表（Transactional Outbox）。
 * publish() 必须与业务写库在同一个事务里调用，从而保证"业务数据 + 事件"原子落库；
 * 由独立投递器异步消费，实现最终一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final int MAX_RETRY = 5;

    private final OutboxMapper outboxMapper;
    private final List<OutboxEventHandler> handlers;

    private Map<String, OutboxEventHandler> handlerMap;

    @PostConstruct
    public void init() {
        handlerMap = handlers.stream()
                .collect(Collectors.toMap(OutboxEventHandler::eventType, Function.identity(), (a, b) -> a));
        log.info("Outbox 处理器注册: {}", handlerMap.keySet());
    }

    /** 在业务事务内登记事件 */
    public void publish(String eventType, String aggregateType, Long aggregateId, Object payload) {
        OutboxMessage message = new OutboxMessage();
        message.setEventType(eventType);
        message.setAggregateType(aggregateType);
        message.setAggregateId(aggregateId);
        message.setPayload(payload == null ? null : JSONUtil.toJsonStr(payload));
        message.setStatus(OutboxMessage.STATUS_PENDING);
        message.setRetryCount(0);
        message.setCreatedAt(LocalDateTime.now());
        outboxMapper.insert(message);
    }

    /** 投递一批待处理事件（独立事务） */
    @Transactional(rollbackFor = Exception.class)
    public int dispatchOnce() {
        List<OutboxMessage> list = outboxMapper.selectList(
                new LambdaQueryWrapper<OutboxMessage>()
                        .eq(OutboxMessage::getStatus, OutboxMessage.STATUS_PENDING)
                        .orderByAsc(OutboxMessage::getId)
                        .last("LIMIT 50"));
        int delivered = 0;
        for (OutboxMessage message : list) {
            OutboxEventHandler handler = handlerMap.get(message.getEventType());
            try {
                if (handler == null) {
                    throw new IllegalStateException("无处理器: " + message.getEventType());
                }
                handler.handle(message);
                message.setStatus(OutboxMessage.STATUS_DELIVERED);
                message.setLastError(null);
                delivered++;
            } catch (Exception e) {
                int retry = message.getRetryCount() == null ? 0 : message.getRetryCount();
                message.setRetryCount(retry + 1);
                message.setLastError(truncate(e.getMessage()));
                if (message.getRetryCount() >= MAX_RETRY) {
                    message.setStatus(OutboxMessage.STATUS_FAILED);
                    log.error("Outbox 事件投递失败(放弃) id={} type={}", message.getId(), message.getEventType(), e);
                } else {
                    log.warn("Outbox 事件投递失败(重试 {}) id={} type={}", message.getRetryCount(), message.getId(),
                            message.getEventType(), e);
                }
            }
            outboxMapper.updateById(message);
        }
        return delivered;
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 250 ? s.substring(0, 250) : s;
    }
}
