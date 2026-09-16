package com.market.module.payment.controller;

import com.market.common.annotation.RepeatSubmit;
import com.market.common.response.Result;
import com.market.common.util.SecurityUtil;
import com.market.module.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** 统一下单：为订单创建支付单，返回支付参数 */
    @RepeatSubmit(interval = 2)
    @PostMapping("/create/{orderId}")
    public Result<Map<String, Object>> create(@PathVariable Long orderId) {
        return Result.success(paymentService.createPayment(SecurityUtil.getCurrentUserId(), orderId));
    }

    /** 查询订单的支付状态 */
    @GetMapping("/{orderId}")
    public Result<Map<String, Object>> get(@PathVariable Long orderId) {
        return Result.success(paymentService.getPayment(SecurityUtil.getCurrentUserId(), orderId));
    }

    /** 支付网关异步回调（验签 + 金额核对 + 幂等） */
    @PostMapping("/notify")
    public String notify(@RequestParam Map<String, String> params) {
        return paymentService.handleNotify(params);
    }

    /** 用户申请退款 */
    @PostMapping("/refund/order/{orderId}")
    public Result<Map<String, Object>> refund(@PathVariable Long orderId,
                                              @RequestParam(required = false) String reason) {
        return Result.success(paymentService.applyRefund(SecurityUtil.getCurrentUserId(), orderId, reason));
    }

    /** 退款网关异步回调 */
    @PostMapping("/refund/notify")
    public String refundNotify(@RequestParam Map<String, String> params) {
        return paymentService.handleRefundNotify(params);
    }

    /** 模拟用户完成支付（沙箱：由网关发起回调） */
    @PostMapping("/sandbox/{paymentNo}/pay")
    public Result<Void> sandboxPay(@PathVariable String paymentNo) {
        paymentService.sandboxPay(SecurityUtil.getCurrentUserId(), paymentNo);
        return Result.success();
    }

    /** 模拟网关完成退款 */
    @PostMapping("/sandbox/refund/{refundNo}")
    public Result<Void> sandboxRefund(@PathVariable String refundNo) {
        paymentService.sandboxRefund(SecurityUtil.getCurrentUserId(), refundNo);
        return Result.success();
    }

    /** 手动触发对账（近 N 天） */
    @GetMapping("/reconcile")
    public Result<Map<String, Object>> reconcile(@RequestParam(defaultValue = "1") Integer days) {
        return Result.success(paymentService.reconcile(days));
    }
}
