package com.magic.withdraw.reapal.recharge;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** 融宝充值订单查询结果。 */
@Data
@Accessors(chain = true)
public class ReapalRechargeQueryResult implements Serializable {

    private boolean querySuccessful;
    private RechargeState state = RechargeState.UNKNOWN;
    private String payoutOrderNo;
    private String rechargeOrderNo;
    private String rechargeOutOrderNo;
    private Long amount;
    private String rechargeStatus;
    private String responseBody;
    private String message;

    public enum RechargeState {
        PROCESSING,
        SUCCESS,
        FAILED,
        UNKNOWN
    }
}
