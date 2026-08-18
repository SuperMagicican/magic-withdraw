package com.magic.withdraw.reapal.recharge;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** 融宝关联充值发起请求。 */
@Data
@Accessors(chain = true)
public class ReapalRechargeRequest implements Serializable {

    /** 充值商户订单号。 */
    private String rechargeOrderNo;

    /** 关联的代付商户订单号。 */
    private String payoutOrderNo;

    /** 融宝代付订单号，用于关联充值。 */
    private String payoutOutOrderNo;

    /** 充值金额，单位：分。 */
    private Long amount;

    /** 充值订单标题。 */
    private String orderTitle;
}
