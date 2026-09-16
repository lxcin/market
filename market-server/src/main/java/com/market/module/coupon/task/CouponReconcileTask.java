package com.market.module.coupon.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.metrics.BusinessMetrics;
import com.market.module.coupon.entity.CouponTemplate;
import com.market.module.coupon.entity.UserCoupon;
import com.market.module.coupon.mapper.CouponTemplateMapper;
import com.market.module.coupon.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 优惠券对账：不变量 —— 模板 remaining_count == total_count - 已发放数量。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReconcileTask {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final BusinessMetrics businessMetrics;

    @Scheduled(cron = "0 */5 * * * ?")
    public void scheduled() {
        reconcile();
    }

    public Map<String, Object> reconcile() {
        List<CouponTemplate> templates = couponTemplateMapper.selectList(null);
        List<Map<String, Object>> mismatches = new ArrayList<>();
        for (CouponTemplate t : templates) {
            Long issued = userCouponMapper.selectCount(
                    new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponTemplateId, t.getId()));
            int expectedRemaining = t.getTotalCount() - (issued == null ? 0 : issued.intValue());
            if (t.getRemainingCount() != expectedRemaining) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("templateId", t.getId());
                m.put("name", t.getName());
                m.put("remaining", t.getRemainingCount());
                m.put("expectedRemaining", expectedRemaining);
                m.put("issued", issued);
                mismatches.add(m);
                log.error("【优惠券对账告警】templateId={} remaining={} 期望={} issued={}",
                        t.getId(), t.getRemainingCount(), expectedRemaining, issued);
            }
        }
        for (int i = 0; i < mismatches.size(); i++) {
            businessMetrics.reconcileMismatch();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checked", templates.size());
        result.put("mismatchCount", mismatches.size());
        result.put("mismatches", mismatches);
        log.info("优惠券对账完成: 检查 {} 个模板, 不一致 {} 个", templates.size(), mismatches.size());
        return result;
    }
}
