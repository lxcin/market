package com.market.module.cart.controller;

import com.market.common.response.Result;
import com.market.common.util.SecurityUtil;
import com.market.module.cart.entity.CartItemVO;
import com.market.module.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(cartService.list(userId));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody CartAddRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        cartService.addItem(userId, request.getBookId(), request.getQuantity());
        return Result.success();
    }

    @PutMapping("/quantity")
    public Result<Void> updateQuantity(@Valid @RequestBody CartAddRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        cartService.updateQuantity(userId, request.getBookId(), request.getQuantity());
        return Result.success();
    }

    @DeleteMapping("/{bookId}")
    public Result<Void> remove(@PathVariable Long bookId) {
        Long userId = SecurityUtil.getCurrentUserId();
        cartService.removeItem(userId, bookId);
        return Result.success();
    }

    @PutMapping("/check/{bookId}")
    public Result<Void> toggleCheck(@PathVariable Long bookId) {
        Long userId = SecurityUtil.getCurrentUserId();
        cartService.toggleCheck(userId, bookId);
        return Result.success();
    }

    @PutMapping("/check-all")
    public Result<Void> checkAll(@RequestBody CheckAllRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        cartService.checkAll(userId, request.getChecked());
        return Result.success();
    }

    @DeleteMapping("/checked")
    public Result<Void> removeChecked() {
        Long userId = SecurityUtil.getCurrentUserId();
        cartService.removeChecked(userId);
        return Result.success();
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(cartService.getCartSummary(userId));
    }

    @Data
    static class CheckAllRequest {
        private Boolean checked;
    }
}
