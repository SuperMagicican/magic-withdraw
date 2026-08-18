package com.magic.withdraw.reapal;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.annotation.TradePlatform;
import com.magic.withdraw.core.constants.OrderStatusConstant;
import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import com.magic.withdraw.core.domain.request.CancelRequest;
import com.magic.withdraw.core.domain.request.QueryBalanceRequest;
import com.magic.withdraw.core.domain.request.QueryBillRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.CancelResponse;
import com.magic.withdraw.core.domain.response.QueryBalanceResponse;
import com.magic.withdraw.core.domain.response.QueryBillResponse;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.reapal.ReapalSingleWithdrawData.SubmitStage;
import com.magic.withdraw.core.service.PlatformConfigService;
import com.magic.withdraw.core.service.TradeService;
import com.magic.withdraw.reapal.recharge.ReapalRechargeService;
import com.reapal.api.Client;
import com.reapal.api.model.DfSingleTradeResult;
import com.reapal.api.model.DfTradeSubResult;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 融宝实现类。
 *
 * @author lgy
 * @since 2026/1/13
 */
@Slf4j
@RequiredArgsConstructor
@TradePlatform(PlatformConstant.REAPAL)
public class ReapalWithdrawTrade implements TradeService {

    private static final String PAY_SIGHT_OTHER = "51";
    private static final String ACCOUNT_TYPE_CORPORATE = "01";
    private static final String ACCOUNT_TYPE_PERSONAL = "02";
    private static final String REAPAL_SUCCESS_CODE = "0000";
    private static final String PAYOUT_WAIT_RECHARGE_CODE = "2000";
    private static final String PAYOUT_PRODUCT_CODE = "OR_PAY";
    private static final String PAYOUT_BUSINESS_CODE = "PAYMENT";
    private static final String STANDARD_PAYOUT_TRADE_TYPE = "1";
    private static final String ONLINE_BANK_RECHARGE_TYPE = "O_PAY";
    private static final String REAPAL_STATUS_REJECTED = "4";
    private static final String REAPAL_STATUS_PROCESSING = "5";
    private static final String REAPAL_STATUS_SUCCESS = "6";
    private static final String REAPAL_STATUS_FAIL = "7";
    private static final String REAPAL_STATUS_SERVICE_REJECTED = "10";
    private static final String REAPAL_STATUS_CLOSED = "11";
    private static final String REAPAL_STATUS_WAIT_RECHARGE = "12";
    private final PlatformConfigService platformConfigService;
    private final ReapalRechargeService rechargeService;
    private final ReapalClientFactory clientFactory;

    @Override
    public SingleWithdrawResponse singleWithdraw(SingleWithdrawRequest request) {
        SingleWithdrawResponse response = new SingleWithdrawResponse();
        response.setPollingRequired(false);
        ReapalSingleWithdrawData platformData = new ReapalSingleWithdrawData();
        response.setPlatformData(platformData);
        String validationMessage = validateRequest(request);
        if (validationMessage != null) {
            platformData.setSubmitStage(SubmitStage.VALIDATION_FAILED);
            response.setMessage(validationMessage);
            return response;
        }

        platformData.setRechargeOrderNo(request.getRechargeOrderNo());
        try {
            ReapalConfig config = getConfig();
            String configValidationMessage = rechargeService.validate(config);
            if (configValidationMessage != null) {
                platformData.setSubmitStage(SubmitStage.VALIDATION_FAILED);
                response.setMessage(configValidationMessage);
                return response;
            }
            Client client = buildClient(config);
            String bankNo = resolvePayoutBankNo(client, config, request, response);
            if (!StringUtils.hasText(bankNo)) {
                platformData.setSubmitStage(SubmitStage.PAYOUT_REJECTED);
                return response;
            }

            DfSingleTradeRequest payoutRequest = buildPayoutRequest(config, request, bankNo);
            response.setRequestBody(JSON.toJSONString(payoutRequest));
            DfSingleTradeResponse payoutResponse = client.execute(payoutRequest);
            response.setResponseBody(JSON.toJSONString(payoutResponse));
            log.info("融宝标准订单代付响应结果：{}", response.getResponseBody());

            if (!isPayoutAccepted(payoutResponse)) {
                platformData.setSubmitStage(SubmitStage.PAYOUT_REJECTED);
                response.setMessage(resolvePayoutMessage(payoutResponse));
                return response;
            }

            DfSingleTradeResult payoutResult = payoutResponse.getData();
            populatePayoutResponse(response, platformData, payoutResult);
            return rechargeService.submitForWithdraw(
                    client, config, request, response, platformData, payoutResult);
        } catch (Exception e) {
            log.error("融宝订单代付提交异常", e);
            response.setSuccess(false);
            if (platformData.getSubmitStage() == SubmitStage.PAYOUT_ACCEPTED) {
                platformData.setSubmitStage(SubmitStage.RECHARGE_UNKNOWN);
            } else if (platformData.getSubmitStage() == null) {
                platformData.setSubmitStage(SubmitStage.PAYOUT_REJECTED);
            }
            response.setMessage(e.getMessage());
            return response;
        }
    }

