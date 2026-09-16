package com.market.module.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.market.module.product.entity.Book;

import java.util.List;

public interface BookService {

    IPage<Book> page(Integer page, Integer size, Long categoryId, String keyword);

    Book getById(Long id);

    List<Book> getByIds(List<Long> ids);

    void increaseSales(Long bookId, Integer quantity);

    List<Book> search(String keyword, String field, Integer limit);

    List<Book> getHotBooks(Integer limit);

    List<Book> getTopRated(Integer limit);

    void evictBook(Long bookId);
}
