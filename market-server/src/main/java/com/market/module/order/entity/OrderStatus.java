package com.market.module.order.entity;

/**
 * 订单状态机：显式定义状态与允许的流转。
 */
public enum OrderStatus {

    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDED(5, "已退款");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatus of(Integer code) {
        if (code != null) {
            for (OrderStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
        }
        return PENDING;
    }

    /** 允许的状态流转 */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED || target == REFUNDED;
            case SHIPPED -> target == COMPLETED || target == REFUNDED;
            case COMPLETED, CANCELLED, REFUNDED -> false;
        };
    }
}
