package com.magic.withdraw.reapal.recharge;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** 融宝充值主动查询调度器。 */
@Slf4j
@RequiredArgsConstructor
public class ReapalRechargePollingScheduler {

    private final ReapalRechargePollingService pollingService;
    private ScheduledExecutorService executor;

    @PostConstruct
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "reapal-recharge-polling");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::pollSafely, 1, 1, TimeUnit.SECONDS);
        log.info("融宝充值主动巡检任务已启动");
    }

    private void pollSafely() {
        try {
            pollingService.pollDueOrders();
        } catch (Exception e) {
            log.error("融宝充值主动巡检任务异常", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
