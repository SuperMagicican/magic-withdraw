package com.magic.withdraw.reapal;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.annotation.TradePlatform;
import com.magic.withdraw.core.constants.OrderStatusConstant;
import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import com.magic.withdraw.core.domain.request.CancelRequest;
import com.magic.withdraw.core.domain.request.QueryBalanceRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.CancelResponse;
import com.magic.withdraw.core.domain.response.QueryBalanceResponse;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.core.service.PlatformConfigService;
import com.magic.withdraw.core.service.TradeService;
import com.reapal.api.Client;
import com.reapal.api.DefaultClient;
import com.reapal.api.ReapalConfig;
import com.reapal.api.model.DfTradeSubResult;
import com.reapal.api.model.DfSingleTradeResult;
import com.reapal.api.request.DfTradeQueryRequest;
import com.reapal.api.request.DfSingleTradeRequest;
import com.reapal.api.request.FastCardQueryRequest;
import com.reapal.api.request.MemberMerchantAccountBalanceRequest;
import com.reapal.api.response.DfTradeQueryResponse;
import com.reapal.api.response.DfSingleTradeResponse;
import com.reapal.api.response.FastCardQueryResponse;
import com.reapal.api.response.MemberMerchantAccountBalanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
    private final static String PAY_SIGHT_OTHER = "51";
    private final static String ACCOUNT_TYPE_CORPORATE = "01";
    private final static String ACCOUNT_TYPE_PERSONAL = "02";
    private final static String REAPAL_SUCCESS_CODE = "0000";
    private final static String REAPAL_STATUS_REJECTED = "4";
    private final static String REAPAL_STATUS_PROCESSING = "5";
    private final static String REAPAL_STATUS_SUCCESS = "6";
    private final static String REAPAL_STATUS_FAIL = "7";
    private final static String REAPAL_STATUS_SERVICE_REJECTED = "10";

    private final PlatformConfigService platformConfigService;
    private Client client;

    @Override
    public SingleWithdrawResponse singleWithdraw(SingleWithdrawRequest request) {
        SingleWithdrawResponse response = new SingleWithdrawResponse();
        try {
            TradePlatformConfig tradePlatformConfig = platformConfigService.get(PlatformConstant.REAPAL);
            if (tradePlatformConfig instanceof com.magic.withdraw.reapal.ReapalConfig config) {
                reapalClientBuilder(config);
                if (!StringUtils.hasText(request.getBankNo())) {
                    if (!StringUtils.hasText(request.getCardNo())) {
                        response.setSuccess(false);
                        response.setMessage("银行卡号不能为空，无法查询银行编号");
                        return response;
                    }
                    FastCardQueryResponse cardQueryResponse = queryCardBin(config, request.getCardNo());
                    response.setResponseBody(JSON.toJSONString(cardQueryResponse));
                    if (Objects.isNull(cardQueryResponse)
                            || !REAPAL_SUCCESS_CODE.equals(cardQueryResponse.getCode())
                            || Objects.isNull(cardQueryResponse.getData())
                            || !StringUtils.hasText(cardQueryResponse.getData().getBankNo())) {
                        response.setSuccess(false);
                        response.setMessage(Objects.nonNull(cardQueryResponse) && Objects.nonNull(cardQueryResponse.getMsg())
                                ? cardQueryResponse.getMsg()
                                : "融宝卡BIN查询未返回银行编号");
                        return response;
                    }
                    request.setBankNo(cardQueryResponse.getData().getBankNo());
                }

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
                response.setResponseBody(JSON.toJSONString(dfSingleTradeResponse));

                log.info("融宝单笔代付响应结果：{}", dfSingleTradeResponse);

                if (REAPAL_SUCCESS_CODE.equals(dfSingleTradeResponse.getCode())
                        && Objects.nonNull(dfSingleTradeResponse.getData())
                        && REAPAL_SUCCESS_CODE.equals(dfSingleTradeResponse.getData().getResultCode())) {
                    DfSingleTradeResult dfSingleTradeResult = dfSingleTradeResponse.getData();
                    response.setSuccess(true);
                    response.setOrderNo(dfSingleTradeResult.getMerchantOrderNo());
                    response.setOutOrderNo(dfSingleTradeResult.getOrderId());
                    response.setMessage(dfSingleTradeResult.getResultMsg());
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

    private FastCardQueryResponse queryCardBin(com.magic.withdraw.reapal.ReapalConfig config, String cardNo) throws Exception {
        FastCardQueryRequest cardQueryRequest = new FastCardQueryRequest();
        cardQueryRequest.setMerchantId(config.getMerchantId());
        cardQueryRequest.setCardNo(cardNo);
        FastCardQueryResponse cardQueryResponse = client.execute(cardQueryRequest);
        log.info("融宝卡BIN查询响应结果：{}", cardQueryResponse);
        return cardQueryResponse;
    }

    @Override
    public QueryBalanceResponse queryBalance(QueryBalanceRequest request) {
        QueryBalanceResponse response = new QueryBalanceResponse();
        try {
            TradePlatformConfig tradePlatformConfig = platformConfigService.get(PlatformConstant.REAPAL);
            if (tradePlatformConfig instanceof com.magic.withdraw.reapal.ReapalConfig config) {
                reapalClientBuilder(config);
                MemberMerchantAccountBalanceRequest balanceRequest =
                        new MemberMerchantAccountBalanceRequest();
                balanceRequest.setCustomerId(config.getCustomerId());
                balanceRequest.setMerchantId(config.getMerchantId());
                MemberMerchantAccountBalanceResponse balanceResponse = client.execute(balanceRequest);

                log.info("融宝查询余额响应结果：{}", balanceResponse);

                if (REAPAL_SUCCESS_CODE.equals(balanceResponse.getCode())) {
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
        QueryResponse response = new QueryResponse();
        try {
            TradePlatformConfig tradePlatformConfig = platformConfigService.get(PlatformConstant.REAPAL);
            if (tradePlatformConfig instanceof com.magic.withdraw.reapal.ReapalConfig config) {
                reapalClientBuilder(config);
                DfTradeQueryRequest queryRequest = new DfTradeQueryRequest();
                queryRequest.setMerchantId(config.getMerchantId());
                queryRequest.setCustomerId(config.getCustomerId());
                queryRequest.setMerchantOrderNo(orderNo);

                DfTradeQueryResponse queryResponse = client.execute(queryRequest);
                log.info("融宝代付查询响应结果：{}", queryResponse);
                response.setResponseBody(JSON.toJSONString(queryResponse));

                if (!REAPAL_SUCCESS_CODE.equals(queryResponse.getCode())) {
                    response.setSuccess(false);
                    response.setMessage(queryResponse.getMsg());
                    return response;
                }
                if (Objects.isNull(queryResponse.getData())) {
                    response.setSuccess(false);
                    response.setMessage("融宝代付查询响应数据为空");
                    return response;
                }
                if (!REAPAL_SUCCESS_CODE.equals(queryResponse.getData().getResultCode())) {
                    response.setSuccess(false);
                    response.setMessage(queryResponse.getData().getResultMsg());
                    return response;
                }

                DfTradeSubResult detail = findTradeDetail(queryResponse.getData().getDetails(), orderNo);
                if (Objects.isNull(detail)) {
                    response.setSuccess(false);
                    response.setMessage("融宝代付查询响应明细为空");
                    return response;
                }

                response.setSuccess(true);
                response.setOrderStatus(convertReapalStatus(detail.getStatus()));
                response.setFailReason(detail.getResultMsg());
                response.setMessage(detail.getResultMsg());
            } else {
                response.setSuccess(false);
                response.setMessage("reapal config is null");
            }
        } catch (Exception e) {
            log.error("融宝代付查询异常：", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    @Override
    public CancelResponse cancelWithdraw(CancelRequest request) {
        return null;
    }

    @Override
    public String getOpenid(String code) {
        return "";
    }

    private void reapalClientBuilder(com.magic.withdraw.reapal.ReapalConfig config) {
        ReapalConfig reapalConfig = new ReapalConfig();
        reapalConfig.setServerUrl(normalizeOpenApiDomain(config.getOpenApiDomain()));
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

    private static DfTradeSubResult findTradeDetail(List<DfTradeSubResult> details, String orderNo) {
        if (Objects.isNull(details) || details.isEmpty()) {
            return null;
        }
        return details.stream()
                .filter(detail -> Objects.equals(orderNo, detail.getMerchantOrderNo()))
                .findFirst()
                .orElse(null);
    }

    private static String convertReapalStatus(String status) {
        if (REAPAL_STATUS_SUCCESS.equals(status)) {
            return OrderStatusConstant.SUCCESS;
        }
        if (REAPAL_STATUS_FAIL.equals(status)
                || REAPAL_STATUS_REJECTED.equals(status)
                || REAPAL_STATUS_SERVICE_REJECTED.equals(status)) {
            return OrderStatusConstant.FAIL;
        }
        if (REAPAL_STATUS_PROCESSING.equals(status)) {
            return OrderStatusConstant.PROCESSING;
        }
        return OrderStatusConstant.PROCESSING;
    }

    private static String normalizeOpenApiDomain(String openApiDomain) {
        if (Objects.isNull(openApiDomain)) {
            return null;
        }
        return openApiDomain
                .replace("/dforder/df/singleTrade", "")
                .replace("/dforder/df/batchTrade", "")
                .replace("/dforder/df/query", "")
                .replace("/member/merchant/account/balance", "")
                .replaceAll("/+$", "");
    }

    private static Long convertBigDecimalToFenLong(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(HUNDRED).setScale(0, RoundingMode.DOWN).longValue();
    }
}
