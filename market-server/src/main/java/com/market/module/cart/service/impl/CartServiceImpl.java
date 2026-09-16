package com.market.module.cart.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.exception.BusinessException;
import com.market.module.cart.entity.CartItem;
import com.market.module.cart.entity.CartItemVO;
import com.market.module.cart.mapper.CartItemMapper;
import com.market.module.cart.service.CartService;
import com.market.module.product.entity.Book;
import com.market.module.product.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 购物车：Redis 为主存储，变更通过 Redis Stream 可靠异步落库（t_cart 作为备份/对账依据）。
 * 取代原先失效的 RocketMQ 链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    public static final String CART_STREAM = "cart:stream";
    private static final String CART_KEY_PREFIX = "cart:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final BookMapper bookMapper;
    private final CartItemMapper cartItemMapper;

    @Override
    public void addItem(Long userId, Long bookId, Integer quantity) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("Book not found");
        }
        String cartKey = getCartKey(userId);
        String fieldKey = String.valueOf(bookId);
        Object existing = redisTemplate.opsForHash().get(cartKey, fieldKey);

        CartItemVO item;
        if (existing != null) {
            item = JSONUtil.toBean((String) existing, CartItemVO.class);
            item.setQuantity(item.getQuantity() + quantity);
            item.setPrice(book.getPrice());
        } else {
            item = buildCartItemVO(book, quantity);
        }
        redisTemplate.opsForHash().put(cartKey, fieldKey, JSONUtil.toJsonStr(item));
        publishEvent(userId, bookId, item.getQuantity(), Boolean.TRUE.equals(item.getChecked()), "UPSERT");
    }

    @Override
    public void updateQuantity(Long userId, Long bookId, Integer quantity) {
        String cartKey = getCartKey(userId);
        String fieldKey = String.valueOf(bookId);
        Object existing = redisTemplate.opsForHash().get(cartKey, fieldKey);
        if (existing == null) {
            throw new BusinessException("Item not in cart");
        }
        CartItemVO item = JSONUtil.toBean((String) existing, CartItemVO.class);
        item.setQuantity(quantity);
        redisTemplate.opsForHash().put(cartKey, fieldKey, JSONUtil.toJsonStr(item));
        publishEvent(userId, bookId, quantity, Boolean.TRUE.equals(item.getChecked()), "UPSERT");
    }

    @Override
    public void removeItem(Long userId, Long bookId) {
        redisTemplate.opsForHash().delete(getCartKey(userId), String.valueOf(bookId));
        publishEvent(userId, bookId, 0, false, "DELETE");
    }

    @Override
    public void toggleCheck(Long userId, Long bookId) {
        String cartKey = getCartKey(userId);
        String fieldKey = String.valueOf(bookId);
        Object existing = redisTemplate.opsForHash().get(cartKey, fieldKey);
        if (existing == null) {
            throw new BusinessException("Item not in cart");
        }
        CartItemVO item = JSONUtil.toBean((String) existing, CartItemVO.class);
        item.setChecked(!Boolean.TRUE.equals(item.getChecked()));
        redisTemplate.opsForHash().put(cartKey, fieldKey, JSONUtil.toJsonStr(item));
        publishEvent(userId, bookId, item.getQuantity(), Boolean.TRUE.equals(item.getChecked()), "UPSERT");
    }

    @Override
    public void checkAll(Long userId, Boolean checked) {
        String cartKey = getCartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            CartItemVO item = JSONUtil.toBean((String) entry.getValue(), CartItemVO.class);
            item.setChecked(checked);
            redisTemplate.opsForHash().put(cartKey, entry.getKey(), JSONUtil.toJsonStr(item));
            publishEvent(userId, item.getBookId(), item.getQuantity(), Boolean.TRUE.equals(checked), "UPSERT");
        }
    }

    @Override
    public void removeChecked(Long userId) {
        String cartKey = getCartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            CartItemVO item = JSONUtil.toBean((String) entry.getValue(), CartItemVO.class);
            if (Boolean.TRUE.equals(item.getChecked())) {
                redisTemplate.opsForHash().delete(cartKey, entry.getKey());
                publishEvent(userId, item.getBookId(), 0, false, "DELETE");
            }
        }
    }

    @Override
    public List<CartItemVO> list(Long userId) {
        String cartKey = getCartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);
        if (entries.isEmpty()) {
            // Redis 空（如重启/淘汰）时从 DB 备份恢复
            syncFromDb(userId);
            entries = redisTemplate.opsForHash().entries(cartKey);
        }
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<CartItemVO> items = new ArrayList<>();
        Set<Long> bookIds = new HashSet<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            CartItemVO item = JSONUtil.toBean((String) entry.getValue(), CartItemVO.class);
            items.add(item);
            bookIds.add(item.getBookId());
        }

        Map<Long, BigDecimal> priceMap = bookMapper.selectBatchIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Book::getPrice));
        for (CartItemVO item : items) {
            BigDecimal latestPrice = priceMap.get(item.getBookId());
            if (latestPrice != null && !latestPrice.equals(item.getPrice())) {
                item.setPrice(latestPrice);
                redisTemplate.opsForHash().put(cartKey, String.valueOf(item.getBookId()), JSONUtil.toJsonStr(item));
            }
        }
        return items;
    }

    @Override
    public void syncFromDb(Long userId) {
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getUserId, userId);
        List<CartItem> dbItems = cartItemMapper.selectList(wrapper);
        if (dbItems.isEmpty()) {
            return;
        }
        Set<Long> bookIds = dbItems.stream().map(CartItem::getBookId).collect(Collectors.toSet());
        Map<Long, Book> bookMap = bookMapper.selectBatchIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b));

        String cartKey = getCartKey(userId);
        for (CartItem dbItem : dbItems) {
            Book book = bookMap.get(dbItem.getBookId());
            if (book == null) {
                continue;
            }
            CartItemVO vo = buildCartItemVO(book, dbItem.getQuantity());
            vo.setChecked(dbItem.getChecked() == 1);
            redisTemplate.opsForHash().put(cartKey, String.valueOf(dbItem.getBookId()), JSONUtil.toJsonStr(vo));
        }
        log.info("购物车已从 DB 恢复: userId={} 条数={}", userId, dbItems.size());
    }

    @Override
    public Map<String, Object> getCartSummary(Long userId) {
        List<CartItemVO> items = list(userId);
        int totalCount = 0;
        int checkedCount = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (CartItemVO item : items) {
            totalCount += item.getQuantity();
            if (Boolean.TRUE.equals(item.getChecked())) {
                checkedCount += item.getQuantity();
                totalPrice = totalPrice.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("items", items);
        summary.put("totalCount", totalCount);
        summary.put("checkedCount", checkedCount);
        summary.put("totalPrice", totalPrice);
        return summary;
    }

    private String getCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private CartItemVO buildCartItemVO(Book book, Integer quantity) {
        CartItemVO vo = new CartItemVO();
        vo.setBookId(book.getId());
        vo.setTitle(book.getTitle());
        vo.setAuthor(book.getAuthor());
        vo.setCoverImage(book.getCoverImage());
        vo.setPrice(book.getPrice());
        vo.setQuantity(quantity);
        vo.setChecked(true);
        return vo;
    }

    /** 变更事件写入 Redis Stream，由 CartPersistWorker 可靠落库 */
    private void publishEvent(Long userId, Long bookId, Integer quantity, boolean checked, String op) {
        Map<String, String> msg = new HashMap<>();
        msg.put("userId", String.valueOf(userId));
        msg.put("bookId", String.valueOf(bookId));
        msg.put("quantity", String.valueOf(quantity == null ? 0 : quantity));
        msg.put("checked", checked ? "1" : "0");
        msg.put("op", op);
        try {
            stringRedisTemplate.opsForStream().add(CART_STREAM, msg);
        } catch (Exception e) {
            log.error("购物车持久化事件投递失败 userId={} bookId={}", userId, bookId, e);
        }
    }
}
