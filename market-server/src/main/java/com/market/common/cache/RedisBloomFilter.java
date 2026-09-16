package com.market.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 位图的布隆过滤器：用于拦截不存在的 bookId，防缓存穿透。
 * 注意：存在假阳性（可能误判存在），不存在假阴性；删除不支持，只能重建。
 */
@Component
@RequiredArgsConstructor
public class RedisBloomFilter {

    private static final String KEY = "bloom:book:ids";
    private static final long BITS = 1L << 22;
    private static final int HASHES = 4;

    private final StringRedisTemplate stringRedisTemplate;

    public void add(Long id) {
        if (id == null) {
            return;
        }
        String value = String.valueOf(id);
        for (int i = 0; i < HASHES; i++) {
            stringRedisTemplate.opsForValue().setBit(KEY, index(value, i), true);
        }
    }

    public boolean mightContain(Long id) {
        if (id == null) {
            return false;
        }
        String value = String.valueOf(id);
        for (int i = 0; i < HASHES; i++) {
            if (!Boolean.TRUE.equals(stringRedisTemplate.opsForValue().getBit(KEY, index(value, i)))) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        stringRedisTemplate.delete(KEY);
    }

    private long index(String value, int seed) {
        int h1 = value.hashCode();
        int h2 = Integer.rotateLeft(h1, 16) ^ 0x9e3779b9;
        long combined = Integer.toUnsignedLong(h1 + seed * h2);
        return combined % BITS;
    }
}
