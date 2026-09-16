package com.market.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.exception.BusinessException;
import com.market.module.inventory.entity.Inventory;
import com.market.module.inventory.mapper.InventoryMapper;
import com.market.module.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;

    @Override
    public Integer getStock(Long bookId) {
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getBookId, bookId));
        return inventory != null ? inventory.getStock() : 0;
    }

    @Override
    @Transactional
    public boolean deductStock(Long bookId, Integer quantity) {
        for (int i = 0; i < 3; i++) {
            Inventory inventory = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<Inventory>().eq(Inventory::getBookId, bookId));
            if (inventory == null || inventory.getStock() < quantity) {
                throw new BusinessException("库存不足");
            }
            int rows = inventoryMapper.deductStock(bookId, quantity, inventory.getVersion());
            if (rows > 0) {
                return true;
            }
        }
        throw new BusinessException("库存不足");
    }

    @Override
    @Transactional
    public boolean reserveStock(Long bookId, Integer quantity) {
        for (int i = 0; i < 3; i++) {
            Inventory inventory = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<Inventory>().eq(Inventory::getBookId, bookId));
            if (inventory == null || inventory.getStock() < quantity) {
                throw new BusinessException("库存不足");
            }
            int rows = inventoryMapper.reserveStock(bookId, quantity, inventory.getVersion());
            if (rows > 0) {
                return true;
            }
        }
        throw new BusinessException("库存不足");
    }

    @Override
    public void commitStock(Long bookId, Integer quantity) {
        inventoryMapper.commitStock(bookId, quantity);
    }

    @Override
    public void releaseStock(Long bookId, Integer quantity) {
        inventoryMapper.releaseStock(bookId, quantity);
    }

    @Override
    public void increaseStock(Long bookId, Integer quantity) {
        inventoryMapper.increaseStock(bookId, quantity);
    }

    @Override
    @Transactional
    public void initInventory(Long bookId, Integer stock) {
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getBookId, bookId));
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setBookId(bookId);
            inventory.setStock(stock);
            inventory.setVersion(0);
            inventoryMapper.insert(inventory);
        } else {
            inventory.setStock(stock);
            inventoryMapper.updateById(inventory);
        }
    }
}
