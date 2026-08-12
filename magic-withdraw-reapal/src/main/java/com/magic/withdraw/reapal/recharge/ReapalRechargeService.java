package com.magic.withdraw.reapal.recharge;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.reapal.ReapalConfig;
import com.magic.withdraw.reapal.ReapalSingleWithdrawData;
import com.magic.withdraw.reapal.ReapalSingleWithdrawData.SubmitStage;
import com.magic.withdraw.reapal.recharge.ReapalRechargeOrderStore.Claim;
import com.magic.withdraw.reapal.recharge.ReapalRechargeOrderStore.ClaimStatus;
import com.reapal.api.Client;
import com.reapal.api.model.DfSingleTradeResult;
import com.reapal.api.model.OrderQueryResult;
import com.reapal.api.model.TradeResult;
import com.reapal.api.request.OrderQueryRequest;
import com.reapal.api.request.TradeRequest;
import com.reapal.api.response.OrderQueryResponse;
import com.reapal.api.response.TradeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 融宝充值公共流程：构建订单、提交、异常对账及响应归一化。
 */
@Slf4j
@RequiredArgsConstructor
public class ReapalRechargeService {

    private static final String REAPAL_SUCCESS_CODE = "0000";
    private static final String RECHARGE_BUSINESS_CODE = "RECHARGE";
    private static final String TERMINAL_TYPE_WEB = "web";
    private static final String NON_MEMBER_PAYMENT = "0";
    private static final String DEFAULT_RECHARGE_TITLE = "订单代付充值";
    private static final Set<String> ACCEPTED_STATUSES = Set.of("wait", "processing", "completed");
    private static final Set<String> FAILED_STATUSES = Set.of("failed", "closed");

    private final ReapalRechargeStrategyRegistry strategyRegistry;
    private final ReapalRechargeOrderStore orderStore;

    public String validate(ReapalConfig config) {
        ReapalRechargeStrategy strategy = strategyRegistry.get(config.getRechargeMode());
        if (!StringUtils.hasText(config.getMemberId())) {
            return "充值会员号不能为空";
        }
        if (!StringUtils.hasText(config.getMemberIp())) {
            return "充值用户IP不能为空";
        }
        if (!StringUtils.hasText(config.getRechargeNotifyUrl())) {
            return "充值异步通知地址不能为空";
        }
        return strategy.validate(config);
    }

    public SingleWithdrawResponse submit(Client client, ReapalConfig config,
                                         SingleWithdrawRequest request,
                                         SingleWithdrawResponse response,
                                         ReapalSingleWithdrawData platformData,
                                         DfSingleTradeResult payoutResult) {
        ReapalRechargeStrategy strategy = strategyRegistry.get(config.getRechargeMode());
        TradeRequest rechargeRequest = buildRechargeRequest(config, request, payoutResult, strategy);
        orderStore.save(request.getRechargeOrderNo(), request.getOrderNo());
        platformData.setRechargeRequestBody(JSON.toJSONString(rechargeRequest));
        TradeResponse rechargeResponse = null;
        Exception submitException = null;
        try {
            rechargeResponse = client.execute(rechargeRequest);
            platformData.setRechargeResponseBody(JSON.toJSONString(rechargeResponse));
            log.info("融宝订单代付充值响应结果：{}", platformData.getRechargeResponseBody());
            if (applySubmitResult(response, platformData, rechargeResponse)) {
                return response;
            }
        } catch (Exception e) {
            submitException = e;
            log.warn("融宝订单代付充值响应不明确，准备查询充值订单：{}", request.getRechargeOrderNo(), e);
        }
        return reconcile(client, config, request.getRechargeOrderNo(), response, platformData,
                rechargeResponse, submitException);
    }

    private SingleWithdrawResponse reconcile(Client client, ReapalConfig config,
                                              String rechargeOrderNo,
                                              SingleWithdrawResponse response,
                                              ReapalSingleWithdrawData platformData,
                                              TradeResponse rechargeResponse,
                                              Exception submitException) {
        OrderQueryResponse queryResponse = null;
        try {
            OrderQueryRequest queryRequest = new OrderQueryRequest();
            queryRequest.setMerchantId(config.getMerchantId());
            queryRequest.setMerchantOrderNo(rechargeOrderNo);
            queryResponse = client.execute(queryRequest);
            log.info("融宝订单代付充值对账响应结果：{}", JSON.toJSONString(queryResponse));
            platformData.setRechargeResponseBody(mergeResponses(rechargeResponse, queryResponse));
            if (applyQueryResult(response, platformData, queryResponse)) {
                return response;
            }
        } catch (Exception queryException) {
            log.warn("融宝订单代付充值对账失败：{}", rechargeOrderNo, queryException);
            platformData.setRechargeResponseBody(mergeResponses(rechargeResponse, queryResponse));
        }

        response.setSuccess(false);
        platformData.setSubmitStage(SubmitStage.RECHARGE_UNKNOWN);
        response.setMessage(submitException == null ? "融宝充值结果暂时无法确认" : submitException.getMessage());
        return response;
    }

