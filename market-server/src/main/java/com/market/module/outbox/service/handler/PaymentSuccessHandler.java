package com.market.module.outbox.service.handler;

import com.market.module.outbox.entity.OutboxMessage;
import com.market.module.outbox.service.OutboxEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付成功事件：可用于发通知、加积分、报表等（此处仅记录，示例最后一致性消费者）。
 */
@Slf4j
@Component
public class PaymentSuccessHandler implements OutboxEventHandler {

    @Override
    public String eventType() {
        return "PAYMENT_SUCCESS";
    }

    @Override
    public void handle(OutboxMessage message) {
        log.info("Outbox[PAYMENT_SUCCESS] 处理完成 orderId={} payload={}",
                message.getAggregateId(), message.getPayload());
    }
}
