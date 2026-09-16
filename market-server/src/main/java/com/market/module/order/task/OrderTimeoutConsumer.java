package com.market.module.order.task;

import com.market.module.order.service.OrderDelayQueue;
import com.market.module.order.service.OrderService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 延时关单消费者：专用线程用 RBlockingQueue.take() 阻塞等待到期订单，
 * 来一个处理一个，没有定时轮询空转。CAS 幂等，重复/乱序安全。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderDelayQueue orderDelayQueue;
    private final OrderService orderService;

    private volatile boolean running = true;
    private Thread worker;

    @PostConstruct
    public void start() {
        worker = new Thread(this::loop, "order-timeout-consumer");
        worker.setDaemon(true);
        worker.start();
        log.info("延时关单消费者已启动（take 阻塞模式，无空转）");
    }

    private void loop() {
        while (running) {
            try {
                Long orderId = orderDelayQueue.take();
                if (orderId == null) {
                    continue;
                }
                orderService.cancelTimeoutOrder(orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("延时关单处理失败", e);
            }
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }
}
