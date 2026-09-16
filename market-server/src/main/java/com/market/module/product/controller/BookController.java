package com.market.module.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.market.common.response.Result;
import com.market.module.product.entity.Book;
import com.market.module.product.search.ProductSearchService;
import com.market.module.product.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ProductSearchService searchService;

    @GetMapping("/page")
    public Result<IPage<Book>> page(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "20") Integer size,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) String keyword) {
        return Result.success(bookService.page(page, size, categoryId, keyword));
    }

    @GetMapping("/{id}")
    public Result<Book> getById(@PathVariable Long id) {
        return Result.success(bookService.getById(id));
    }

    /** 关键词搜索：优先 ES，异常降级 MySQL。field: all / title / author */
    @GetMapping("/search")
    public Result<List<Book>> search(@RequestParam String keyword,
                                     @RequestParam(required = false) String field,
                                     @RequestParam(defaultValue = "20") Integer limit) {
        try {
            return Result.success(searchService.search(keyword, field, limit));
        } catch (Exception e) {
            log.warn("ES 搜索失败，降级 MySQL: {}", e.getMessage());
            return Result.success(bookService.search(keyword, field, limit));
        }
    }

    /** 高级搜索：字段维度 + 过滤 + 排序 + 分字段高亮 + 分类/作者聚合 */
    @GetMapping("/search/rich")
    public Result<Map<String, Object>> searchRich(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String field,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String categoryName,
                                                  @RequestParam(required = false) String author,
                                                  @RequestParam(required = false) Double minPrice,
                                                  @RequestParam(required = false) Double maxPrice,
                                                  @RequestParam(required = false) String sort,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "12") Integer size) {
        try {
            return Result.success(searchService.searchPage(keyword, field, categoryId, categoryName, author,
                    minPrice, maxPrice, sort, page, size));
        } catch (Exception e) {
            log.warn("ES 高级搜索失败，降级 MySQL", e);
            List<Book> list = bookService.search(keyword == null ? "" : keyword, field, size);
            List<Map<String, Object>> items = new ArrayList<>();
            for (Book b : list) {
                Map<String, Object> item = new HashMap<>();
                item.put("book", b);
                item.put("highlight", Map.of());
                items.add(item);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("total", list.size());
            result.put("items", items);
            result.put("facets", Map.of("categories", List.of(), "prices", List.of()));
            result.put("degraded", true);
            return Result.success(result);
        }
    }

    /** 作者主页：该作者的全部作品 + 分类分布 + 平均评分 */
    @GetMapping("/author")
    public Result<Map<String, Object>> authorBooks(@RequestParam String name,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "12") Integer size) {
        try {
            return Result.success(searchService.authorPage(name, page, size));
        } catch (Exception e) {
            log.warn("ES 作者页失败，降级 MySQL: {}", e.getMessage());
            List<Book> items = new ArrayList<>(bookService.search(name, "author", size));
            Map<String, Object> result = new HashMap<>();
            result.put("author", name);
            result.put("total", items.size());
            result.put("items", items);
            result.put("categories", List.of());
            result.put("avgRate", 0.0);
            result.put("degraded", true);
            return Result.success(result);
        }
    }

    /** 搜索建议：标题/作者（含拼音、首字母）前缀补全 */
    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam String prefix,
                                        @RequestParam(defaultValue = "10") Integer limit) {
        try {
            return Result.success(searchService.suggest(prefix, limit));
        } catch (Exception e) {
            log.warn("ES 搜索建议失败: {}", e.getMessage());
            return Result.success(List.of());
        }
    }

    @GetMapping("/hot")
    public Result<List<Book>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(bookService.getHotBooks(limit));
    }

    @GetMapping("/top-rated")
    public Result<List<Book>> topRated(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(bookService.getTopRated(limit));
    }
}
