package com.market.module.outbox.task;

import com.market.module.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 投递器：定时扫描待处理事件并投递。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 2000)
    public void dispatch() {
        try {
            int delivered = outboxService.dispatchOnce();
            if (delivered > 0) {
                log.debug("Outbox 本轮投递 {} 条", delivered);
            }
        } catch (Exception e) {
            log.error("Outbox 投递异常", e);
        }
    }
}