    private static void populatePayoutResponse(SingleWithdrawResponse response,
                                               ReapalSingleWithdrawData platformData,
                                               DfSingleTradeResult result) {
        response.setOrderNo(result.getMerchantOrderNo());
        response.setOutOrderNo(result.getOrderId());
        platformData.setRechargeAmount(result.getRechargeAmount());
        platformData.setSubmitStage(SubmitStage.PAYOUT_ACCEPTED);
        response.setMessage(result.getResultMsg());
    }

    private String resolvePayoutBankNo(Client client, ReapalConfig config,
                                       SingleWithdrawRequest request,
                                       SingleWithdrawResponse response) throws Exception {
        if (StringUtils.hasText(request.getBankNo())) {
            return request.getBankNo();
        }
        FastCardQueryResponse cardQueryResponse = queryCardBin(client, config, request.getCardNo());
        if (cardQueryResponse == null || !REAPAL_SUCCESS_CODE.equals(cardQueryResponse.getCode())
                || cardQueryResponse.getData() == null
                || !StringUtils.hasText(cardQueryResponse.getData().getBankNo())) {
            response.setSuccess(false);
            response.setMessage(cardQueryResponse != null && StringUtils.hasText(cardQueryResponse.getMsg())
                    ? cardQueryResponse.getMsg() : "融宝卡BIN查询未返回银行编号");
            return null;
        }
        return cardQueryResponse.getData().getBankNo();
    }

    private static DfSingleTradeRequest buildPayoutRequest(ReapalConfig config,
                                                            SingleWithdrawRequest request,
                                                            String bankNo) {
        DfSingleTradeRequest payoutRequest = new DfSingleTradeRequest();
        payoutRequest.setMerchantId(config.getMerchantId());
        payoutRequest.setCustomerId(config.getCustomerId());
        payoutRequest.setAmount(convertBigDecimalToFenLong(request.getAmount()));
        payoutRequest.setPaySight(PAY_SIGHT_OTHER);
        payoutRequest.setMerchantOrderNo(request.getOrderNo());
        payoutRequest.setAccountType(Objects.equals(SingleWithdrawRequest.EnumAccountType.COMPANY.getCode(),
                request.getAccountType()) ? ACCOUNT_TYPE_CORPORATE : ACCOUNT_TYPE_PERSONAL);
        payoutRequest.setBankNo(bankNo);
        payoutRequest.setCardNo(request.getCardNo());
        payoutRequest.setCardName(request.getCardName());
        payoutRequest.setNotifyUrl(request.getNotifyUrl());
        payoutRequest.setProductCode(PAYOUT_PRODUCT_CODE);
        payoutRequest.setBusinessCode(PAYOUT_BUSINESS_CODE);
        payoutRequest.setTradeType(STANDARD_PAYOUT_TRADE_TYPE);
        payoutRequest.setRechargeType(ONLINE_BANK_RECHARGE_TYPE);
        return payoutRequest;
    }

