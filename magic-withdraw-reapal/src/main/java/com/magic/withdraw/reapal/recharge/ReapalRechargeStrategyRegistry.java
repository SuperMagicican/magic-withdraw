package com.magic.withdraw.reapal.recharge;

import com.magic.withdraw.reapal.ReapalConfig.RechargeMode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 融宝充值策略注册器。
 */
public class ReapalRechargeStrategyRegistry {

    private final Map<RechargeMode, ReapalRechargeStrategy> strategies = new EnumMap<>(RechargeMode.class);

    public ReapalRechargeStrategyRegistry(List<ReapalRechargeStrategy> strategies) {
        for (ReapalRechargeStrategy strategy : strategies) {
            ReapalRechargeStrategy previous = this.strategies.put(strategy.mode(), strategy);
            if (previous != null) {
                throw new IllegalStateException("重复的融宝充值策略：" + strategy.mode());
            }
        }
    }

    public ReapalRechargeStrategy get(RechargeMode mode) {
        RechargeMode effectiveMode = mode == null ? RechargeMode.B2B_DIRECT : mode;
        ReapalRechargeStrategy strategy = strategies.get(effectiveMode);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的融宝充值模式：" + effectiveMode);
        }
        return strategy;
    }
}
