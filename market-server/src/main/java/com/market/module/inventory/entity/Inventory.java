package com.market.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookId;

    private Integer stock = 0;

    private Integer lockedStock = 0;

    @Version
    private Integer version = 0;

    private LocalDateTime updatedAt;
}
