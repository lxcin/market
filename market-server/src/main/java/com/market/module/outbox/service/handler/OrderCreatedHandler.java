package com.market.module.outbox.service.handler;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.market.module.outbox.entity.OutboxMessage;
import com.market.module.outbox.service.OutboxEventHandler;
import com.market.module.product.entity.Book;
import com.market.module.product.mapper.BookMapper;
import com.market.module.product.search.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单创建事件：累加商品销量（派生数据，异步最终一致），并同步刷新 ES 中的商品（实时）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedHandler implements OutboxEventHandler {

    private final BookMapper bookMapper;
    private final ProductSearchService productSearchService;

    @Override
    public String eventType() {
        return "ORDER_CREATED";
    }

    @Override
    public void handle(OutboxMessage message) {
        JSONObject json = JSONUtil.parseObj(message.getPayload());
        JSONArray items = json.getJSONArray("items");
        if (items != null) {
            for (Object o : items) {
                JSONObject it = (JSONObject) o;
                Long bookId = it.getLong("bookId");
                Integer quantity = it.getInt("quantity");
                Book book = bookMapper.selectById(bookId);
                if (book != null) {
                    book.setSales((book.getSales() == null ? 0 : book.getSales()) + quantity);
                    bookMapper.updateById(book);
                    // 实时同步 ES（销量）
                    productSearchService.indexBook(book);
                }
            }
        }
        log.info("Outbox[ORDER_CREATED] 已累加销量并刷新 ES，orderId={}", message.getAggregateId());
    }
}
