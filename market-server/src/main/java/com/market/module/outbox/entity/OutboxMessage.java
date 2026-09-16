package com.market.module.outbox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_outbox")
public class OutboxMessage {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_DELIVERED = 1;
    public static final int STATUS_FAILED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventType;

    private String aggregateType;

    private Long aggregateId;

    private String payload;

    private Integer status = STATUS_PENDING;

    private Integer retryCount = 0;

    private String lastError;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
