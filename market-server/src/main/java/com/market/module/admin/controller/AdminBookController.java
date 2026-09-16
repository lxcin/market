package com.market.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.market.common.cache.RedisBloomFilter;
import com.market.common.response.Result;
import com.market.module.product.entity.Book;
import com.market.module.product.mapper.BookMapper;
import com.market.module.product.search.ProductSearchService;
import com.market.module.product.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/book")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookController {

    private final BookMapper bookMapper;
    private final BookService bookService;
    private final RedisBloomFilter bloomFilter;
    private final ProductSearchService searchService;

    @GetMapping("/page")
    public Result<Page<Book>> page(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer size,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Book::getTitle, keyword).or().like(Book::getAuthor, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Book::getStatus, status);
        }
        wrapper.orderByDesc(Book::getCreatedAt);
        return Result.success(bookMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{id}")
    public Result<Book> getById(@PathVariable Long id) {
        return Result.success(bookMapper.selectById(id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody AdminBookRequest request) {
        Book book = new Book();
        BeanUtils.copyProperties(request, book);
        bookMapper.insert(book);
        bloomFilter.add(book.getId());
        searchService.indexBook(book);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminBookRequest request) {
        Book book = new Book();
        BeanUtils.copyProperties(request, book);
        book.setId(id);
        bookMapper.updateById(book);
        bookService.evictBook(id);
        searchService.indexBook(bookMapper.selectById(id));
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        Book book = new Book();
        book.setId(id);
        book.setStatus(request.getStatus());
        bookMapper.updateById(book);
        bookService.evictBook(id);
        searchService.indexBook(bookMapper.selectById(id));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bookMapper.deleteById(id);
        bookService.evictBook(id);
        searchService.deleteBook(id);
        return Result.success();
    }
}
