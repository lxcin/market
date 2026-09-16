package com.market.common.util;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付网关签名/验签工具（示例使用 SHA256withRSA）。
 * 真实场景：平台仅持有网关公钥用于验签，网关私钥签名。
 */
public final class PaySignUtil {

    private PaySignUtil() {
    }

    /** 待签名字符串：过滤 sign/空值，按 key 升序，k=v 以 & 拼接 */
    public static String canonical(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !"sign".equals(e.getKey()))
                .filter(e -> !e.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    public static String sign(String content, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("签名失败", e);
        }
    }

    public static boolean verify(String content, String sign, PublicKey publicKey) {
        if (sign == null || publicKey == null) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }
}
