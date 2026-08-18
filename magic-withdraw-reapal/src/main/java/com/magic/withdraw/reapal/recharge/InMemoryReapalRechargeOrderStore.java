package com.magic.withdraw.reapal.recharge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 默认的融宝充值订单内存关联存储。 */
@ConditionalOnMissingBean(ReapalRechargeOrderStore.class)
public class InMemoryReapalRechargeOrderStore implements ReapalRechargeOrderStore {

    private final ConcurrentMap<String, RechargePollingOrder> ordersByPayout =
            new ConcurrentHashMap<>();

    @Override
    public synchronized void add(RechargePollingOrder order) {
        validate(order);
        RechargePollingOrder current = ordersByPayout.get(order.payoutOrderNo());
        if (current != null) {
            throw new IllegalStateException(
                    "代付订单存在待查询的充值订单：" + current.rechargeOrderNo());
        }
        boolean usedByAnotherPayout = ordersByPayout.values().stream()
                .anyMatch(existing -> existing.rechargeOrderNo().equals(
                                order.rechargeOrderNo())
                        && !existing.payoutOrderNo().equals(order.payoutOrderNo()));
        if (usedByAnotherPayout) {
            throw new IllegalStateException(
                    "充值订单已关联其他代付订单：" + order.rechargeOrderNo());
        }
        ordersByPayout.put(order.payoutOrderNo(), order);
    }

    @Override
    public RechargePollingOrder getByPayoutOrderNo(String payoutOrderNo) {
        return ordersByPayout.get(payoutOrderNo);
    }

    @Override
    public Collection<RechargePollingOrder> listDue(long currentTimeMillis) {
        return ordersByPayout.values().stream()
                .filter(order -> order.nextQueryTimeMillis() <= currentTimeMillis)
                .toList();
    }

    @Override
    public void reschedule(String rechargeOrderNo, long nextQueryTimeMillis) {
        String payoutOrderNo = findPayoutOrderNo(rechargeOrderNo);
        if (payoutOrderNo == null) {
            return;
        }
        ordersByPayout.computeIfPresent(payoutOrderNo,
                (key, current) -> current.rechargeOrderNo().equals(rechargeOrderNo)
                        ? new RechargePollingOrder(
                                current.rechargeOrderNo(), current.payoutOrderNo(),
                                nextQueryTimeMillis, current.expireTimeMillis(),
                                current.queryIntervalMillis())
                        : current);
    }

    @Override
    public void remove(String rechargeOrderNo) {
        String payoutOrderNo = findPayoutOrderNo(rechargeOrderNo);
        if (payoutOrderNo != null) {
            ordersByPayout.computeIfPresent(payoutOrderNo, (key, current) ->
                    current.rechargeOrderNo().equals(rechargeOrderNo) ? null : current);
        }
    }

    @Override
    public void removeByPayoutOrderNo(String payoutOrderNo) {
        ordersByPayout.remove(payoutOrderNo);
    }

    private String findPayoutOrderNo(String rechargeOrderNo) {
        if (rechargeOrderNo == null) {
            return null;
        }
        return ordersByPayout.entrySet().stream()
                .filter(entry -> rechargeOrderNo.equals(entry.getValue().rechargeOrderNo()))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private void validate(RechargePollingOrder order) {
        if (order == null || order.rechargeOrderNo() == null
                || order.rechargeOrderNo().isBlank()
                || order.payoutOrderNo() == null
                || order.payoutOrderNo().isBlank()) {
            throw new IllegalArgumentException("充值订单号和代付订单号不能为空");
        }
    }
}
