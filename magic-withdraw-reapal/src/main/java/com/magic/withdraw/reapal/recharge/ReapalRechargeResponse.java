package com.magic.withdraw.reapal.recharge;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** 融宝关联充值发起响应。 */
@Data
@Accessors(chain = true)
public class ReapalRechargeResponse implements Serializable {

    private boolean success;
    private boolean pollingRequired;
    private String payoutOrderNo;
    private String rechargeOrderNo;
    private String rechargeOutOrderNo;
    private Long amount;
    private String rechargeStatus;
    private String paymentUrl;
    private String paymentToken;
    private String requestBody;
    private String responseBody;
    private String message;
}
