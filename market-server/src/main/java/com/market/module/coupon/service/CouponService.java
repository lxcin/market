package com.market.module.coupon.service;

import com.market.module.coupon.entity.CouponTemplate;
import com.market.module.coupon.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CouponService {

    List<CouponTemplate> listAvailableTemplates();

    List<UserCoupon> listUserCoupons(Long userId);

    CouponTemplate getTemplateById(Long id);

    Map<String, Object> previewDiscount(Long userId, Long userCouponId, BigDecimal orderAmount);

    List<Map<String, Object>> findBestCoupons(Long userId, BigDecimal orderAmount);

    boolean useCoupon(Long userCouponId, Long orderId);

    boolean refundCoupon(Long userCouponId);
}
