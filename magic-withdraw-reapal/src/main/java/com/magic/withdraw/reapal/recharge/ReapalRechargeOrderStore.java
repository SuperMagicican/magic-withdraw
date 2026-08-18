package com.magic.withdraw.reapal.recharge;

import java.util.Collection;

/**
 * 融宝充值巡检任务存储。
 * 业务方可提供自定义 Bean，将默认内存实现替换为持久化实现。
 */
public interface ReapalRechargeOrderStore {

    /**
     * 登记一笔待主动查询的充值订单。
     * 同一代付订单已存在充值查询任务时，必须拒绝新任务。
     */
    void add(RechargePollingOrder order);

    /** 获取代付订单当前关联的充值任务。 */
    RechargePollingOrder getByPayoutOrderNo(String payoutOrderNo);

    /** 返回当前已经到达查询时间的任务。 */
    Collection<RechargePollingOrder> listDue(long currentTimeMillis);

    /** 更新下一次查询时间。 */
    void reschedule(String rechargeOrderNo, long nextQueryTimeMillis);

    /** 充值查询得到终态或查询超时后移除任务。 */
    void remove(String rechargeOrderNo);

    /** 按代付订单移除当前关联的充值任务。 */
    void removeByPayoutOrderNo(String payoutOrderNo);

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
