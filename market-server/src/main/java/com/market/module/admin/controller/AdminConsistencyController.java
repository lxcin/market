package com.market.module.admin.controller;

import com.market.common.response.Result;
import com.market.module.coupon.task.CouponReconcileTask;
import com.market.module.inventory.task.InventoryReconcileTask;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 一致性对账（手动触发/查看）。
 */
@RestController
@RequestMapping("/api/admin/consistency")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConsistencyController {

    private final InventoryReconcileTask inventoryReconcileTask;
    private final CouponReconcileTask couponReconcileTask;

    @GetMapping("/inventory")
    public Result<Map<String, Object>> inventory() {
        return Result.success(inventoryReconcileTask.reconcile());
    }

    @GetMapping("/coupon")
    public Result<Map<String, Object>> coupon() {
        return Result.success(couponReconcileTask.reconcile());
    }
}
