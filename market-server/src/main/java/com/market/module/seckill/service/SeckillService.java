package com.market.module.seckill.service;

import java.util.Map;

public interface SeckillService {

    /** 秒杀领券：Redis 原子预扣 + 异步落库，立即返回受理结果 */
    Map<String, Object> claim(Long userId, Long templateId);

    /** 预热库存到 Redis（force=false 时仅当不存在才写入） */
    Map<String, Object> warmup(Long templateId, boolean force);

    /** 预热所有进行中的券模板，返回预热数量 */
    int warmupAll();

    /** 查询秒杀库存（Redis + DB） */
    Map<String, Object> stockInfo(Long templateId);

    /** 幂等落库：写入用户券并扣减 DB 剩余数量（供异步消费者调用） */
    void persistClaim(Long userId, Long templateId);

    /** 对账：DB 已发数量 / 剩余数量 / Redis 库存 的一致性检查与修正 */
    Map<String, Object> reconcile(Long templateId);
}
