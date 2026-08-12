package com.magic.withdraw.core.service.impl;

import com.magic.withdraw.core.domain.bean.CallbackConfig;
import com.magic.withdraw.core.domain.bean.ProcessingOrder;
import com.magic.withdraw.core.domain.request.CancelRequest;
import com.magic.withdraw.core.domain.request.QueryBalanceRequest;
import com.magic.withdraw.core.domain.request.QueryBillRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.CancelResponse;
import com.magic.withdraw.core.domain.response.QueryBalanceResponse;
import com.magic.withdraw.core.domain.response.QueryBillResponse;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.core.loader.MagicLoader;
import com.magic.withdraw.core.service.ProcessingOrderService;
import com.magic.withdraw.core.service.TradeService;
import com.magic.withdraw.core.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 提现实现类
 *
 * @author lgy
 * @since 2026/1/13
 */
@Service
@RequiredArgsConstructor
public class WithdrawServiceImpl implements WithdrawService {

    private final CallbackConfig callbackConfig;
    private final ProcessingOrderService processingOrderService;

    /**
     * 单笔提现
     */
    @Override
    public SingleWithdrawResponse singleWithdraw(SingleWithdrawRequest request, String platform) {

        SingleWithdrawResponse singleWithdrawResponse = getPlatFormService(platform).singleWithdraw(request);

        if (callbackConfig.isEnabled()
                && singleWithdrawResponse.isPollingRequired()
                && StringUtils.hasText(singleWithdrawResponse.getOrderNo())) {
            ProcessingOrder processingOrder = new ProcessingOrder();
            processingOrder.setOrderNo(singleWithdrawResponse.getOrderNo());
            processingOrder.setPlatform(platform);
            processingOrderService.add(processingOrder);
        }

        return singleWithdrawResponse;
    }

    /**
     * 查询余额
     */
    @Override
    public QueryBalanceResponse queryBalance(QueryBalanceRequest request, String platform) {
        return getPlatFormService(platform).queryBalance(request);
    }

    /**
     * 查询订单
     */
    @Override
    public QueryResponse queryTradingOrderNo(String orderNo, String platform) {
        return getPlatFormService(platform).queryTradingOrderNo(orderNo);
    }

    /**
     * 查询账单
     */
    @Override
    public QueryBillResponse queryBill(QueryBillRequest request, String platform) {
        return getPlatFormService(platform).queryBill(request);
    }

    /**
     * 撤销提现
     */
    @Override
    public CancelResponse cancelWithdraw(CancelRequest request, String platform) {
        return getPlatFormService(platform).cancelWithdraw(request);
    }

    /**
     * 获取opneid
     */
    @Override
    public String getOpenid(String code, String platform) {
        return getPlatFormService(platform).getOpenid(code);
    }

    /**
     * 根据平台名称获取实现类的方法
     * @param platForm 平台
     * @return 结果
     */
    private TradeService getPlatFormService(String platForm) {
        return MagicLoader.getTradeService(platForm);
    }
}
