package com.magic.withdraw.reapal.recharge;

/**
 * 融宝充值成功回调。
 * 业务方可提供自定义 Bean 替换默认实现。
 */
public interface ReapalRechargeCallback {

    /** 充值成功。 */
    void successRecharge(ReapalRechargeQueryResult result);
}
