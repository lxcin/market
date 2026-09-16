package com.market.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 进程内一级缓存（L1）：带 TTL 与容量上限，用于抗热点、降低 Redis 压力。
 */
public class LocalCache {

    private record Entry(Object value, long expireAt) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final int maxSize;

    public LocalCache(int maxSize) {
        this.maxSize = maxSize;
    }

    public Object get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt() < System.currentTimeMillis()) {
            store.remove(key);
            return null;
        }
        return entry.value();
    }

    public void put(String key, Object value, long ttlMillis) {
        if (store.size() >= maxSize) {
            evictSome();
        }
        store.put(key, new Entry(value, System.currentTimeMillis() + ttlMillis));
    }

    public void evict(String key) {
        store.remove(key);
    }

    public void clear() {
        store.clear();
    }

    private void evictSome() {
        // 简单淘汰：随机清理 ~10%，避免无界增长
        int target = Math.max(1, maxSize / 10);
        int removed = 0;
        for (String key : store.keySet()) {
            if (removed >= target) {
                break;
            }
            if (ThreadLocalRandom.current().nextBoolean()) {
                store.remove(key);
                removed++;
            }
        }
    }
}
