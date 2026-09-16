package com.market.module.inventory.task;

import com.market.common.metrics.BusinessMetrics;
import com.market.module.inventory.entity.Inventory;
import com.market.module.inventory.mapper.InventoryMapper;
import com.market.module.order.mapper.OrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存对账：不变量 —— 某图书的 locked_stock == 该图书所有"待支付订单"的预占数量之和。
 * 不一致则告警并计数（business_reconcile_mismatch_total）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReconcileTask {

    private final InventoryMapper inventoryMapper;
    private final OrderItemMapper orderItemMapper;
    private final BusinessMetrics businessMetrics;

    @Scheduled(cron = "0 */5 * * * ?")
    public void scheduled() {
        reconcile();
    }

    public Map<String, Object> reconcile() {
        Map<Long, Integer> pending = new HashMap<>();
        for (Map<String, Object> row : orderItemMapper.sumPendingQuantityByBook()) {
            Long bookId = ((Number) row.get("bookId")).longValue();
            Integer qty = ((Number) row.get("qty")).intValue();
            pending.put(bookId, qty);
        }

        List<Inventory> list = inventoryMapper.selectList(null);
        List<Map<String, Object>> mismatches = new ArrayList<>();
        for (Inventory inv : list) {
            int actual = inv.getLockedStock() == null ? 0 : inv.getLockedStock();
            int expected = pending.getOrDefault(inv.getBookId(), 0);
            if (actual != expected || (inv.getStock() != null && inv.getStock() < 0)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("bookId", inv.getBookId());
                m.put("stock", inv.getStock());
                m.put("lockedStock", actual);
                m.put("expectedLocked", expected);
                mismatches.add(m);
                log.error("【库存对账告警】bookId={} locked_stock={} 期望={} stock={}",
                        inv.getBookId(), actual, expected, inv.getStock());
            }
        }
        for (int i = 0; i < mismatches.size(); i++) {
            businessMetrics.reconcileMismatch();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checked", list.size());
        result.put("mismatchCount", mismatches.size());
        result.put("mismatches", mismatches);
        log.info("库存对账完成: 检查 {} 条, 不一致 {} 条", list.size(), mismatches.size());
        return result;
    }
}
