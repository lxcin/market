package com.market.common.aspect;

import com.market.common.annotation.RepeatSubmit;
import com.market.common.exception.BusinessException;
import com.market.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis SETNX 的接口级防重提交。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private static final String PREFIX = "repeat:";

    private final StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        Long userId = SecurityUtil.getCurrentUserId();
        String key = PREFIX + (userId == null ? "anon" : userId)
                + ":" + joinPoint.getSignature().toShortString();

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(repeatSubmit.interval()));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(repeatSubmit.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            // 业务失败允许立即重试
            stringRedisTemplate.delete(key);
            throw t;
        }
    }
}
