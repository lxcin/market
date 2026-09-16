package com.market.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.exception.BusinessException;
import com.market.module.coupon.entity.CouponStatus;
import com.market.module.coupon.entity.CouponTemplate;
import com.market.module.coupon.entity.UserCoupon;
import com.market.module.coupon.mapper.CouponTemplateMapper;
import com.market.module.coupon.mapper.UserCouponMapper;
import com.market.module.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public List<CouponTemplate> listAvailableTemplates() {
        LocalDateTime now = LocalDateTime.now();
        return couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStatus, 1)
                        .le(CouponTemplate::getStartTime, now)
                        .ge(CouponTemplate::getEndTime, now)
                        .gt(CouponTemplate::getRemainingCount, 0)
        );
    }

    @Override
    public List<UserCoupon> listUserCoupons(Long userId) {
        return userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .orderByDesc(UserCoupon::getCreatedAt)
        );
    }

    @Override
    public CouponTemplate getTemplateById(Long id) {
        return couponTemplateMapper.selectById(id);
    }

    @Override
    public Map<String, Object> previewDiscount(Long userId, Long userCouponId, BigDecimal orderAmount) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }

        CouponStatus status = CouponStatus.of(userCoupon.getStatus());
        if (status != CouponStatus.UNUSED) {
            throw new BusinessException("优惠券状态不可用");
        }

        CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getCouponTemplateId());
        if (template == null) {
            throw new BusinessException("优惠券模板不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(template.getEndTime())) {
            throw new BusinessException("优惠券已过期");
        }

        if (orderAmount.compareTo(template.getThresholdAmount()) < 0) {
            throw new BusinessException("订单金额未达到优惠券使用门槛");
        }

        BigDecimal discountAmount;
        Integer type = template.getType();

        if (type == 1) {
            discountAmount = template.getDiscountAmount();
        } else if (type == 2) {
            discountAmount = orderAmount.multiply(
                    BigDecimal.ONE.subtract(template.getDiscountRate())
            ).setScale(2, RoundingMode.HALF_UP);
        } else if (type == 3) {
            discountAmount = template.getDiscountAmount().min(orderAmount);
        } else {
            throw new BusinessException("未知的优惠券类型");
        }

        BigDecimal finalAmount = orderAmount.subtract(discountAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("templateName", template.getName());
        result.put("type", template.getType());
        result.put("thresholdAmount", template.getThresholdAmount());
        result.put("discountAmount", discountAmount);
        result.put("finalAmount", finalAmount);
        return result;
    }

    @Override
    public List<Map<String, Object>> findBestCoupons(Long userId, BigDecimal orderAmount) {
        return userCouponMapper.findBestCoupons(userId, orderAmount);
    }

    @Override
    public boolean useCoupon(Long userCouponId, Long orderId) {
        int rows = userCouponMapper.useCouponCas(userCouponId, orderId);
        return rows > 0;
    }

    @Override
    public boolean refundCoupon(Long userCouponId) {
        int rows = userCouponMapper.releaseCoupon(userCouponId);
        return rows > 0;
    }
}
