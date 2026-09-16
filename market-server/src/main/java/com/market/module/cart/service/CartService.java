package com.market.module.cart.service;

import com.market.module.cart.entity.CartItemVO;

import java.util.List;
import java.util.Map;

public interface CartService {

    void addItem(Long userId, Long bookId, Integer quantity);

    void updateQuantity(Long userId, Long bookId, Integer quantity);

    void removeItem(Long userId, Long bookId);

    void toggleCheck(Long userId, Long bookId);

    void checkAll(Long userId, Boolean checked);

    void removeChecked(Long userId);

    List<CartItemVO> list(Long userId);

    void syncFromDb(Long userId);

    Map<String, Object> getCartSummary(Long userId);
}
