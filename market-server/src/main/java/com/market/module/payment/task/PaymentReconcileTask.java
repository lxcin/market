package com.market.module.payment.task;

import com.market.module.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日与支付网关对账（真实场景应下载网关账单文件逐笔比对）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconcileTask {

    private final PaymentService paymentService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReconcile() {
        log.info("开始每日支付对账...");
        try {
            paymentService.reconcile(1);
        } catch (Exception e) {
            log.error("每日对账执行失败", e);
        }
    }
}
