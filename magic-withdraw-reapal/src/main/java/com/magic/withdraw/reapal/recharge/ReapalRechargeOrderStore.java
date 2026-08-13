package com.magic.withdraw.reapal.recharge;

import java.util.Collection;

/**
 * 融宝充值巡检任务存储。
 * 业务方可提供自定义 Bean，将默认内存实现替换为持久化实现。
 */
public interface ReapalRechargeOrderStore {

    /** 登记一笔待主动查询的充值订单。 */
    void add(RechargePollingOrder order);

    /** 返回当前已经到达查询时间的任务。 */
    Collection<RechargePollingOrder> listDue(long currentTimeMillis);

    /** 更新下一次查询时间。 */
    void reschedule(String rechargeOrderNo, long nextQueryTimeMillis);

    /** 充值完成、失败或查询超时后移除任务。 */
    void remove(String rechargeOrderNo);

    /**
     * @param rechargeOrderNo 充值商户订单号
     * @param payoutOrderNo 代付商户订单号
     * @param nextQueryTimeMillis 下一次查询时间
     * @param expireTimeMillis 查询截止时间
     * @param queryIntervalMillis 查询间隔
     */
    record RechargePollingOrder(String rechargeOrderNo,
                                String payoutOrderNo,
                                long nextQueryTimeMillis,
                                long expireTimeMillis,
                                long queryIntervalMillis) {
    }
}
