package com.market.module.seckill.service;

import jakarta.annotation.PostConstruct;
import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 秒杀领取的异步落库消费者：基于 Redis Stream 消费组。
 * 保证"Redis 预扣成功 → 最终一定落库"，失败消息保留在 PEL 中重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillClaimWorker {

    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillService seckillService;
    private final Tracer braveTracer;

    @PostConstruct
    public void initGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(
                    SeckillConstants.STREAM, ReadOffset.from("0"), SeckillConstants.GROUP);
            log.info("秒杀消费组已创建: {}", SeckillConstants.GROUP);
        } catch (Exception e) {
            log.info("秒杀消费组已存在或创建跳过: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void consumeNew() {
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(SeckillConstants.GROUP, SeckillConstants.CONSUMER),
                    StreamReadOptions.empty().count(50),
                    StreamOffset.create(SeckillConstants.STREAM, ReadOffset.lastConsumed()));
            if (records == null || records.isEmpty()) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                process(record);
            }
        } catch (Exception e) {
            log.error("秒杀消息消费异常", e);
        }
    }

    /** 重试长时间未确认的待处理消息 */
    @Scheduled(fixedDelay = 30000)
    public void retryPending() {
        try {
            PendingMessages pending = stringRedisTemplate.opsForStream().pending(
                    SeckillConstants.STREAM, SeckillConstants.GROUP, Range.unbounded(), 100L);
            if (pending == null || pending.isEmpty()) {
                return;
            }
            List<RecordId> ids = new ArrayList<>();
            for (PendingMessage pm : pending) {
                ids.add(pm.getId());
            }
            if (ids.isEmpty()) {
                return;
            }
            List<MapRecord<String, Object, Object>> claimed = stringRedisTemplate.opsForStream().claim(
                    SeckillConstants.STREAM, SeckillConstants.GROUP, SeckillConstants.CONSUMER,
                    Duration.ofSeconds(60), ids.toArray(new RecordId[0]));
            if (claimed != null) {
                for (MapRecord<String, Object, Object> record : claimed) {
                    process(record);
                }
            }
        } catch (Exception e) {
            log.error("秒杀消息重试异常", e);
        }
    }

    private void process(MapRecord<String, Object, Object> record) {
        String prevTrace = MDC.get("traceId");
        Span span = null;
        Tracer.SpanInScope scope = null;
        try {
            Object userId = record.getValue().get("userId");
            Object templateId = record.getValue().get("templateId");
            Object traceId = record.getValue().get("traceId");
            Object spanId = record.getValue().get("spanId");
            if (userId == null || templateId == null) {
                acknowledge(record.getId());
                return;
            }

            String traceIdStr = traceId == null ? "" : traceId.toString();
            String spanIdStr = spanId == null ? "" : spanId.toString();

            // 用原领取请求的 traceId/spanId 创建子 span，使其在 Zipkin 中归入同一条 trace
            if (traceIdStr.length() == 32 && spanIdStr.length() == 16) {
                TraceContext parent = TraceContext.newBuilder()
                        .traceIdHigh(Long.parseUnsignedLong(traceIdStr.substring(0, 16), 16))
                        .traceId(Long.parseUnsignedLong(traceIdStr.substring(16, 32), 16))
                        .spanId(Long.parseUnsignedLong(spanIdStr, 16))
                        .sampled(Boolean.TRUE)
                        .build();
                span = braveTracer.nextSpan(TraceContextOrSamplingFlags.create(parent))
                        .name("seckill.persist").start();
                MDC.put("traceId", traceIdStr);
                scope = braveTracer.withSpanInScope(span);
            }

            seckillService.persistClaim(Long.valueOf(userId.toString()), Long.valueOf(templateId.toString()));
            acknowledge(record.getId());
        } catch (Exception e) {
            if (span != null) {
                span.error(e);
            }
            // 不确认，留在 PEL 中由 retryPending 重试
            log.error("秒杀落库失败，稍后重试: recordId={}", record.getId(), e);
        } finally {
            if (scope != null) {
                scope.close();
            }
            if (span != null) {
                span.finish();
            }
            if (prevTrace != null) {
                MDC.put("traceId", prevTrace);
            } else {
                MDC.remove("traceId");
            }
        }
    }

    private void acknowledge(RecordId recordId) {
        try {
            stringRedisTemplate.opsForStream().acknowledge(
                    SeckillConstants.STREAM, SeckillConstants.GROUP, recordId);
        } catch (Exception e) {
            log.warn("秒杀消息确认失败: {}", recordId, e);
        }
    }
}
