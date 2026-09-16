package com.market.module.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.module.cart.entity.CartItem;
import com.market.module.cart.mapper.CartItemMapper;
import com.market.module.cart.service.impl.CartServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车持久化消费者：Redis Stream 消费组，阻塞读取（无空转），
 * 按事件 op 幂等 upsert/delete t_cart，失败留 PEL 重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartPersistWorker {

    private static final String STREAM = CartServiceImpl.CART_STREAM;
    private static final String GROUP = "cart-group";
    private static final String CONSUMER = "cart-consumer-1";

    private final StringRedisTemplate stringRedisTemplate;
    private final CartItemMapper cartItemMapper;

    @PostConstruct
    public void initGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM, ReadOffset.from("0"), GROUP);
            log.info("购物车消费组已创建: {}", GROUP);
        } catch (Exception e) {
            log.info("购物车消费组已存在或跳过: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 500)
    public void consume() {
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(GROUP, CONSUMER),
                    StreamReadOptions.empty().count(50).block(Duration.ofSeconds(2)),
                    StreamOffset.create(STREAM, ReadOffset.lastConsumed()));
            if (records == null || records.isEmpty()) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                process(record);
            }
        } catch (Exception e) {
            log.error("购物车持久化消费异常", e);
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void retryPending() {
        try {
            PendingMessages pending = stringRedisTemplate.opsForStream().pending(
                    STREAM, GROUP, Range.unbounded(), 100L);
            if (pending == null || pending.isEmpty()) {
                return;
            }
            List<RecordId> ids = new ArrayList<>();
            for (PendingMessage pm : pending) {
                ids.add(pm.getId());
            }
            if (ids.isEmpty()) {
                return;
            }
            List<MapRecord<String, Object, Object>> claimed = stringRedisTemplate.opsForStream().claim(
                    STREAM, GROUP, CONSUMER, Duration.ofSeconds(60), ids.toArray(new RecordId[0]));
            if (claimed != null) {
                for (MapRecord<String, Object, Object> record : claimed) {
                    process(record);
                }
            }
        } catch (Exception e) {
            log.error("购物车持久化重试异常", e);
        }
    }

    private void process(MapRecord<String, Object, Object> record) {
        try {
            Long userId = Long.valueOf(String.valueOf(record.getValue().get("userId")));
            Long bookId = Long.valueOf(String.valueOf(record.getValue().get("bookId")));
            String op = String.valueOf(record.getValue().get("op"));

            if ("DELETE".equals(op)) {
                cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId).eq(CartItem::getBookId, bookId));
            } else {
                int quantity = Integer.parseInt(String.valueOf(record.getValue().get("quantity")));
                int checked = "1".equals(String.valueOf(record.getValue().get("checked"))) ? 1 : 0;
                CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId).eq(CartItem::getBookId, bookId));
                if (existing != null) {
                    existing.setQuantity(quantity);
                    existing.setChecked(checked);
                    cartItemMapper.updateById(existing);
                } else {
                    CartItem item = new CartItem();
                    item.setUserId(userId);
                    item.setBookId(bookId);
                    item.setQuantity(quantity);
                    item.setChecked(checked);
                    cartItemMapper.insert(item);
                }
            }
            acknowledge(record.getId());
        } catch (Exception e) {
            log.error("购物车落库失败，稍后重试: recordId={}", record.getId(), e);
        }
    }

    private void acknowledge(RecordId recordId) {
        try {
            stringRedisTemplate.opsForStream().acknowledge(STREAM, GROUP, recordId);
        } catch (Exception e) {
            log.warn("购物车消息确认失败: {}", recordId, e);
        }
    }
}
