package com.magic.withdraw.reapal.recharge;

import com.magic.withdraw.reapal.recharge.ReapalRechargeOrderStore.RechargePollingOrder;
import com.magic.withdraw.reapal.recharge.ReapalRechargeQueryResult.RechargeState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/** 主动查询融宝充值结果，充值成功后交给可扩展回调处理。 */
@Slf4j
@RequiredArgsConstructor
public class ReapalRechargePollingService {

    private final ReapalRechargeOrderStore orderStore;
    private final ReapalRechargeService rechargeService;
    private final ReapalRechargeCallback rechargeCallback;

    public void pollDueOrders() {
        long now = System.currentTimeMillis();
        Collection<RechargePollingOrder> orders = orderStore.listDue(now);
        if (orders.isEmpty()) {
            return;
        }
        orders.forEach(order -> pollOrder(order, now));
    }

    private void pollOrder(RechargePollingOrder order, long now) {
        if (now > order.expireTimeMillis()) {
            removeTimeoutOrder(order);
            return;
        }
        ReapalRechargeQueryResult result = rechargeService.query(
                order.rechargeOrderNo(), order.payoutOrderNo());
        if (!result.isQuerySuccessful()
                || result.getState() == RechargeState.UNKNOWN
                || result.getState() == RechargeState.PROCESSING) {
            rescheduleOrTimeout(order, now);
            return;
        }
        if (result.getState() == RechargeState.SUCCESS) {
            handleSuccess(order, result);
            return;
        }
        removeTerminalOrder(order, result);
    }

    private void handleSuccess(RechargePollingOrder order,
                               ReapalRechargeQueryResult result) {
        try {
            rechargeCallback.successRecharge(result);
        } catch (RuntimeException exception) {
            log.error("融宝充值成功回调失败，rechargeOrderNo={}, status={}",
                    order.rechargeOrderNo(), result.getRechargeStatus(), exception);
        } finally {
            removeTerminalOrder(order, result);
        }
    }

    private void removeTerminalOrder(RechargePollingOrder order,
                                     ReapalRechargeQueryResult result) {
        orderStore.remove(order.rechargeOrderNo());
        log.info("融宝充值巡检已移除终态订单，rechargeOrderNo={}, status={}",
                order.rechargeOrderNo(), result.getRechargeStatus());
    }

    private void rescheduleOrTimeout(RechargePollingOrder order, long now) {
        long nextQueryTime = now + order.queryIntervalMillis();
        if (nextQueryTime > order.expireTimeMillis()) {
            removeTimeoutOrder(order);
            return;
        }
        orderStore.reschedule(order.rechargeOrderNo(), nextQueryTime);
    }

    private void removeTimeoutOrder(RechargePollingOrder order) {
        orderStore.remove(order.rechargeOrderNo());
        log.warn("融宝充值查询超时，已移除巡检任务，rechargeOrderNo={}, payoutOrderNo={}",
                order.rechargeOrderNo(), order.payoutOrderNo());
    }
}
