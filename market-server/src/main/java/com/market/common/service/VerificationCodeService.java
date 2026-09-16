package com.market.common.service;

import com.market.common.exception.BusinessException;
import com.market.common.util.CaptchaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 图形验证码 & 短信验证码的生成、存储、校验（基于 Redis）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final String CAPTCHA_KEY = "auth:captcha:";
    private static final String SMS_KEY = "auth:sms:";
    private static final String SMS_LIMIT_KEY = "auth:sms:limit:";
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final Duration SMS_TTL = Duration.ofMinutes(5);
    private static final Duration SMS_LIMIT_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    @Value("${sms.mock:true}")
    private boolean smsMock;

    public record CaptchaResult(String captchaId, String image) {
    }

    /**
     * 生成图形验证码，返回 captchaId 与 base64 图片。
     */
    public CaptchaResult createCaptcha() {
        String code = CaptchaUtil.randomCode(4);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(CAPTCHA_KEY + captchaId, code, CAPTCHA_TTL);
        return new CaptchaResult(captchaId, CaptchaUtil.toBase64Png(code));
    }

    /**
     * 校验图形验证码（一次性，无论成功失败都会失效）。
     */
    public boolean verifyCaptcha(String captchaId, String input) {
        if (captchaId == null || input == null) {
            return false;
        }
        String key = CAPTCHA_KEY + captchaId;
        String expected = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return expected != null && expected.equalsIgnoreCase(input.trim());
    }

    /**
     * 发送短信验证码，返回验证码明文（仅用于 mock 演示，真实环境由短信网关下发）。
     */
    public String sendSmsCode(String phone) {
        String limitKey = SMS_LIMIT_KEY + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }
        String code = CaptchaUtil.randomNumeric(6);
        redisTemplate.opsForValue().set(SMS_KEY + phone, code, SMS_TTL);
        redisTemplate.opsForValue().set(limitKey, "1", SMS_LIMIT_TTL);
        log.info("【短信验证码】手机号 {} 验证码 {} 有效期 5 分钟", phone, code);
        return code;
    }

    public boolean verifySmsCode(String phone, String input) {
        if (phone == null || input == null) {
            return false;
        }
        String key = SMS_KEY + phone;
        String expected = redisTemplate.opsForValue().get(key);
        if (expected != null && expected.equals(input.trim())) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public boolean isSmsMock() {
        return smsMock;
    }
}
