package com.market.module.order.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal payAmount;

    private Integer status;

    private String statusDesc;

    private List<OrderItemVO> items;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String couponName;

    private LocalDateTime createdAt;
}
