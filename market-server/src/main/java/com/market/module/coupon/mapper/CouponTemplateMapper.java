package com.market.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    int deductRemainingCount(@Param("templateId") Long templateId);
}
