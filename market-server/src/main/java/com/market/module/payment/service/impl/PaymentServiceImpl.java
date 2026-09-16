package com.market.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.exception.BusinessException;
import com.market.common.metrics.BusinessMetrics;
import com.market.common.util.PaySignUtil;
import com.market.module.inventory.service.InventoryService;
import com.market.module.order.entity.Order;
import com.market.module.order.entity.OrderItem;
import com.market.module.order.mapper.OrderItemMapper;
import com.market.module.order.mapper.OrderMapper;
import com.market.module.outbox.service.OutboxService;
import com.market.module.payment.entity.GatewayBill;
import com.market.module.payment.entity.Payment;
import com.market.module.payment.entity.Refund;
import com.market.module.payment.mapper.GatewayBillMapper;
import com.market.module.payment.mapper.PaymentMapper;
import com.market.module.payment.mapper.RefundMapper;
import com.market.module.payment.service.MockPayGatewayService;
import com.market.module.payment.service.PaymentService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final long PAY_TIMEOUT_MINUTES = 30;

    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final GatewayBillMapper gatewayBillMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;
    private final MockPayGatewayService mockPayGateway;
    private final BusinessMetrics businessMetrics;
    private final OutboxService outboxService;
    private final SecureRandom random = new SecureRandom();

    @Override
    public Map<String, Object> createPayment(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BusinessException("订单状态不可支付");
        }
        if (order.getCreatedAt() != null
                && order.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(PAY_TIMEOUT_MINUTES))) {
            throw new BusinessException("订单已超时，无法支付");
        }

        Payment existing = latestPayment(orderId);
        if (existing != null) {
            if (Objects.equals(existing.getStatus(), Payment.STATUS_SUCCESS)) {
                throw new BusinessException("订单已支付");
            }
            if (Objects.equals(existing.getStatus(), Payment.STATUS_PENDING)) {
                return buildPayParams(existing);
            }
        }

        Payment payment = new Payment();
        payment.setPaymentNo("PAY" + timestamp() + randomDigits(6));
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setChannel("MOCK");
        payment.setAmount(order.getPayAmount());
        payment.setStatus(Payment.STATUS_PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        log.info("创建支付单: paymentNo={} orderNo={} amount={}",
                payment.getPaymentNo(), payment.getOrderNo(), payment.getAmount());
        return buildPayParams(payment);
    }

    @Override
    public Map<String, Object> getPayment(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该订单");
        }
        Payment payment = latestPayment(orderId);
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("orderStatus", order.getStatus());
        if (payment != null) {
            data.put("paymentNo", payment.getPaymentNo());
            data.put("amount", payment.getAmount());
            data.put("channel", payment.getChannel());
            data.put("status", payment.getStatus());
            data.put("tradeNo", payment.getTradeNo());
        }
        return data;
    }

    @Override
    @Observed(name = "payment.notify")
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(Map<String, String> params) {
        if (!verifySign(params)) {
            log.warn("支付回调验签失败: {}", params);
            return "fail";
        }

        String paymentNo = params.get("paymentNo");
        String status = params.get("status");
        String amountStr = params.get("amount");

        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) {
            log.warn("支付回调支付单不存在: {}", paymentNo);
            return "fail";
        }

        // 金额核对：回调金额必须与本地支付单金额一致
        BigDecimal notifyAmount = parseAmount(amountStr);
        if (notifyAmount == null || notifyAmount.compareTo(payment.getAmount()) != 0) {
            log.error("【对账告警】回调金额不一致 paymentNo={} local={} notify={}",
                    paymentNo, payment.getAmount(), amountStr);
            return "fail";
        }
        // 金额核对：支付单金额必须与订单应付金额一致
        Order order = orderMapper.selectById(payment.getOrderId());
        if (order == null || order.getPayAmount() == null
                || order.getPayAmount().compareTo(payment.getAmount()) != 0) {
            log.error("【对账告警】订单应付金额与支付单金额不一致 paymentNo={}", paymentNo);
            return "fail";
        }

        if ("SUCCESS".equalsIgnoreCase(status)) {
            int rows = paymentMapper.markPaid(paymentNo, params.get("tradeNo"));
            if (rows == 0) {
                Payment current = paymentMapper.selectById(payment.getId());
                if (Objects.equals(current.getStatus(), Payment.STATUS_SUCCESS)) {
                    return "success"; // 幂等：重复回调
                }
                return "fail";
            }
            int orderRows = orderMapper.payOrderCas(payment.getOrderId());
            if (orderRows == 0) {
                log.warn("支付回调到达但订单状态非待支付，可能已取消/已支付 orderId={}", payment.getOrderId());
            }
            // 支付成功：锁定库存转已售（只减 locked_stock，可用库存在下单预占时已扣）
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, payment.getOrderId()));
            for (OrderItem item : items) {
                inventoryService.commitStock(item.getBookId(), item.getQuantity());
            }
            // 本地消息表：支付成功事件（与订单状态变更同一事务）
            outboxService.publish("PAYMENT_SUCCESS", "ORDER", payment.getOrderId(),
                    Map.of("orderId", payment.getOrderId(), "paymentNo", payment.getPaymentNo()));
            log.info("支付成功回调处理完成 paymentNo={} tradeNo={}", paymentNo, params.get("tradeNo"));
            businessMetrics.paymentSuccess();
            return "success";
        }

        paymentMapper.markFailed(paymentNo);
        log.info("支付失败回调处理完成 paymentNo={}", paymentNo);
        businessMetrics.paymentFailed();
        return "success";
    }

    @Override
    public Map<String, Object> applyRefund(Long userId, Long orderId, String reason) {
        Order order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException("当前订单状态不可退款");
        }

        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getStatus, Payment.STATUS_SUCCESS)
                        .orderByDesc(Payment::getId)
                        .last("LIMIT 1"));
        if (payment == null) {
            throw new BusinessException("支付记录不存在，无法退款");
        }

        Refund existing = latestRefund(orderId);
        if (existing != null && !Objects.equals(existing.getStatus(), Refund.STATUS_FAILED)) {
            return buildRefundResult(existing);
        }

        Refund refund = new Refund();
        refund.setRefundNo("RF" + timestamp() + randomDigits(6));
        refund.setPaymentNo(payment.getPaymentNo());
        refund.setOrderId(orderId);
        refund.setUserId(userId);
        refund.setAmount(payment.getAmount());
        refund.setReason(reason);
        refund.setStatus(Refund.STATUS_PROCESSING);
        refund.setCreatedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());
        refundMapper.insert(refund);

        log.info("发起退款: refundNo={} paymentNo={} amount={}",
                refund.getRefundNo(), refund.getPaymentNo(), refund.getAmount());
        mockPayGateway.refundSuccess(refund);

        return buildRefundResult(refundMapper.selectById(refund.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleRefundNotify(Map<String, String> params) {
        if (!verifySign(params)) {
            log.warn("退款回调验签失败: {}", params);
            return "fail";
        }

        String refundNo = params.get("refundNo");
        Refund refund = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>().eq(Refund::getRefundNo, refundNo));
        if (refund == null) {
            log.warn("退款回调退款单不存在: {}", refundNo);
            return "fail";
        }

        BigDecimal notifyAmount = parseAmount(params.get("amount"));
        if (notifyAmount == null || notifyAmount.compareTo(refund.getAmount()) != 0) {
            log.error("【对账告警】退款金额不一致 refundNo={} local={} notify={}",
                    refundNo, refund.getAmount(), params.get("amount"));
            return "fail";
        }

        if ("SUCCESS".equalsIgnoreCase(params.get("status"))) {
            int rows = refundMapper.markSuccess(refundNo, params.get("refundTradeNo"));
            if (rows == 0) {
                Refund current = refundMapper.selectById(refund.getId());
                if (Objects.equals(current.getStatus(), Refund.STATUS_SUCCESS)) {
                    return "success"; // 幂等
                }
                return "fail";
            }
            paymentMapper.markRefunded(refund.getPaymentNo());
            markOrderRefunded(refund.getOrderId());
            log.info("退款成功回调处理完成 refundNo={}", refundNo);
            businessMetrics.refundSuccess();
            return "success";
        }

        refundMapper.markFailed(refundNo);
        return "success";
    }

    @Override
    public void sandboxPay(Long userId, String paymentNo) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) {
            throw new BusinessException("支付单不存在");
        }
        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该支付单");
        }
        if (!Objects.equals(payment.getStatus(), Payment.STATUS_PENDING)) {
            throw new BusinessException("支付单状态不可支付");
        }
        mockPayGateway.paySuccess(payment);
    }

    @Override
    public void sandboxRefund(Long userId, String refundNo) {
        Refund refund = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>().eq(Refund::getRefundNo, refundNo));
        if (refund == null) {
            throw new BusinessException("退款单不存在");
        }
        if (!refund.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该退款单");
        }
        if (!Objects.equals(refund.getStatus(), Refund.STATUS_PROCESSING)) {
            throw new BusinessException("退款单状态不可操作");
        }
        mockPayGateway.refundSuccess(refund);
    }

    @Override
    public Map<String, Object> reconcile(int days) {
        LocalDateTime from = LocalDate.now().minusDays(Math.max(days, 1)).atStartOfDay();

        List<Payment> localPaid = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getStatus, Payment.STATUS_SUCCESS)
                        .ge(Payment::getCreatedAt, from));
        List<GatewayBill> bills = gatewayBillMapper.selectList(
                new LambdaQueryWrapper<GatewayBill>()
                        .eq(GatewayBill::getStatus, "SUCCESS")
                        .ge(GatewayBill::getBillTime, from));

        Map<String, Payment> localMap = localPaid.stream()
                .collect(Collectors.toMap(Payment::getPaymentNo, p -> p, (a, b) -> a));
        Map<String, GatewayBill> billMap = bills.stream()
                .collect(Collectors.toMap(GatewayBill::getPaymentNo, b -> b, (a, b) -> b));

        List<String> localOnly = new ArrayList<>();
        List<String> billOnly = new ArrayList<>();
        List<String> amountMismatch = new ArrayList<>();

        for (Payment p : localPaid) {
            GatewayBill bill = billMap.get(p.getPaymentNo());
            if (bill == null) {
                localOnly.add(p.getPaymentNo());
            } else if (bill.getAmount().compareTo(p.getAmount()) != 0) {
                amountMismatch.add(p.getPaymentNo());
            }
        }
        for (GatewayBill bill : bills) {
            if (!localMap.containsKey(bill.getPaymentNo())) {
                billOnly.add(bill.getPaymentNo());
            }
        }

        // 掉单补偿：网关已成功但本地未更新，按网关账单补记为已支付
        int repaired = 0;
        for (String paymentNo : billOnly) {
            GatewayBill bill = billMap.get(paymentNo);
            Payment p = paymentMapper.selectOne(
                    new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, paymentNo));
            if (p != null && !Objects.equals(p.getStatus(), Payment.STATUS_SUCCESS)) {
                if (paymentMapper.markPaid(paymentNo, bill.getTradeNo()) > 0) {
                    orderMapper.payOrderCas(p.getOrderId());
                    repaired++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("from", from);
        result.put("localPaidCount", localPaid.size());
        result.put("gatewayBillCount", bills.size());
        result.put("localOnly", localOnly);
        result.put("gatewayOnly", billOnly);
        result.put("amountMismatch", amountMismatch);
        result.put("repaired", repaired);
        log.info("对账完成: local={} gateway={} localOnly={} gatewayOnly={} amountMismatch={} repaired={}",
                localPaid.size(), bills.size(), localOnly.size(), billOnly.size(), amountMismatch.size(), repaired);
        if (!localOnly.isEmpty() || !amountMismatch.isEmpty()) {
            log.error("【对账告警】存在本地成功但网关缺失/金额不一致的支付单: localOnly={} amountMismatch={}",
                    localOnly, amountMismatch);
        }
        return result;
    }

    // ---------------- helpers ----------------

    private void markOrderRefunded(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || Objects.equals(order.getStatus(), 5)) {
            return;
        }
        order.setStatus(5);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            inventoryService.increaseStock(item.getBookId(), item.getQuantity());
        }
        log.info("订单已退款并回补库存 orderId={}", orderId);
    }

    private Map<String, Object> buildPayParams(Payment payment) {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentNo", payment.getPaymentNo());
        data.put("orderId", payment.getOrderId());
        data.put("orderNo", payment.getOrderNo());
        data.put("amount", payment.getAmount());
        data.put("channel", payment.getChannel());
        data.put("status", payment.getStatus());
        data.put("sandboxUrl", "/api/payment/sandbox/" + payment.getPaymentNo() + "/pay");
        return data;
    }

    private Map<String, Object> buildRefundResult(Refund refund) {
        Map<String, Object> data = new HashMap<>();
        data.put("refundNo", refund.getRefundNo());
        data.put("orderId", refund.getOrderId());
        data.put("amount", refund.getAmount());
        data.put("status", refund.getStatus());
        data.put("refundTradeNo", refund.getRefundTradeNo());
        return data;
    }

    private boolean verifySign(Map<String, String> params) {
        String sign = params.get("sign");
        if (sign == null || sign.isEmpty()) {
            return false;
        }
        return PaySignUtil.verify(PaySignUtil.canonical(params), sign, mockPayGateway.getPublicKey());
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private Payment latestPayment(Long orderId) {
        return paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
    }

    private Refund latestRefund(Long orderId) {
        return refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderId, orderId)
                .orderByDesc(Refund::getId)
                .last("LIMIT 1"));
    }

    private BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String randomDigits(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
