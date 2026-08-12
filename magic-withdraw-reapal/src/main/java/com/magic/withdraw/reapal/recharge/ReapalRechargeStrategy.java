package com.magic.withdraw.reapal.recharge;

import com.magic.withdraw.reapal.ReapalConfig;
import com.magic.withdraw.reapal.ReapalConfig.RechargeMode;

import java.util.Map;

/**
 * 融宝充值模式策略，仅负责模式差异参数。
 */
public interface ReapalRechargeStrategy {

    RechargeMode mode();

    String productCode();

    Map<String, Object> buildExpend(ReapalConfig config);

    default String validate(ReapalConfig config) {
        return null;
    }
}
