package com.magic.withdraw.reapal.recharge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/** 默认的融宝充值订单内存关联存储。 */
@ConditionalOnMissingBean(ReapalRechargeOrderStore.class)
public class InMemoryReapalRechargeOrderStore implements ReapalRechargeOrderStore {

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void save(String rechargeOrderNo, String payoutOrderNo) {
        entries.compute(rechargeOrderNo, (key, current) -> {
            if (current != null && !Objects.equals(current.payoutOrderNo(), payoutOrderNo)) {
                throw new IllegalStateException("充值订单已关联其他代付订单：" + rechargeOrderNo);
            }
            return current == null ? new Entry(payoutOrderNo, false) : current;
        });
    }

    @Override
    public Claim claim(String rechargeOrderNo) {
        AtomicReference<Claim> result = new AtomicReference<>();
        entries.compute(rechargeOrderNo, (key, current) -> {
            if (current == null) {
                result.set(new Claim(ClaimStatus.NOT_FOUND, null));
                return null;
            }
            if (current.handled()) {
                result.set(new Claim(ClaimStatus.ALREADY_HANDLED, current.payoutOrderNo()));
                return current;
            }
            result.set(new Claim(ClaimStatus.CLAIMED, current.payoutOrderNo()));
            return new Entry(current.payoutOrderNo(), true);
        });
        return result.get();
    }

    @Override
    public void release(String rechargeOrderNo) {
        entries.computeIfPresent(rechargeOrderNo,
                (key, current) -> new Entry(current.payoutOrderNo(), false));
    }

    @Override
    public void remove(String rechargeOrderNo) {
        entries.remove(rechargeOrderNo);
    }

    private record Entry(String payoutOrderNo, boolean handled) {
    }
}
