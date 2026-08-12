package com.magic.withdraw.reapal.recharge;

import com.magic.withdraw.reapal.ReapalConfig;
import com.magic.withdraw.reapal.ReapalConfig.RechargeMode;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 指定银行后直达企业网银。
 */
public class B2bDirectRechargeStrategy implements ReapalRechargeStrategy {

    private static final String PRODUCT_CODE = "O_PAY_B2B";

    @Override
    public RechargeMode mode() {
        return RechargeMode.B2B_DIRECT;
    }

    @Override
    public String productCode() {
        return PRODUCT_CODE;
    }

    @Override
    public Map<String, Object> buildExpend(ReapalConfig config) {
        Map<String, Object> expend = new LinkedHashMap<>();
        expend.put("payMethod", "directPay");
        expend.put("wgType", "BB");
        expend.put("bankNo", config.getRechargeBankNo());
        expend.put("pyeeAcctType", "20");
        return expend;
    }

    @Override
    public String validate(ReapalConfig config) {
        return StringUtils.hasText(config.getRechargeBankNo()) ? null : "充值银行编码不能为空";
    }
}
