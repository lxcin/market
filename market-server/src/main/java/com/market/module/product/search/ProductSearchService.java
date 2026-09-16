package com.market.module.product.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.json.JsonData;
import com.market.module.product.entity.Book;
import com.market.module.product.entity.Category;
import com.market.module.product.mapper.BookMapper;
import com.market.module.product.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品搜索：Elasticsearch（倒排索引 + 中文 IK 分词 + BM25）。
 * 支持字段维度（全部/书名/作者）、作者精确筛选、分类/作者聚合、分字段高亮。
 * 索引采用「版本化索引 + 别名」实现零停机重建：写/查都走别名 book。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    public static final String ALIAS = "book";
    private static final String INDEX_PREFIX = "book_v";

    private final ElasticsearchClient esClient;
    private final RestClient restClient;
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;

    // ---------------- 索引管理 ----------------

    public synchronized int rebuildIndex() {
        try {
            String old = aliasedIndex();
            String newIndex = INDEX_PREFIX + System.currentTimeMillis();
            createVersionedIndex(newIndex);
            int count = bulkLoad(newIndex);
            switchAlias(old, newIndex);
            if (old != null && !old.equals(newIndex)) {
                esClient.indices().delete(d -> d.index(old));
                log.info("ES 索引重建完成，别名 [{}] -> {}，旧索引 {} 已删除", ALIAS, newIndex, old);
            } else {
                log.info("ES 索引创建完成，别名 [{}] -> {}", ALIAS, newIndex);
            }
            return count;
        } catch (Exception e) {
            log.error("ES 索引重建失败", e);
            return 0;
        }
    }

    public void refreshIntoAlias() {
        try {
            bulkLoad(ALIAS);
            log.info("ES 索引刷新完成 -> {}", ALIAS);
        } catch (Exception e) {
            log.error("ES 索引刷新失败", e);
        }
    }

    public void indexBook(Book book) {
        try {
            Category category = book.getCategoryId() == null ? null : categoryMapper.selectById(book.getCategoryId());
            String categoryName = category == null ? null : category.getName();
            esClient.index(i -> i.index(ALIAS).id(String.valueOf(book.getId()))
                    .document(toDocument(book, categoryName)));
        } catch (Exception e) {
            log.error("ES 索引商品失败 id={}", book.getId(), e);
        }
    }

    public void deleteBook(Long id) {
        try {
            esClient.delete(d -> d.index(ALIAS).id(String.valueOf(id)));
        } catch (Exception e) {
            log.error("ES 删除商品索引失败 id={}", id, e);
        }
    }

    // ---------------- 查询 ----------------

    /** 简单搜索：返回 Book 列表（field: all/title/author） */
    public List<Book> search(String keyword, String field, int limit) throws Exception {
        List<String> fields = resolveFields(field);
        SearchResponse<Map> resp = esClient.search(s -> {
            s.index(ALIAS).size(limit);
            s.query(q -> q.bool(b -> {
                b.must(m -> m.multiMatch(mm -> mm.query(keyword).fields(fields)));
                b.filter(f -> f.term(t -> t.field("status").value(1)));
                return b;
            }));
            return s;
        }, Map.class);
        List<Book> books = new ArrayList<>();
        for (Hit<Map> hit : resp.hits().hits()) {
            books.add(toBook(hit.id(), hit.source()));
        }
        return books;
    }

    /** 高级搜索：字段维度 + 分面导航(post_filter) + 排序 + 分字段高亮 + 分类/作者/价格聚合 */
    public Map<String, Object> searchPage(String keyword, String field, Long categoryId, String categoryName,
                                          String author, Double minPrice, Double maxPrice, String sort,
                                          int page, int size) throws Exception {
        List<String> fields = resolveFields(field);
        int from = Math.max(0, (page - 1) * size);
        boolean hasPostFilter = categoryId != null || StringUtils.hasText(categoryName)
                || StringUtils.hasText(author) || minPrice != null || maxPrice != null;

        SearchResponse<Map> resp = esClient.search(s -> {
            s.index(ALIAS).from(from).size(size);
            s.query(q -> q.bool(b -> {
                if (StringUtils.hasText(keyword)) {
                    b.must(m -> m.multiMatch(mm -> mm.query(keyword).fields(fields)));
                } else {
                    b.must(m -> m.matchAll(ma -> ma));
                }
                b.filter(f -> f.term(t -> t.field("status").value(1)));
                return b;
            }));
            // 筛选走 post_filter：命中结果受筛选限制，但聚合按未筛选的候选集计算，实现分面导航
            if (hasPostFilter) {
                s.postFilter(q -> q.bool(b -> {
                    if (categoryId != null) {
                        b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
                    }
                    if (StringUtils.hasText(categoryName)) {
                        b.filter(f -> f.term(t -> t.field("categoryName").value(categoryName)));
                    }
                    if (StringUtils.hasText(author)) {
                        b.filter(f -> f.term(t -> t.field("author.keyword").value(author)));
                    }
                    if (minPrice != null || maxPrice != null) {
                        b.filter(f -> f.range(r -> {
                            r.field("price");
                            if (minPrice != null) r.gte(JsonData.of(minPrice));
                            if (maxPrice != null) r.lte(JsonData.of(maxPrice));
                            return r;
                        }));
                    }
                    return b;
                }));
            }
            if (StringUtils.hasText(keyword)) {
                s.highlight(h -> h.fields("title", hf -> hf).fields("author", hf -> hf).fields("description", hf -> hf));
            }
            switch (sort == null ? "" : sort) {
                case "sales" -> s.sort(so -> so.field(f -> f.field("sales").order(SortOrder.Desc)));
                case "rate" -> s.sort(so -> so.field(f -> f.field("rate").order(SortOrder.Desc)));
                case "price_asc" -> s.sort(so -> so.field(f -> f.field("price").order(SortOrder.Asc)));
                case "price_desc" -> s.sort(so -> so.field(f -> f.field("price").order(SortOrder.Desc)));
                default -> { }
            }
            s.aggregations("byCategory", a -> a.terms(t -> t.field("categoryName").size(20)));
            s.aggregations("byPrice", a -> a.range(r -> r.field("price").ranges(
                    AggregationRange.of(x -> x.key("0-50").to("50")),
                    AggregationRange.of(x -> x.key("50-100").from("50").to("100")),
                    AggregationRange.of(x -> x.key("100-200").from("100").to("200")),
                    AggregationRange.of(x -> x.key("200+").from("200")))));
            return s;
        }, Map.class);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Hit<Map> hit : resp.hits().hits()) {
            Map<String, Object> item = new HashMap<>();
            item.put("book", toBook(hit.id(), hit.source()));
            item.put("highlight", hit.highlight());
            items.add(item);
        }

        Map<String, Object> facets = new HashMap<>();
        facets.put("categories", parseBuckets(resp, "byCategory", "categoryName"));
        facets.put("prices", parseRangeBuckets(resp, "byPrice"));

        Map<String, Object> result = new HashMap<>();
        long total = resp.hits().total() == null ? items.size() : resp.hits().total().value();
        result.put("total", total);
        result.put("items", items);
        result.put("facets", facets);
        return result;
    }

    private List<Map<String, Object>> parseRangeBuckets(SearchResponse<Map> resp, String aggName) {
        List<Map<String, Object>> list = new ArrayList<>();
        var agg = resp.aggregations().get(aggName);
        if (agg != null && agg.isRange()) {
            for (var bucket : agg.range().buckets().array()) {
                Map<String, Object> f = new HashMap<>();
                f.put("key", bucket.key());
                f.put("count", bucket.docCount());
                list.add(f);
            }
        }
        return list;
    }

    /** 作者主页：该作者的全部作品（按销量排序）+ 分类分布 + 平均评分 */
    public Map<String, Object> authorPage(String author, int page, int size) throws Exception {
        int from = Math.max(0, (page - 1) * size);
        SearchResponse<Map> resp = esClient.search(s -> {
            s.index(ALIAS).from(from).size(size);
            s.query(q -> q.bool(b -> {
                b.must(m -> m.term(t -> t.field("author.keyword").value(author)));
                b.filter(f -> f.term(t -> t.field("status").value(1)));
                return b;
            }));
            s.sort(so -> so.field(f -> f.field("sales").order(SortOrder.Desc)));
            s.aggregations("byCategory", a -> a.terms(t -> t.field("categoryName").size(20)));
            s.aggregations("avgRate", a -> a.avg(av -> av.field("rate")));
            return s;
        }, Map.class);

        List<Book> items = new ArrayList<>();
        for (Hit<Map> hit : resp.hits().hits()) {
            items.add(toBook(hit.id(), hit.source()));
        }
        var avg = resp.aggregations().get("avgRate");

        Map<String, Object> result = new HashMap<>();
        result.put("author", author);
        result.put("total", resp.hits().total() == null ? items.size() : resp.hits().total().value());
        result.put("items", items);
        result.put("categories", parseBuckets(resp, "byCategory", "categoryName"));
        result.put("avgRate", (avg != null && avg.isAvg()) ? avg.avg().value() : 0.0);
        return result;
    }

    /** 搜索建议：标题/作者（含拼音、首字母）前缀补全 */
    public List<String> suggest(String prefix, int limit) throws Exception {
        if (!StringUtils.hasText(prefix)) {
            return List.of();
        }
        String p = prefix.trim();
        String lower = p.toLowerCase();
        SearchResponse<Map> resp = esClient.search(s -> {
            s.index(ALIAS).size(limit);
            s.query(q -> q.bool(b -> {
                b.should(sh -> sh.prefix(pr -> pr.field("title.keyword").value(p).caseInsensitive(true)));
                b.should(sh -> sh.prefix(pr -> pr.field("author.keyword").value(p).caseInsensitive(true)));
                b.should(sh -> sh.prefix(pr -> pr.field("title.pinyin").value(lower)));
                b.should(sh -> sh.prefix(pr -> pr.field("author.pinyin").value(lower)));
                b.filter(f -> f.term(t -> t.field("status").value(1)));
                b.minimumShouldMatch("1");
                return b;
            }));
            return s;
        }, Map.class);

        List<String> out = new ArrayList<>();
        for (Hit<Map> hit : resp.hits().hits()) {
            addDistinct(out, hit.source().get("title"), limit);
            addDistinct(out, hit.source().get("author"), limit);
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private void addDistinct(List<String> list, Object value, int limit) {
        if (value == null || list.size() >= limit) {
            return;
        }
        String s = value.toString();
        if (!list.contains(s)) {
            list.add(s);
        }
    }

    private List<Map<String, Object>> parseBuckets(SearchResponse<Map> resp, String aggName, String keyName) {
        List<Map<String, Object>> list = new ArrayList<>();
        var agg = resp.aggregations().get(aggName);
        if (agg != null && agg.isSterms()) {
            for (var bucket : agg.sterms().buckets().array()) {
                Map<String, Object> f = new HashMap<>();
                f.put(keyName, bucket.key().stringValue());
                f.put("count", bucket.docCount());
                list.add(f);
            }
        }
        return list;
    }

    private List<String> resolveFields(String field) {
        if ("title".equals(field)) {
            return List.of("title^2", "title.pinyin^2");
        }
        if ("author".equals(field)) {
            return List.of("author^2", "author.pinyin^2");
        }
        return List.of("title^3", "author^2", "description", "title.pinyin^2", "author.pinyin^2");
    }

    // ---------------- mapping / 数据 ----------------

    private void createVersionedIndex(String index) throws Exception {
        String body = """
                {
                  "settings": {
                    "number_of_replicas": 0,
                    "analysis": {
                      "filter": {
                        "pinyin_filter": {
                          "type": "pinyin",
                          "keep_joined_full_pinyin": true,
                          "keep_first_letter": true,
                          "keep_full_pinyin": false,
                          "keep_none_chinese": true,
                          "keep_none_chinese_together": true,
                          "none_chinese_pick": 1
                        }
                      },
                      "analyzer": {
                        "pinyin_analyzer": { "type": "custom", "tokenizer": "keyword", "filter": ["pinyin_filter"] }
                      }
                    }
                  },
                  "mappings": {
                    "dynamic": "strict",
                    "properties": {
                      "title": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart",
                        "fields": { "keyword": { "type": "keyword" }, "pinyin": { "type": "text", "analyzer": "pinyin_analyzer" } } },
                      "author": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart",
                        "fields": { "keyword": { "type": "keyword" }, "pinyin": { "type": "text", "analyzer": "pinyin_analyzer" } } },
                      "description": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                      "categoryId": { "type": "long" },
                      "categoryName": { "type": "keyword" },
                      "coverImage": { "type": "keyword" },
                      "price": { "type": "double" },
                      "rate": { "type": "double" },
                      "sales": { "type": "integer" },
                      "status": { "type": "integer" }
                    }
                  }
                }
                """;
        Request request = new Request("PUT", "/" + index);
        request.setJsonEntity(body);
        restClient.performRequest(request);
    }

    private int bulkLoad(String index) throws Exception {
        List<Book> books = bookMapper.selectList(null);
        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        BulkRequest.Builder bulk = new BulkRequest.Builder();
        for (Book book : books) {
            Map<String, Object> doc = toDocument(book, categoryNames.get(book.getCategoryId()));
            bulk.operations(op -> op.index(idx -> idx.index(index)
                    .id(String.valueOf(book.getId())).document(doc)));
        }
        esClient.bulk(bulk.build());
        return books.size();
    }

    private void switchAlias(String oldIndex, String newIndex) throws Exception {
        List<Action> actions = new ArrayList<>();
        if (oldIndex != null && !oldIndex.equals(newIndex)) {
            actions.add(Action.of(a -> a.remove(r -> r.index(oldIndex).alias(ALIAS))));
        }
        actions.add(Action.of(a -> a.add(ad -> ad.index(newIndex).alias(ALIAS).isWriteIndex(true))));
        esClient.indices().updateAliases(u -> u.actions(actions));
    }

    private String aliasedIndex() {
        try {
            GetAliasResponse resp = esClient.indices().getAlias(g -> g.name(ALIAS));
            return resp.result().keySet().stream().findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Book toBook(String id, Map source) {
        Book b = new Book();
        if (id != null) {
            b.setId(Long.valueOf(id));
        }
        b.setTitle((String) source.get("title"));
        b.setAuthor((String) source.get("author"));
        b.setDescription((String) source.get("description"));
        b.setCoverImage((String) source.get("coverImage"));
        b.setCategoryId(asLong(source.get("categoryId"), null));
        Object price = source.get("price");
        if (price != null) {
            b.setPrice(new BigDecimal(price.toString()));
        }
        Object rate = source.get("rate");
        if (rate != null) {
            b.setRate(((Number) rate).doubleValue());
        }
        b.setSales(asLong(source.get("sales"), 0L).intValue());
        b.setStatus(asLong(source.get("status"), 1L).intValue());
        return b;
    }

    private Long asLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value instanceof Number n ? n.longValue() : Long.valueOf(value.toString());
    }

    private Map<String, Object> toDocument(Book book, String categoryName) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("title", book.getTitle());
        doc.put("author", book.getAuthor());
        doc.put("description", book.getDescription());
        doc.put("categoryId", book.getCategoryId());
        doc.put("price", book.getPrice());
        doc.put("rate", book.getRate());
        doc.put("sales", book.getSales());
        doc.put("status", book.getStatus());
        doc.put("coverImage", book.getCoverImage());
        if (categoryName != null) {
            doc.put("categoryName", categoryName);
        }
        return doc;
    }
}
