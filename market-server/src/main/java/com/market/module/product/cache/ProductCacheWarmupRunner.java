package com.market.module.product.cache;

import com.market.common.cache.RedisBloomFilter;
import com.market.module.product.entity.Book;
import com.market.module.product.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时把商品 ID 灌入布隆过滤器，作为防缓存穿透的第一道闸。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheWarmupRunner implements ApplicationRunner {

    private final BookMapper bookMapper;
    private final RedisBloomFilter bloomFilter;

    @Override
    public void run(ApplicationArguments args) {
        try {
            bloomFilter.reset();
            List<Book> books = bookMapper.selectList(null);
            for (Book book : books) {
                bloomFilter.add(book.getId());
            }
            log.info("商品布隆过滤器预热完成，共 {} 个商品", books.size());
        } catch (Exception e) {
            log.error("商品布隆过滤器预热失败", e);
        }
    }
}
