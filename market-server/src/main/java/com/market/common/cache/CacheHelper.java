package com.market.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 二级缓存读取器：L1 进程内 + L2 Redis。
 * 同时解决缓存三件套：
 *  - 穿透：空值哨兵缓存（配合布隆过滤器）
 *  - 击穿：分布式互斥锁重建热点 key
 *  - 雪崩：TTL 随机抖动 + L1/L2 两级
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheHelper {

    public static final String NULL_VALUE = "__NULL__";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final MeterRegistry meterRegistry;
    private final LocalCache localCache = new LocalCache(2000);

    private Counter hitLocal;
    private Counter hitRedis;
    private Counter miss;

    @PostConstruct
    public void initMetrics() {
        hitLocal = Counter.builder("cache_access_total").tag("result", "hit_local").register(meterRegistry);
        hitRedis = Counter.builder("cache_access_total").tag("result", "hit_redis").register(meterRegistry);
        miss = Counter.builder("cache_access_total").tag("result", "miss").register(meterRegistry);
    }

    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        Object cached = read(key);
        if (cached != null) {
            return unwrap(cached);
        }

        RLock lock = redissonClient.getLock("cache:rebuild:" + key);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 10, TimeUnit.SECONDS);
            if (locked) {
                Object again = read(key);
                if (again != null) {
                    return unwrap(again);
                }
                T loaded = loader.get();
                write(key, loaded, ttl);
                return loaded;
            }
            // 未抢到锁：短暂自旋后重读，仍无则兜底查库，避免长时间阻塞
            Thread.sleep(50);
            Object again = read(key);
            if (again != null) {
                return unwrap(again);
            }
            return loader.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void evict(String key) {
        redisTemplate.delete(key);
        localCache.evict(key);
    }

    public void evictByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        // L1 简单整体清空，保证一致性
        localCache.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> T unwrap(Object value) {
        if (NULL_VALUE.equals(value)) {
            return null;
        }
        return (T) value;
    }

    private Object read(String key) {
        Object value = localCache.get(key);
        if (value != null) {
            hitLocal.increment();
            return value;
        }
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            hitRedis.increment();
            localCache.put(key, value, Math.min(30_000, jitter(30_000)));
        } else {
            miss.increment();
        }
        return value;
    }

    private void write(String key, Object value, Duration ttl) {
        if (value == null) {
            long nullTtl = Math.min(60_000, ttl.toMillis());
            redisTemplate.opsForValue().set(key, NULL_VALUE, Duration.ofMillis(nullTtl));
            localCache.put(key, NULL_VALUE, nullTtl);
        } else {
            long ttlMs = jitter(ttl.toMillis());
            redisTemplate.opsForValue().set(key, value, Duration.ofMillis(ttlMs));
            localCache.put(key, value, Math.min(ttlMs, 30_000));
        }
    }

    private long jitter(long ttlMs) {
        double factor = 0.8 + ThreadLocalRandom.current().nextDouble() * 0.4;
        return (long) (ttlMs * factor);
    }
}
