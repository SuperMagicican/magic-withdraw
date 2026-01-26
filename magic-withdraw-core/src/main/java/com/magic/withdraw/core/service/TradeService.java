package com.magic.withdraw.core.service;

import com.magic.withdraw.core.domain.request.QueryBalaceRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.QueryBalaceResponse;
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
    QueryBalaceResponse queryBalance(QueryBalaceRequest request);

    /**
     * 查询订单
     */
    QueryResponse queryTradingOrderNo(String orderNo);

    /**
     * 获取openid
     */
    String getOpenid(String code);
}
