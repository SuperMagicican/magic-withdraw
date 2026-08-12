package com.magic.withdraw.reapal.recharge;

/**
 * 充值订单与代付订单的关联存储。
 * 业务方可提供自定义 Bean，将默认内存实现替换为持久化实现。
 */
public interface ReapalRechargeOrderStore {

    /** 保存充值商户订单号与代付商户订单号的关系。 */
    void save(String rechargeOrderNo, String payoutOrderNo);

    /** 原子领取一笔尚未处理的充值通知。 */
    Claim claim(String rechargeOrderNo);

    /** 业务处理失败时释放领取状态，以便融宝重试通知。 */
    void release(String rechargeOrderNo);

    /** 明确拒绝的充值订单不再保留映射。 */
    void remove(String rechargeOrderNo);

    enum ClaimStatus {
        CLAIMED,
        ALREADY_HANDLED,
        NOT_FOUND
    }

    record Claim(ClaimStatus status, String payoutOrderNo) {
    }
}
