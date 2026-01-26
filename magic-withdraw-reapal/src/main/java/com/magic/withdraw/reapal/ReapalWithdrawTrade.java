package com.magic.withdraw.reapal;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.annotation.TradePlatform;
import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.context.TradePlatformConfigContext;
import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import com.magic.withdraw.core.domain.request.QueryBalaceRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.QueryBalaceResponse;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.core.service.TradeService;
import com.reapal.api.Client;
import com.reapal.api.DefaultClient;
import com.reapal.api.ReapalConfig;
import com.reapal.api.model.DfSingleTradeResult;
import com.reapal.api.request.DfSingleTradeRequest;
import com.reapal.api.request.MemberMerchantAccountBalanceRequest;
import com.reapal.api.response.DfSingleTradeResponse;
import com.reapal.api.response.MemberMerchantAccountBalanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 融宝 实现类
 * @author lgy
 * @since 2026/1/13
 */
@Slf4j
@Service
@RequiredArgsConstructor
@TradePlatform(PlatformConstant.REAPAL)
public class ReapalWithdrawTrade implements TradeService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private final static String PAY_SIGHT_OTHER = "其他";
    private final static String ACCOUNT_TYPE_CORPORATE = "01";
    private final static String ACCOUNT_TYPE_PERSONAL = "02";

    private Client client;

    @Override
    public SingleWithdrawResponse singleWithdraw(SingleWithdrawRequest request) {
        SingleWithdrawResponse response = new SingleWithdrawResponse();
        try {
            TradePlatformConfig tradePlatformConfig = TradePlatformConfigContext.get();
            if (tradePlatformConfig instanceof com.magic.withdraw.reapal.ReapalConfig config) {
                reapalClientBulider(config);
                DfSingleTradeRequest dfSingleTradeRequest = new DfSingleTradeRequest();
                dfSingleTradeRequest.setMerchantId(config.getMerchantId());
                dfSingleTradeRequest.setCustomerId(config.getCustomerId());
                dfSingleTradeRequest.setAmount(convertBigDecimalToFenLong(request.getAmount()));
                dfSingleTradeRequest.setPaySight(PAY_SIGHT_OTHER);
                dfSingleTradeRequest.setMerchantOrderNo(request.getOrderNo());
                dfSingleTradeRequest.setAccountType(
                        Objects.equals(SingleWithdrawRequest.EnumAccountType.COMPANY.getCode(),
                                request.getAccountType()) ?
                                ACCOUNT_TYPE_CORPORATE : ACCOUNT_TYPE_PERSONAL);
                dfSingleTradeRequest.setBankNo(request.getBankNo());
                dfSingleTradeRequest.setCardNo(request.getCardNo());
                dfSingleTradeRequest.setCardName(request.getCardName());
                dfSingleTradeRequest.setNotifyUrl(request.getNotifyUrl());

                response.setRequestBody(JSON.toJSONString(dfSingleTradeRequest));
                DfSingleTradeResponse dfSingleTradeResponse = client.execute(dfSingleTradeRequest);

                log.info("融宝单笔代付响应结果：{}", dfSingleTradeResponse);

                if ("0000".equals(dfSingleTradeResponse.getCode()) && "0000".equals(dfSingleTradeResponse.getData().getResultCode())) {
                    DfSingleTradeResult dfSingleTradeResult = dfSingleTradeResponse.getData();
                    response.setSuccess(true);
                    response.setOrderNo(dfSingleTradeResult.getMerchantOrderNo());
                    response.setOutOrderNo(dfSingleTradeResult.getOrderId());
                    response.setMessage(dfSingleTradeResult.getResultMsg());
                    response.setResponseBody(JSON.toJSONString(dfSingleTradeResponse));
                } else {
                    response.setSuccess(false);
                    response.setMessage(dfSingleTradeResponse.getMsg());
                }
            } else {
                response.setSuccess(false);
                response.setMessage("reapal config is null");
            }
        } catch (Exception e) {
            log.error("融宝单笔代付异常：", e);
            response.setSuccess(false);
        }
        return response;
    }

    @Override
    public QueryBalaceResponse queryBalance(QueryBalaceRequest request) {
        QueryBalaceResponse response = new QueryBalaceResponse();
        try {
            TradePlatformConfig tradePlatformConfig = TradePlatformConfigContext.get();
            if (tradePlatformConfig instanceof com.magic.withdraw.reapal.ReapalConfig config) {
                reapalClientBulider(config);
                MemberMerchantAccountBalanceRequest balanceRequest =
                        new MemberMerchantAccountBalanceRequest();
                balanceRequest.setCustomerId(config.getCustomerId());
                balanceRequest.setMerchantId(config.getMerchantId());
                MemberMerchantAccountBalanceResponse balanceResponse = client.execute(balanceRequest);

                log.info("融宝查询余额响应结果：{}", balanceResponse);

                if ("0000".equals(balanceResponse.getCode())) {
                    response.setSuccess(true);
                    response.setAvailableBalance(balanceResponse.getData().getPaymentBalance());
                    response.setMessage(balanceResponse.getMsg());
                } else {
                    response.setSuccess(false);
                    response.setMessage(balanceResponse.getMsg());
                }
            } else {
                response.setSuccess(false);
                response.setMessage("reapal config is null");
            }
        } catch (Exception e) {
            log.error("融宝查询余额异常：", e);
            response.setSuccess(false);
        }
        return response;
    }

    @Override
    public QueryResponse queryTradingOrderNo(String orderNo) {
        return null;
    }

    @Override
    public String getOpenid(String code) {
        return "";
    }

    private void reapalClientBulider(com.magic.withdraw.reapal.ReapalConfig config) {
        ReapalConfig reapalConfig = new ReapalConfig();
        reapalConfig.setServerUrl(config.getOpenApiDomain());
        reapalConfig.setMerchantId(config.getMerchantId());
        reapalConfig.setSignType(config.getSignType());
        reapalConfig.setSignId(config.getSignId());
        reapalConfig.setReapalPublicCertPath(config.getReapalPublicKey());
        reapalConfig.setMerchantprivateCertPath(config.getPrivateKey());
        reapalConfig.setMerchantprivateCertPwd(config.getPrivateKeyPwd());
        reapalConfig.setEncryptId(config.getEncryptId());
        reapalConfig.setEncryptType(config.getEncryptType());
        client = new DefaultClient(reapalConfig);
    }

    private static Long convertBigDecimalToFenLong(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(HUNDRED).setScale(0, RoundingMode.DOWN).longValue();
    }
}