    private static boolean isPayoutAccepted(DfSingleTradeResponse payoutResponse) {
        return payoutResponse != null
                && REAPAL_SUCCESS_CODE.equals(payoutResponse.getCode())
                && payoutResponse.getData() != null
                && isPayoutBusinessAccepted(payoutResponse.getData().getResultCode())
                && StringUtils.hasText(payoutResponse.getData().getOrderId())
                && payoutResponse.getData().getRechargeAmount() != null
                && payoutResponse.getData().getRechargeAmount() > 0;
    }

    private static boolean isPayoutBusinessAccepted(String resultCode) {
        return REAPAL_SUCCESS_CODE.equals(resultCode) || PAYOUT_WAIT_RECHARGE_CODE.equals(resultCode);
    }

    private static String resolvePayoutMessage(DfSingleTradeResponse payoutResponse) {
        if (payoutResponse == null) {
            return "融宝代付响应为空";
        }
        if (payoutResponse.getData() != null && StringUtils.hasText(payoutResponse.getData().getResultMsg())) {
            return payoutResponse.getData().getResultMsg();
        }
        return payoutResponse.getMsg();
    }

    private static String validateRequest(SingleWithdrawRequest request) {
        if (request == null) {
            return "提现请求不能为空";
        }
        if (!StringUtils.hasText(request.getOrderNo())) {
            return "代付订单号不能为空";
        }
        if (!StringUtils.hasText(request.getRechargeOrderNo())) {
            return "充值订单号不能为空";
        }
        if (Objects.equals(request.getOrderNo(), request.getRechargeOrderNo())) {
            return "充值订单号不能与代付订单号相同";
        }
        if (request.getOrderNo().length() > 50 || request.getRechargeOrderNo().length() > 50) {
            return "订单号长度不能超过50个字符";
        }
        if (!StringUtils.hasText(request.getCardNo()) || !StringUtils.hasText(request.getCardName())) {
            return "收款银行卡号和开户姓名不能为空";
        }
        if (!Objects.equals(request.getAccountType(), SingleWithdrawRequest.EnumAccountType.COMPANY.getCode())
                && !Objects.equals(request.getAccountType(), SingleWithdrawRequest.EnumAccountType.PERSONAL.getCode())) {
            return "账户类型不正确";
        }
        if (!isValidAmount(request.getAmount())) {
            return "金额必须大于0且最多保留两位小数";
        }
        return null;
    }

