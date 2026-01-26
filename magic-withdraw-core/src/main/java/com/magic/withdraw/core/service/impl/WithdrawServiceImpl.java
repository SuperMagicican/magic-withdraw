package com.magic.withdraw.core.service.impl;

import com.magic.withdraw.core.domain.bean.CallbackConfig;
import com.magic.withdraw.core.domain.bean.ProcessingOrder;
import com.magic.withdraw.core.domain.request.QueryBalaceRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.QueryBalaceResponse;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.core.loader.MagicLoader;
import com.magic.withdraw.core.service.ProcessingOrderService;
import com.magic.withdraw.core.service.TradeService;
import com.magic.withdraw.core.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        if (callbackConfig.isEnabled()) {
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
    public QueryBalaceResponse queryBalance(QueryBalaceRequest request, String platform) {
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
