package com.market.module.seckill.service;

public final class SeckillConstants {

    private SeckillConstants() {
    }

    public static final String STREAM = "seckill:stream";
    public static final String GROUP = "seckill-group";
    public static final String CONSUMER = "seckill-consumer-1";

    public static final String STOCK_PREFIX = "seckill:stock:";
    public static final String USER_PREFIX = "seckill:user:";
    public static final String LIMIT_PREFIX = "seckill:limit:";

    public static final long USER_TTL_SECONDS = 7L * 24 * 3600;
    public static final int RATE_LIMIT_MAX = 20;
    public static final long RATE_LIMIT_SECONDS = 10;

    public static String stockKey(Long templateId) {
        return STOCK_PREFIX + templateId;
    }

    public static String userKey(Long templateId, Long userId) {
        return USER_PREFIX + templateId + ":" + userId;
    }
}
