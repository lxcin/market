package com.market.module.payment.service;

import com.market.common.util.PaySignUtil;
import com.market.module.payment.entity.GatewayBill;
import com.market.module.payment.entity.Payment;
import com.market.module.payment.entity.Refund;
import com.market.module.payment.mapper.GatewayBillMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模拟第三方支付网关：负责"签名"与"异步发起回调"。
 * 回调通过独立线程池执行，因此调用方（下单/退款接口）会立即返回，
 * 与真实网关"先受理、后异步通知"的行为一致。
 */
@Slf4j
@Service
public class MockPayGatewayService {

    private final GatewayBillMapper gatewayBillMapper;
    private final ThreadPoolTaskExecutor callbackExecutor;
    private final RestTemplate restTemplate;
    private final SecureRandom random = new SecureRandom();

    @Value("${pay.notify-url:http://localhost:8080/api/payment/notify}")
    private String notifyUrl;

    @Value("${pay.refund-notify-url:http://localhost:8080/api/payment/refund/notify}")
    private String refundNotifyUrl;

    /** 模拟网关处理耗时（毫秒），让异步通知更接近真实 */
    @Value("${pay.callback-delay-ms:800}")
    private long callbackDelayMs;

    private KeyPair keyPair;

    public MockPayGatewayService(GatewayBillMapper gatewayBillMapper,
                                 @Qualifier("payCallbackExecutor") ThreadPoolTaskExecutor callbackExecutor,
                                 RestTemplateBuilder restTemplateBuilder) {
        this.gatewayBillMapper = gatewayBillMapper;
        this.callbackExecutor = callbackExecutor;
        // 使用 Boot 构建的 RestTemplate：自动带 Micrometer Observation，
        // 出站请求会创建 client span 并注入 traceparent，使异步回调链路可串起来
        this.restTemplate = restTemplateBuilder.build();
    }

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
            log.info("MockPayGateway 初始化完成，已生成 RSA 密钥对用于回调签名");
        } catch (Exception e) {
            throw new IllegalStateException("初始化模拟支付网关失败", e);
        }
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    /** 模拟用户完成支付：写网关账单，并异步回调平台 */
    public void paySuccess(Payment payment) {
        String tradeNo = "MOCK" + timestamp() + randomDigits(6);
        GatewayBill bill = new GatewayBill();
        bill.setPaymentNo(payment.getPaymentNo());
        bill.setTradeNo(tradeNo);
        bill.setAmount(payment.getAmount());
        bill.setStatus("SUCCESS");
        bill.setBillTime(LocalDateTime.now());
        gatewayBillMapper.insert(bill);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("paymentNo", payment.getPaymentNo());
        params.put("tradeNo", tradeNo);
        params.put("amount", payment.getAmount().toPlainString());
        params.put("status", "SUCCESS");
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        params.put("sign", PaySignUtil.sign(PaySignUtil.canonical(params), keyPair.getPrivate()));

        submitCallback(notifyUrl, params);
    }

    /** 模拟网关退款成功并异步回调平台 */
    public void refundSuccess(Refund refund) {
        String refundTradeNo = "MOCKRF" + timestamp() + randomDigits(6);

        GatewayBill bill = new GatewayBill();
        bill.setPaymentNo(refund.getPaymentNo());
        bill.setTradeNo(refundTradeNo);
        bill.setAmount(refund.getAmount());
        bill.setStatus("REFUND");
        bill.setBillTime(LocalDateTime.now());
        gatewayBillMapper.insert(bill);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("refundNo", refund.getRefundNo());
        params.put("paymentNo", refund.getPaymentNo());
        params.put("refundTradeNo", refundTradeNo);
        params.put("amount", refund.getAmount().toPlainString());
        params.put("status", "SUCCESS");
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        params.put("sign", PaySignUtil.sign(PaySignUtil.canonical(params), keyPair.getPrivate()));

        submitCallback(refundNotifyUrl, params);
    }

    /** 异步提交回调：调用方立即返回，通知在独立线程中延迟送达 */
    private void submitCallback(String url, Map<String, String> params) {
        callbackExecutor.execute(() -> {
            try {
                if (callbackDelayMs > 0) {
                    Thread.sleep(callbackDelayMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            postForm(url, params);
        });
    }

    private void postForm(String url, Map<String, String> params) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            params.forEach(form::add);
            String response = restTemplate.postForObject(url, new HttpEntity<>(form, headers), String.class);
            log.info("模拟网关异步回调 {} -> {}", url, response);
        } catch (Exception e) {
            log.error("模拟网关异步回调失败: {}", url, e);
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