    private static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.signum() > 0 && amount.stripTrailingZeros().scale() <= 2;
    }

    private FastCardQueryResponse queryCardBin(Client client, ReapalConfig config, String cardNo) throws Exception {
        FastCardQueryRequest cardQueryRequest = new FastCardQueryRequest();
        cardQueryRequest.setMerchantId(config.getMerchantId());
        cardQueryRequest.setCardNo(cardNo);
        FastCardQueryResponse cardQueryResponse = client.execute(cardQueryRequest);
        log.info("融宝卡BIN查询响应结果：{}", JSON.toJSONString(cardQueryResponse));
        return cardQueryResponse;
    }

    @Override
    public QueryBalanceResponse queryBalance(QueryBalanceRequest request) {
        QueryBalanceResponse response = new QueryBalanceResponse();
        try {
            ReapalConfig config = getConfig();
            Client client = buildClient(config);
            MemberMerchantAccountBalanceRequest balanceRequest = new MemberMerchantAccountBalanceRequest();
            balanceRequest.setCustomerId(config.getCustomerId());
            balanceRequest.setMerchantId(config.getMerchantId());
            MemberMerchantAccountBalanceResponse balanceResponse = client.execute(balanceRequest);
            log.info("融宝查询余额响应结果：{}", JSON.toJSONString(balanceResponse));

            if (balanceResponse != null && REAPAL_SUCCESS_CODE.equals(balanceResponse.getCode())
                    && balanceResponse.getData() != null) {
                response.setSuccess(true);
                response.setAvailableBalance(balanceResponse.getData().getPaymentBalance());
                response.setMessage(balanceResponse.getMsg());
            } else {
                response.setSuccess(false);
                response.setMessage(balanceResponse == null ? "融宝余额响应为空" : balanceResponse.getMsg());
            }
        } catch (Exception e) {
            log.error("融宝查询余额异常", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    @Override
    public QueryResponse queryTradingOrderNo(String orderNo) {
        QueryResponse response = new QueryResponse();
        try {
            ReapalConfig config = getConfig();
            Client client = buildClient(config);
            DfTradeQueryRequest queryRequest = new DfTradeQueryRequest();
            queryRequest.setMerchantId(config.getMerchantId());
            queryRequest.setCustomerId(config.getCustomerId());
            queryRequest.setMerchantOrderNo(orderNo);

            DfTradeQueryResponse queryResponse = client.execute(queryRequest);
            log.info("融宝代付查询响应结果：{}", JSON.toJSONString(queryResponse));
            response.setResponseBody(JSON.toJSONString(queryResponse));
            if (!isTradeQuerySuccessful(queryResponse)) {
                response.setSuccess(false);
                response.setMessage(resolveTradeQueryMessage(queryResponse));
                return response;
            }

            DfTradeSubResult detail = findTradeDetail(queryResponse.getData().getDetails(), orderNo);
            if (detail == null) {
                response.setSuccess(false);
                response.setMessage("融宝代付查询响应明细为空");
                return response;
            }
            response.setSuccess(true);
            response.setOrderStatus(convertReapalStatus(detail.getStatus()));
            response.setFailReason(detail.getResultMsg());
            response.setMessage(detail.getResultMsg());
        } catch (Exception e) {
            log.error("融宝代付查询异常", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    private static boolean isTradeQuerySuccessful(DfTradeQueryResponse queryResponse) {
        return queryResponse != null
                && REAPAL_SUCCESS_CODE.equals(queryResponse.getCode())
                && queryResponse.getData() != null
                && REAPAL_SUCCESS_CODE.equals(queryResponse.getData().getResultCode());
    }

    private static String resolveTradeQueryMessage(DfTradeQueryResponse queryResponse) {
        if (queryResponse == null) {
            return "融宝代付查询响应为空";
        }
        if (queryResponse.getData() != null && StringUtils.hasText(queryResponse.getData().getResultMsg())) {
            return queryResponse.getData().getResultMsg();
        }
        return queryResponse.getMsg();
    }

    /**
     * 查询账单，融宝暂未实现。
     */
    @Override
    public QueryBillResponse queryBill(QueryBillRequest request) {
        return new QueryBillResponse();
    }

    @Override
    public CancelResponse cancelWithdraw(CancelRequest request) {
        return null;
    }

    @Override
    public String getOpenid(String code) {
        return "";
    }

    private ReapalConfig getConfig() {
        TradePlatformConfig tradePlatformConfig = platformConfigService.get(PlatformConstant.REAPAL);
        if (tradePlatformConfig instanceof ReapalConfig config) {
            return config;
        }
        throw new IllegalStateException("reapal config is null");
    }

    protected Client buildClient(ReapalConfig config) {
        return clientFactory.create(config);
    }

    private static DfTradeSubResult findTradeDetail(List<DfTradeSubResult> details, String orderNo) {
        if (details == null || details.isEmpty()) {
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
                || REAPAL_STATUS_SERVICE_REJECTED.equals(status)
                || REAPAL_STATUS_CLOSED.equals(status)) {
            return OrderStatusConstant.FAIL;
        }
        if (REAPAL_STATUS_PROCESSING.equals(status) || REAPAL_STATUS_WAIT_RECHARGE.equals(status)) {
            return OrderStatusConstant.PROCESSING;
        }
        return null;
    }

    private static Long convertBigDecimalToFenLong(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
