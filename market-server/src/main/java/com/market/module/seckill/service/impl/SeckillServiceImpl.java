package com.market.module.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.exception.BusinessException;
import com.market.common.metrics.BusinessMetrics;
import com.market.module.coupon.entity.CouponStatus;
import com.market.module.coupon.entity.CouponTemplate;
import com.market.module.coupon.entity.UserCoupon;
import com.market.module.coupon.mapper.CouponTemplateMapper;
import com.market.module.coupon.mapper.UserCouponMapper;
import com.market.module.seckill.service.SeckillConstants;
import com.market.module.seckill.service.SeckillService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final BusinessMetrics businessMetrics;

    private final DefaultRedisScript<Long> claimScript = buildScript();

    private static DefaultRedisScript<Long> buildScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/seckill_claim.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Override
    @Observed(name = "seckill.claim")
    public Map<String, Object> claim(Long userId, Long templateId) {
        CouponTemplate template = requireActiveTemplate(templateId);

        // 单用户限流，防刷
        String limitKey = SeckillConstants.LIMIT_PREFIX + userId;
        Long attempts = stringRedisTemplate.opsForValue().increment(limitKey);
        if (attempts != null && attempts == 1L) {
            stringRedisTemplate.expire(limitKey, Duration.ofSeconds(SeckillConstants.RATE_LIMIT_SECONDS));
        }
        if (attempts != null && attempts > SeckillConstants.RATE_LIMIT_MAX) {
            businessMetrics.seckillRejected();
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        // 未预热则懒加载
        String stockKey = SeckillConstants.stockKey(templateId);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            warmup(templateId, false);
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        String traceId = org.slf4j.MDC.get("traceId");
        String spanId = org.slf4j.MDC.get("spanId");
        List<String> keys = List.of(
                stockKey,
                SeckillConstants.userKey(templateId, userId),
                SeckillConstants.STREAM
        );

        Long result = stringRedisTemplate.execute(claimScript, keys,
                String.valueOf(template.getPerUserLimit()),
                String.valueOf(SeckillConstants.USER_TTL_SECONDS),
                String.valueOf(userId),
                String.valueOf(templateId),
                requestId,
                traceId == null ? "" : traceId,
                spanId == null ? "" : spanId);

        int code = result == null ? -3 : result.intValue();
        if (code == -1) {
            businessMetrics.seckillSoldOut();
            throw new BusinessException("优惠券已抢完");
        }
        if (code == -2) {
            businessMetrics.seckillRejected();
            throw new BusinessException("您已领取过该优惠券，每人限领" + template.getPerUserLimit() + "张");
        }
        if (code != 1) {
            throw new BusinessException("活动未就绪，请稍后重试");
        }

        businessMetrics.seckillAccepted();
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("status", "ACCEPTED");
        data.put("message", "领取请求已受理，优惠券发放中");
        return data;
    }

    @Override
    public Map<String, Object> warmup(Long templateId, boolean force) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("优惠券模板不存在");
        }
        String stockKey = SeckillConstants.stockKey(templateId);
        boolean exists = Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey));
        if (force || !exists) {
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(template.getRemainingCount()));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("templateId", templateId);
        data.put("remainingCount", template.getRemainingCount());
        data.put("warmed", force || !exists);
        return data;
    }

    @Override
    public int warmupAll() {
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStatus, 1)
                        .le(CouponTemplate::getStartTime, now)
                        .ge(CouponTemplate::getEndTime, now));
        int count = 0;
        for (CouponTemplate template : templates) {
            String stockKey = SeckillConstants.stockKey(template.getId());
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
                stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(template.getRemainingCount()));
                count++;
            }
        }
        return count;
    }

    @Override
    public Map<String, Object> stockInfo(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("优惠券模板不存在");
        }
        String stock = stringRedisTemplate.opsForValue().get(SeckillConstants.stockKey(templateId));
        Long issued = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponTemplateId, templateId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateId", templateId);
        data.put("dbRemaining", template.getRemainingCount());
        data.put("redisStock", stock == null ? null : Integer.valueOf(stock));
        data.put("issued", issued);
        data.put("total", template.getTotalCount());
        return data;
    }

    @Override
    public void persistClaim(Long userId, Long templateId) {
        Long exists = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponTemplateId, templateId));
        if (exists != null && exists > 0) {
            log.info("秒杀落库幂等命中: userId={} templateId={}", userId, templateId);
            return;
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponTemplateId(templateId);
        userCoupon.setStatus(CouponStatus.UNUSED.getCode());
        userCoupon.setCreatedAt(LocalDateTime.now());
        try {
            userCouponMapper.insert(userCoupon);
        } catch (DuplicateKeyException e) {
            log.info("秒杀落库唯一键冲突(已发放): userId={} templateId={}", userId, templateId);
            return;
        }

        int rows = couponTemplateMapper.deductRemainingCount(templateId);
        if (rows <= 0) {
            log.error("【秒杀告警】DB 库存扣减失败(可能已耗尽) templateId={} userId={}", templateId, userId);
        } else {
            log.info("秒杀落库成功: userId={} templateId={}", userId, templateId);
        }
    }

    @Override
    public Map<String, Object> reconcile(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("优惠券模板不存在");
        }
        Long issued = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponTemplateId, templateId));
        int expectedIssued = template.getTotalCount() - template.getRemainingCount();
        boolean consistent = issued != null && issued == expectedIssued;

        String stock = stringRedisTemplate.opsForValue().get(SeckillConstants.stockKey(templateId));

        PendingMessagesSummary pending = null;
        try {
            pending = stringRedisTemplate.opsForStream()
                    .pending(SeckillConstants.STREAM, SeckillConstants.GROUP);
        } catch (Exception ignore) {
        }
        long pendingCount = pending == null ? 0 : pending.getTotalPendingMessages();

        if (!consistent) {
            log.error("【秒杀对账告警】templateId={} total={} remaining={} issued={} expectedIssued={}",
                    templateId, template.getTotalCount(), template.getRemainingCount(), issued, expectedIssued);
        }
        if (pendingCount == 0 && stock != null
                && Integer.parseInt(stock) < template.getRemainingCount()) {
            // 无待落库消息但 Redis 库存少于 DB，按 DB 修正
            stringRedisTemplate.opsForValue().set(
                    SeckillConstants.stockKey(templateId), String.valueOf(template.getRemainingCount()));
            log.warn("秒杀库存已按 DB 修正: templateId={} redis={} db={}",
                    templateId, stock, template.getRemainingCount());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateId", templateId);
        data.put("consistent", consistent);
        data.put("issued", issued);
        data.put("expectedIssued", expectedIssued);
        data.put("dbRemaining", template.getRemainingCount());
        data.put("redisStock", stock == null ? null : Integer.valueOf(stock));
        data.put("pendingMessages", pendingCount);
        return data;
    }

    private CouponTemplate requireActiveTemplate(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || template.getStatus() != 1) {
            throw new BusinessException("优惠券模板不存在或已禁用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getStartTime())) {
            throw new BusinessException("优惠券活动尚未开始");
        }
        if (now.isAfter(template.getEndTime())) {
            throw new BusinessException("优惠券活动已结束");
        }
        return template;
    }
}
