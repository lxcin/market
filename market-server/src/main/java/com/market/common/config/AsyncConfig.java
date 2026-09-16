package com.market.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 支付回调处理线程池：网关回调与主业务线程解耦，模拟真实网关的异步通知。
 * 使用 ContextPropagatingTaskDecorator 让提交任务时捕获的 TraceContext/MDC
 * 传播到异步线程，保证回调链路与发起请求同一条 trace、日志带同一 traceId。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "payCallbackExecutor")
    public ThreadPoolTaskExecutor payCallbackExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("pay-callback-");
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        executor.initialize();
        return executor;
    }
}
