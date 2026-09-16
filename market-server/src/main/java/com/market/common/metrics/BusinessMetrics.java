package com.market.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 业务指标埋点：只记录关键业务事件（非每一步）。
 * 全部以 Counter 形式暴露为 Prometheus 指标，供 Grafana 展示与告警。
 */
@Component
public class BusinessMetrics {

    private final Counter orderCreated;
    private final Counter paymentSuccess;
    private final Counter paymentFailed;
    private final Counter refundSuccess;
    private final Counter seckillAccepted;
    private final Counter seckillSoldOut;
    private final Counter seckillRejected;
    private final Counter loginFailure;
    private final Counter reconcileMismatch;

    public BusinessMetrics(MeterRegistry registry) {
        this.orderCreated = Counter.builder("business_order_placed_total")
                .description("创建订单数").register(registry);
        this.paymentSuccess = Counter.builder("business_payment_success_total")
                .description("支付成功回调数").register(registry);
        this.paymentFailed = Counter.builder("business_payment_failed_total")
                .description("支付失败回调数").register(registry);
        this.refundSuccess = Counter.builder("business_refund_success_total")
                .description("退款成功数").register(registry);
        this.seckillAccepted = Counter.builder("business_seckill_accepted_total")
                .description("秒杀领取受理数").register(registry);
        this.seckillSoldOut = Counter.builder("business_seckill_sold_out_total")
                .description("秒杀已抢完数").register(registry);
        this.seckillRejected = Counter.builder("business_seckill_rejected_total")
                .description("秒杀超限领/限流数").register(registry);
        this.loginFailure = Counter.builder("business_login_failure_total")
                .description("登录失败次数").register(registry);
        this.reconcileMismatch = Counter.builder("business_reconcile_mismatch_total")
                .description("对账发现的不一致条数").register(registry);
    }

    public void orderCreated() {
        orderCreated.increment();
    }

    public void paymentSuccess() {
        paymentSuccess.increment();
    }

    public void paymentFailed() {
        paymentFailed.increment();
    }

    public void refundSuccess() {
        refundSuccess.increment();
    }

    public void seckillAccepted() {
        seckillAccepted.increment();
    }

    public void seckillSoldOut() {
        seckillSoldOut.increment();
    }

    public void seckillRejected() {
        seckillRejected.increment();
    }

    public void loginFailure() {
        loginFailure.increment();
    }

    public void reconcileMismatch() {
        reconcileMismatch.increment();
    }
}
