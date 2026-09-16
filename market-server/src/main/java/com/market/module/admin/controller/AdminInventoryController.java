package com.market.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.response.Result;
import com.market.module.inventory.entity.Inventory;
import com.market.module.inventory.mapper.InventoryMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final InventoryMapper inventoryMapper;

    @GetMapping("/{bookId}")
    public Result<Inventory> getByBookId(@PathVariable Long bookId) {
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getBookId, bookId));
        return Result.success(inventory);
    }

    @PutMapping("/{bookId}")
    public Result<Void> updateStock(@PathVariable Long bookId, @Valid @RequestBody StockRequest request) {
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getBookId, bookId));
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setBookId(bookId);
            inventory.setStock(request.getStock());
            inventoryMapper.insert(inventory);
        } else {
            inventory.setStock(request.getStock());
            inventoryMapper.updateById(inventory);
        }
        return Result.success();
    }

    @Data
    public static class StockRequest {

        @NotNull(message = "库存不能为空")
        private Integer stock;
    }
}
