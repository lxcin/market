package com.market.module.seckill.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时把进行中的券模板库存预热到 Redis。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillWarmupRunner implements ApplicationRunner {

    private final SeckillService seckillService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int count = seckillService.warmupAll();
            log.info("秒杀库存预热完成，共预热 {} 个券模板", count);
        } catch (Exception e) {
            log.error("秒杀库存预热失败", e);
        }
    }
}