    private boolean applySubmitResult(SingleWithdrawResponse response,
                                      ReapalSingleWithdrawData platformData,
                                      TradeResponse rechargeResponse) {
        if (rechargeResponse == null || !REAPAL_SUCCESS_CODE.equals(rechargeResponse.getCode())
                || rechargeResponse.getData() == null) {
            return false;
        }

        TradeResult result = rechargeResponse.getData();
        populateResponse(platformData, result);
        if (FAILED_STATUSES.contains(result.getOrderStatus())) {
            orderStore.remove(platformData.getRechargeOrderNo());
            response.setSuccess(false);
            platformData.setSubmitStage(SubmitStage.RECHARGE_REJECTED);
            response.setMessage(result.getResultMsg());
            return true;
        }
        if (!ACCEPTED_STATUSES.contains(result.getOrderStatus())) {
            return false;
        }
        if (!"completed".equals(result.getOrderStatus()) && !StringUtils.hasText(platformData.getPaymentUrl())) {
            response.setSuccess(false);
            platformData.setSubmitStage(SubmitStage.RECHARGE_UNKNOWN);
            response.setMessage("融宝充值已受理，但未返回支付跳转地址");
            return true;
        }

        response.setSuccess(true);
        platformData.setSubmitStage(SubmitStage.RECHARGE_ACCEPTED);
        updatePollingRequirement(response, platformData.getRechargeOrderNo(), result.getOrderStatus());
        response.setMessage(result.getResultMsg());
        return true;
    }

    private boolean applyQueryResult(SingleWithdrawResponse response,
                                     ReapalSingleWithdrawData platformData,
                                     OrderQueryResponse queryResponse) {
        if (queryResponse == null || !REAPAL_SUCCESS_CODE.equals(queryResponse.getCode())
                || queryResponse.getData() == null) {
            return false;
        }
        OrderQueryResult result = queryResponse.getData();
        platformData.setRechargeOutOrderNo(result.getOrderId());
        platformData.setRechargeStatus(result.getOrdersts());
        if (result.getAmount() != null) {
            platformData.setRechargeAmount(result.getAmount());
        }
        if (ACCEPTED_STATUSES.contains(result.getOrdersts())) {
            response.setSuccess(true);
            platformData.setSubmitStage(SubmitStage.RECHARGE_ACCEPTED);
            updatePollingRequirement(response, platformData.getRechargeOrderNo(), result.getOrdersts());
            response.setMessage(queryResponse.getMsg());
            return true;
        }
        if (FAILED_STATUSES.contains(result.getOrdersts())) {
            orderStore.remove(platformData.getRechargeOrderNo());
            response.setSuccess(false);
            platformData.setSubmitStage(SubmitStage.RECHARGE_REJECTED);
            response.setMessage(queryResponse.getMsg());
            return true;
        }
        return false;
    }

    private static TradeRequest buildRechargeRequest(ReapalConfig config,
                                                      SingleWithdrawRequest request,
                                                      DfSingleTradeResult payoutResult,
                                                      ReapalRechargeStrategy strategy) {
        String title = StringUtils.hasText(request.getOrderTitle())
                ? request.getOrderTitle() : DEFAULT_RECHARGE_TITLE;
        TradeRequest rechargeRequest = new TradeRequest();
        rechargeRequest.setMerchantId(config.getMerchantId());
        rechargeRequest.setCustomerId(config.getCustomerId());
        rechargeRequest.setMemberId(config.getMemberId());
        rechargeRequest.setMerchantOrderNo(request.getRechargeOrderNo());
        rechargeRequest.setAmount(payoutResult.getRechargeAmount());
        rechargeRequest.setBusinessCode(RECHARGE_BUSINESS_CODE);
        rechargeRequest.setProductCode(strategy.productCode());
        rechargeRequest.setIsMember(NON_MEMBER_PAYMENT);
        rechargeRequest.setNotifyUrl(config.getRechargeNotifyUrl());
        rechargeRequest.setReturnUrl(config.getReturnUrl());
        rechargeRequest.setSubject(title);
        rechargeRequest.setBody(title);
        rechargeRequest.setExpend(strategy.buildExpend(config));
        rechargeRequest.setTerminalType(TERMINAL_TYPE_WEB);
        rechargeRequest.setMemberIp(config.getMemberIp());
        rechargeRequest.setDfOrderId(payoutResult.getOrderId());
        return rechargeRequest;
    }

    private void updatePollingRequirement(SingleWithdrawResponse response,
                                          String rechargeOrderNo,
                                          String rechargeStatus) {
        if (!"completed".equals(rechargeStatus)) {
            response.setPollingRequired(false);
            return;
        }
        Claim claim = orderStore.claim(rechargeOrderNo);
        response.setPollingRequired(claim.status() == ClaimStatus.CLAIMED);
    }

    private static void populateResponse(ReapalSingleWithdrawData platformData, TradeResult result) {
        if (StringUtils.hasText(result.getMerchantOrderNo())) {
            platformData.setRechargeOrderNo(result.getMerchantOrderNo());
        }
        platformData.setRechargeOutOrderNo(result.getOrderId());
        platformData.setRechargeStatus(result.getOrderStatus());
        if (result.getAmount() != null) {
            platformData.setRechargeAmount(result.getAmount());
        }
        if (result.getExtendMap() != null) {
            platformData.setPaymentUrl(result.getExtendMap().get("callbackUrl"));
            platformData.setPaymentToken(result.getExtendMap().get("token"));
        }
    }

    private static String mergeResponses(TradeResponse submitResponse, OrderQueryResponse queryResponse) {
        Map<String, Object> responses = new LinkedHashMap<>();
        responses.put("submit", submitResponse);
        responses.put("query", queryResponse);
        return JSON.toJSONString(responses);
    }
}
