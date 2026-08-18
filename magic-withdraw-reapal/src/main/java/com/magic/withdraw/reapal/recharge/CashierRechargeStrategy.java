package com.magic.withdraw.reapal.recharge;

import com.magic.withdraw.reapal.ReapalConfig;
import com.magic.withdraw.reapal.ReapalConfig.RechargeMode;

import java.util.Map;

/**
 * 融宝收银台充值，由付款人在页面选择付款方式。
 */
public class CashierRechargeStrategy implements ReapalRechargeStrategy {

    private static final String PRODUCT_CODE = "CASHIER";
    private static final Map<String, Object> EXPEND = Map.of("pyeeAcctType", "20");

    @Override
    public RechargeMode mode() {
        return RechargeMode.CASHIER;
    }

    @Override
    public String productCode() {
        return PRODUCT_CODE;
    }

    @Override
    public Map<String, Object> buildExpend(ReapalConfig config) {
        return EXPEND;
    }
}
