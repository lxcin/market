package com.market.module.order.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 订单延时关单队列（基于 Redisson 延时队列，底层 Redis ZSet，重启不丢）。
 * 下单时投递一个"到期关单"任务，到期后转移到目标阻塞队列，由消费者处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayQueue {

    private static final String DEST_QUEUE = "order:timeout:queue";

    private final RedissonClient redissonClient;

    @Value("${order.pay-timeout-seconds:1800}")
    private long payTimeoutSeconds;

    private RBlockingQueue<String> destQueue;
    private RDelayedQueue<String> delayedQueue;

    @PostConstruct
    public void init() {
        destQueue = redissonClient.getBlockingQueue(DEST_QUEUE);
        delayedQueue = redissonClient.getDelayedQueue(destQueue);
        log.info("订单延时关单队列初始化完成，超时时间 {}s", payTimeoutSeconds);
    }

    public void scheduleCancel(Long orderId) {
        delayedQueue.offer(String.valueOf(orderId), payTimeoutSeconds, TimeUnit.SECONDS);
        log.info("订单 {} 已投递延时关单，{}s 后到期", orderId, payTimeoutSeconds);
    }

    /** 阻塞等待到期订单（无空转），被中断时抛 InterruptedException */
    public Long take() throws InterruptedException {
        String value = destQueue.take();
        return value == null ? null : Long.valueOf(value);
    }
}
