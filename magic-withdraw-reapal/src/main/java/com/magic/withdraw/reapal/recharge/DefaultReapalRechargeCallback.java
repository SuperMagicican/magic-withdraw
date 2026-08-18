package com.magic.withdraw.reapal.recharge;

import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.domain.bean.ProcessingOrder;
import com.magic.withdraw.core.service.ProcessingOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.util.StringUtils;

/** 融宝充值成功的默认处理。 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnMissingBean(ReapalRechargeCallback.class)
public class DefaultReapalRechargeCallback implements ReapalRechargeCallback {

    private final ProcessingOrderService processingOrderService;

    @Override
    public void successRecharge(ReapalRechargeQueryResult result) {
        if (!StringUtils.hasText(result.getPayoutOrderNo())) {
            log.warn("融宝充值成功但未关联代付订单，rechargeOrderNo={}",
                    result.getRechargeOrderNo());
            return;
        }
        ProcessingOrder processingOrder = new ProcessingOrder();
        processingOrder.setOrderNo(result.getPayoutOrderNo());
        processingOrder.setPlatform(PlatformConstant.REAPAL);
        processingOrderService.add(processingOrder);
        log.info("融宝充值成功，代付订单已加入结果巡检，rechargeOrderNo={}, payoutOrderNo={}",
                result.getRechargeOrderNo(), result.getPayoutOrderNo());
    }
}
