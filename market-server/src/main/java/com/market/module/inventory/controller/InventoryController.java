package com.market.module.inventory.controller;

import com.market.common.response.Result;
import com.market.module.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{bookId}")
    public Result<Integer> getStock(@PathVariable Long bookId) {
        return Result.success(inventoryService.getStock(bookId));
    }
}
