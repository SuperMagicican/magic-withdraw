package com.magic.withdraw.reapal.recharge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 默认的融宝充值订单内存关联存储。 */
@ConditionalOnMissingBean(ReapalRechargeOrderStore.class)
public class InMemoryReapalRechargeOrderStore implements ReapalRechargeOrderStore {

    private final ConcurrentMap<String, RechargePollingOrder> orders = new ConcurrentHashMap<>();

    @Override
    public void add(RechargePollingOrder order) {
        orders.compute(order.rechargeOrderNo(), (key, current) -> {
            if (current != null && !Objects.equals(current.payoutOrderNo(), order.payoutOrderNo())) {
                throw new IllegalStateException("充值订单已关联其他代付订单：" + order.rechargeOrderNo());
            }
            return current == null ? order : current;
        });
    }

    @Override
    public Collection<RechargePollingOrder> listDue(long currentTimeMillis) {
        return orders.values().stream()
                .filter(order -> order.nextQueryTimeMillis() <= currentTimeMillis)
                .toList();
    }

    @Override
    public void reschedule(String rechargeOrderNo, long nextQueryTimeMillis) {
        orders.computeIfPresent(rechargeOrderNo, (key, current) -> new RechargePollingOrder(
                current.rechargeOrderNo(), current.payoutOrderNo(), nextQueryTimeMillis,
                current.expireTimeMillis(), current.queryIntervalMillis()));
    }

    @Override
    public void remove(String rechargeOrderNo) {
        orders.remove(rechargeOrderNo);
    }
}
