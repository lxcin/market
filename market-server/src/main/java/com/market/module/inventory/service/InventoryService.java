package com.market.module.inventory.service;

public interface InventoryService {

    Integer getStock(Long bookId);

    boolean deductStock(Long bookId, Integer quantity);

    /** 下单预占库存：可用库存减少、锁定库存增加 */
    boolean reserveStock(Long bookId, Integer quantity);

    /** 支付成功：释放锁定（可用库存在预占时已扣，此处只减锁定） */
    void commitStock(Long bookId, Integer quantity);

    /** 取消/超时：释放预占，可用库存回补 */
    void releaseStock(Long bookId, Integer quantity);

    void increaseStock(Long bookId, Integer quantity);

    void initInventory(Long bookId, Integer stock);
}

