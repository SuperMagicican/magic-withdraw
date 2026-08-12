package com.magic.withdraw.reapal.recharge;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.domain.bean.CallbackConfig;
import com.magic.withdraw.core.domain.bean.ProcessingOrder;
import com.magic.withdraw.core.service.ProcessingOrderService;
import com.magic.withdraw.reapal.recharge.ReapalRechargeOrderStore.Claim;
import com.magic.withdraw.reapal.recharge.ReapalRechargeOrderStore.ClaimStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/** 处理融宝充值结果通知，并在充值完成后启动代付巡检。 */
@RequiredArgsConstructor
public class ReapalRechargeNotificationHandler {

    private static final String RECHARGE = "RECHARGE";
    private static final String COMPLETED = "completed";
    private static final String FAILED = "failed";

    private final ReapalRechargeOrderStore orderStore;
    private final ProcessingOrderService processingOrderService;
    private final CallbackConfig callbackConfig;

    /**
     * @return true 表示通知已完成业务处理，可以向融宝确认 success
     */
    public boolean handle(String body) {
        JSONObject notification = JSON.parseObject(body);
        String rechargeOrderNo = notification.getString("merchantOrderNo");
        String status = notification.getString("status");
        String businessCode = notification.getString("businessCode");
        if (!StringUtils.hasText(rechargeOrderNo)
                || (!COMPLETED.equals(status) && !FAILED.equals(status))
                || (StringUtils.hasText(businessCode) && !RECHARGE.equals(businessCode))) {
            return false;
        }

        Claim claim = orderStore.claim(rechargeOrderNo);
        if (FAILED.equals(status)) {
            return true;
        }
        if (claim.status() == ClaimStatus.NOT_FOUND) {
            return false;
        }
        if (claim.status() == ClaimStatus.ALREADY_HANDLED) {
            return true;
        }
        try {
            if (COMPLETED.equals(status) && callbackConfig.isEnabled()) {
                ProcessingOrder processingOrder = new ProcessingOrder();
                processingOrder.setOrderNo(claim.payoutOrderNo());
                processingOrder.setPlatform(PlatformConstant.REAPAL);
                processingOrderService.add(processingOrder);
            }
            return true;
        } catch (RuntimeException e) {
            orderStore.release(rechargeOrderNo);
            throw e;
        }
    }
}
