package com.magic.withdraw.reapal.recharge;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.domain.bean.ProcessingOrder;
import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import com.magic.withdraw.core.service.PlatformConfigService;
import com.magic.withdraw.core.service.ProcessingOrderService;
import com.magic.withdraw.reapal.ReapalClientFactory;
import com.magic.withdraw.reapal.ReapalConfig;
import com.magic.withdraw.reapal.recharge.ReapalRechargeOrderStore.RechargePollingOrder;
import com.reapal.api.Client;
import com.reapal.api.request.OrderQueryRequest;
import com.reapal.api.response.OrderQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Set;

/** 主动查询融宝充值结果，并在充值完成后启动代付巡检。 */
@Slf4j
@RequiredArgsConstructor
public class ReapalRechargePollingService {

    private static final String SUCCESS_CODE = "0000";
    private static final String COMPLETED = "completed";
    private static final Set<String> FAILED_STATUSES = Set.of("failed", "closed");

    private final ReapalRechargeOrderStore orderStore;
    private final ProcessingOrderService processingOrderService;
    private final PlatformConfigService platformConfigService;
    private final ReapalClientFactory clientFactory;

    public void pollDueOrders() {
        long now = System.currentTimeMillis();
        Collection<RechargePollingOrder> orders = orderStore.listDue(now);
        if (orders.isEmpty()) {
            return;
        }
        try {
            Client client = clientFactory.create(getConfig());
            orders.forEach(order -> pollOrder(client, order, now));
        } catch (Exception e) {
            log.error("融宝充值巡检初始化失败", e);
            orders.forEach(order -> rescheduleOrExpire(order, now));
        }
    }

    private void pollOrder(Client client, RechargePollingOrder order, long now) {
        if (now > order.expireTimeMillis()) {
            expire(order);
            return;
        }
        try {
            OrderQueryRequest request = new OrderQueryRequest();
            request.setMerchantId(getConfig().getMerchantId());
            request.setMerchantOrderNo(order.rechargeOrderNo());
            OrderQueryResponse response = client.execute(request);
            log.info("融宝充值巡检响应，rechargeOrderNo={}, response={}",
                    order.rechargeOrderNo(), JSON.toJSONString(response));
            handleResult(order, response, now);
        } catch (Exception e) {
            log.warn("融宝充值巡检查询异常，rechargeOrderNo={}", order.rechargeOrderNo(), e);
            rescheduleOrExpire(order, now);
        }
    }

    private void handleResult(RechargePollingOrder order, OrderQueryResponse response, long now) {
        if (response == null || !SUCCESS_CODE.equals(response.getCode()) || response.getData() == null) {
            rescheduleOrExpire(order, now);
            return;
        }
        String status = response.getData().getOrdersts();
        if (COMPLETED.equals(status) || FAILED_STATUSES.contains(status)) {
            addPayoutPolling(order);
            orderStore.remove(order.rechargeOrderNo());
            log.info("融宝充值已结束，代付订单进入结果巡检，rechargeOrderNo={}, status={}",
                    order.rechargeOrderNo(), status);
            return;
        }
        rescheduleOrExpire(order, now);
    }

    private void addPayoutPolling(RechargePollingOrder order) {
        ProcessingOrder processingOrder = new ProcessingOrder();
        processingOrder.setOrderNo(order.payoutOrderNo());
        processingOrder.setPlatform(PlatformConstant.REAPAL);
        processingOrderService.add(processingOrder);
        log.info("融宝代付订单已加入结果巡检，payoutOrderNo={}", order.payoutOrderNo());
    }

    private void rescheduleOrExpire(RechargePollingOrder order, long now) {
        long nextQueryTime = now + order.queryIntervalMillis();
        if (nextQueryTime > order.expireTimeMillis()) {
            expire(order);
            return;
        }
        orderStore.reschedule(order.rechargeOrderNo(), nextQueryTime);
    }

    private void expire(RechargePollingOrder order) {
        orderStore.remove(order.rechargeOrderNo());
        log.warn("融宝充值巡检超时，rechargeOrderNo={}, payoutOrderNo={}",
                order.rechargeOrderNo(), order.payoutOrderNo());
    }

    private ReapalConfig getConfig() {
        TradePlatformConfig config = platformConfigService.get(PlatformConstant.REAPAL);
        if (config instanceof ReapalConfig reapalConfig) {
            return reapalConfig;
        }
        throw new IllegalStateException("reapal config is null");
    }
}
