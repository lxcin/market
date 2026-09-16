package com.market.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    int useCouponCas(@Param("id") Long id, @Param("orderId") Long orderId);

    int releaseCoupon(@Param("id") Long id);

    List<Map<String, Object>> findBestCoupons(@Param("userId") Long userId,
                                               @Param("orderAmount") BigDecimal orderAmount);
}
