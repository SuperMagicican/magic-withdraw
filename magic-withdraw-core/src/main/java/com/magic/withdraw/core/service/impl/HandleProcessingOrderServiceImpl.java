package com.magic.withdraw.core.service.impl;

import com.magic.withdraw.core.callback.CallBackService;
import com.magic.withdraw.core.domain.bean.CallbackConfig;
import com.magic.withdraw.core.domain.bean.ProcessingOrder;
import com.magic.withdraw.core.domain.bean.WithdrawResult;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.service.ProcessingOrderService;
import com.magic.withdraw.core.service.WithdrawService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author lgy
 * @since 2026/1/22
 */
@Component
@ConditionalOnProperty(prefix = "magic.withdraw.callback",name = "watch",havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class HandleProcessingOrderServiceImpl {

    private final CallbackConfig callbackConfig;

    private final ProcessingOrderService processingOrderService;
    private final WithdrawService withdrawService;
    private final CallBackService callBackService;

    @PostConstruct
    public void handleProcessingOrder() {
        if (callbackConfig.getCycle() <= 0) {
            return;
        }
        log.info("开启提现结果定期巡检任务，周期：{}秒", callbackConfig.getCycle());
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    Collection<ProcessingOrder> processingOrders = processingOrderService.list();
                    if (CollectionUtils.isEmpty(processingOrders)) {
                        return;
                    }
                    for (ProcessingOrder processingOrder : processingOrders) {
                        QueryResponse response = withdrawService.queryTradingOrderNo(
                                processingOrder.getOrderNo(),
                                processingOrder.getPlatform());

                        if (Objects.equals("SUCCESS", response.getOrderStatus())) {
                            callBackService.successWithdraw(
                                    new WithdrawResult().setQueryBody(response.getResponseBody())
                                            .setOrderNo(processingOrder.getOrderNo()));
                            processingOrderService.remove(processingOrder);
                        } else if (Objects.equals("FAIL", response.getOrderStatus()) ||
                                    Objects.equals("REFUND", response.getOrderStatus())) {
                            callBackService.failWithdraw(
                                    new WithdrawResult().setQueryBody(response.getResponseBody())
                                            .setOrderNo(processingOrder.getOrderNo())
                                            .setFailReason(response.getFailReason())
                            );
                            processingOrderService.remove(processingOrder);
                        }
                    }
                } catch (Exception e) {
                    log.error("提现结果定期巡检任务异常",e);
                }
            }
        };
        timer.schedule(task, callbackConfig.getCycle() * 1000, callbackConfig.getCycle() * 1000);
    }
}
