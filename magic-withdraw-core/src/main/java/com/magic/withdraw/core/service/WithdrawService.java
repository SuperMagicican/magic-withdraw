package com.magic.withdraw.core.service;

import com.magic.withdraw.core.domain.request.QueryBalaceRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.QueryBalaceResponse;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;

/**
 * @author lgy
 * @since 2026/1/13
 */
public interface WithdrawService {

    /**
     * 单笔提现
     */
    SingleWithdrawResponse singleWithdraw(SingleWithdrawRequest request, String platform);

    /**
     * 查询余额
     */
    QueryBalaceResponse queryBalance(QueryBalaceRequest request, String platform);

    /**
     * 查询订单
     */
    QueryResponse queryTradingOrderNo(String orderNo, String platform);

    /**
     * 获得openID
     */
    String getOpenid(String code, String platform);
}
