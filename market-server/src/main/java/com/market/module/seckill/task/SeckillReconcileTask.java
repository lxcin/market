package com.market.module.seckill.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.module.coupon.entity.CouponTemplate;
import com.market.module.coupon.mapper.CouponTemplateMapper;
import com.market.module.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秒杀对账：定时校验 DB 已发/剩余/Redis 库存的一致性（真实场景还需比对领券流水）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillReconcileTask {

    private final CouponTemplateMapper couponTemplateMapper;
    private final SeckillService seckillService;

    @Scheduled(cron = "0 */10 * * * ?")
    public void reconcileAll() {
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>().eq(CouponTemplate::getStatus, 1));
        for (CouponTemplate template : templates) {
            try {
                seckillService.reconcile(template.getId());
            } catch (Exception e) {
                log.error("秒杀对账异常 templateId={}", template.getId(), e);
            }
        }
    }
}
