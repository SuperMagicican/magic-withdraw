package com.magic.withdraw.core.service;

import com.magic.withdraw.core.domain.request.CancelRequest;
import com.magic.withdraw.core.domain.request.QueryBalanceRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.CancelResponse;
import com.magic.withdraw.core.domain.response.QueryBalanceResponse;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;

/**
 * 交易接口
 * @author lgy
 * @since 2026/1/13
 */
public interface TradeService {

    /**
     * 单笔提现
     */
    SingleWithdrawResponse singleWithdraw(SingleWithdrawRequest request);

    /**
     * 查询余额
     */
    QueryBalanceResponse queryBalance(QueryBalanceRequest request);

    /**
     * 查询订单
     */
    QueryResponse queryTradingOrderNo(String orderNo);

    /**
     * 撤销提现
     */
    CancelResponse cancelWithdraw(CancelRequest request);

    /**
     * 获取openid
     */
    String getOpenid(String code);
}
