package com.market.module.product.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 启动时建索引并全量同步；之后定时全量刷新（商品量小，简单可靠）。
 * 生产环境应改为增量同步（updated_at 水位 / 本地消息 / Canal）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchInitializer implements ApplicationRunner {

    private final ProductSearchService searchService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            searchService.rebuildIndex();
        } catch (Exception e) {
            log.error("ES 索引初始化失败（搜索将降级到数据库）", e);
        }
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    public void refreshIndex() {
        searchService.refreshIntoAlias();
    }
}
