package com.market.module.outbox.service;

import com.market.module.outbox.entity.OutboxMessage;

/**
 * Outbox 事件处理器：实现必须幂等（同一事件可能被重复投递）。
 */
public interface OutboxEventHandler {

    String eventType();

    void handle(OutboxMessage message) throws Exception;
}
