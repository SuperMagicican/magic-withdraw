package com.magic.withdraw.core.service;

import com.magic.withdraw.core.domain.request.CancelRequest;
import com.magic.withdraw.core.domain.request.QueryBalanceRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.CancelResponse;
import com.magic.withdraw.core.domain.response.QueryBalanceResponse;
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
    QueryBalanceResponse queryBalance(QueryBalanceRequest request, String platform);

    /**
     * 查询订单
     */
    QueryResponse queryTradingOrderNo(String orderNo, String platform);

    /**
     * 撤销提现
     */
    CancelResponse cancelWithdraw(CancelRequest request, String platform);

    /**
     * 获得openID
     */
    String getOpenid(String code, String platform);
}
