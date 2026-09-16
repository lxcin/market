package com.market.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * JWT 黑名单：登出后按 jti 拉黑，直至 token 自然过期。
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "auth:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    public void blacklist(String jti, long ttlMillis) {
        if (jti == null || ttlMillis <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(PREFIX + jti, "1", Duration.ofMillis(ttlMillis));
    }

    public boolean isBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(PREFIX + jti));
    }
}
