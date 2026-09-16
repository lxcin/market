package com.market.module.payment.service;

import java.util.Map;

public interface PaymentService {

    Map<String, Object> createPayment(Long userId, Long orderId);

    Map<String, Object> getPayment(Long userId, Long orderId);

    String handleNotify(Map<String, String> params);

    Map<String, Object> applyRefund(Long userId, Long orderId, String reason);

    String handleRefundNotify(Map<String, String> params);

    void sandboxPay(Long userId, String paymentNo);

    void sandboxRefund(Long userId, String refundNo);

    Map<String, Object> reconcile(int days);
}
