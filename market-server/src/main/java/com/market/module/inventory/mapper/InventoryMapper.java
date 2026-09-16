package com.market.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.module.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {
    int deductStock(@Param("bookId") Long bookId, @Param("quantity") Integer quantity,
                    @Param("version") Integer version);

    int increaseStock(@Param("bookId") Long bookId, @Param("quantity") Integer quantity);

    int reserveStock(@Param("bookId") Long bookId, @Param("quantity") Integer quantity,
                     @Param("version") Integer version);

    int commitStock(@Param("bookId") Long bookId, @Param("quantity") Integer quantity);

    int releaseStock(@Param("bookId") Long bookId, @Param("quantity") Integer quantity);
}
