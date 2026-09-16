package com.market.module.product.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.market.common.cache.CacheHelper;
import com.market.common.cache.RedisBloomFilter;
import com.market.module.product.entity.Book;
import com.market.module.product.mapper.BookMapper;
import com.market.module.product.service.BookService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private static final Duration BOOK_TTL = Duration.ofMinutes(10);
    private static final Duration LIST_TTL = Duration.ofSeconds(60);

    private final BookMapper bookMapper;
    private final CacheHelper cacheHelper;
    private final RedisBloomFilter bloomFilter;

    @Override
    public IPage<Book> page(Integer page, Integer size, Long categoryId, String keyword) {
        Page<Book> pageParam = new Page<>(page, size);
        return bookMapper.selectPageWithCategory(pageParam, categoryId, keyword);
    }

    @Override
    @Observed(name = "book.detail")
    public Book getById(Long id) {
        // 布隆过滤器：一定不存在直接返回，防穿透
        if (id == null || !bloomFilter.mightContain(id)) {
            return null;
        }
        return cacheHelper.getOrLoad("cache:book:" + id, BOOK_TTL,
                () -> bookMapper.selectById(id));
    }

    @Override
    public List<Book> getByIds(List<Long> ids) {
        return bookMapper.selectBatchIds(ids);
    }

    @Override
    @Transactional
    public void increaseSales(Long bookId, Integer quantity) {
        Book book = bookMapper.selectById(bookId);
        if (book != null) {
            book.setSales(book.getSales() + quantity);
            bookMapper.updateById(book);
            evictBook(bookId);
        }
    }

    @Override
    public List<Book> search(String keyword, String field, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>();
        }
        String kw = keyword.trim();
        List<Book> results = bookMapper.searchByKeyword(kw, field, limit);
        if (results.isEmpty() && kw.length() > 1) {
            for (int i = kw.length() - 1; i >= 1; i--) {
                results = bookMapper.searchByKeyword(kw.substring(0, i), field, limit);
                if (!results.isEmpty()) break;
            }
        }
        return results;
    }

    @Override
    public List<Book> getHotBooks(Integer limit) {
        return cacheHelper.getOrLoad("cache:book:hot:" + limit, LIST_TTL,
                () -> bookMapper.selectHotBooks(limit));
    }

    @Override
    public List<Book> getTopRated(Integer limit) {
        return cacheHelper.getOrLoad("cache:book:top:" + limit, LIST_TTL,
                () -> bookMapper.selectTopRated(limit));
    }

    /** 商品写操作后清理相关缓存 */
    @Override
    public void evictBook(Long bookId) {
        cacheHelper.evict("cache:book:" + bookId);
        cacheHelper.evictByPattern("cache:book:hot:*");
        cacheHelper.evictByPattern("cache:book:top:*");
    }
}
