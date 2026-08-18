package com.magic.withdraw.reapal.recharge;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.core.service.PlatformConfigService;
import com.magic.withdraw.reapal.ReapalClientFactory;
import com.magic.withdraw.reapal.ReapalConfig;
import com.magic.withdraw.reapal.ReapalSingleWithdrawData;
import com.magic.withdraw.reapal.ReapalSingleWithdrawData.SubmitStage;
import com.magic.withdraw.reapal.recharge.ReapalRechargeOrderStore.RechargePollingOrder;
import com.magic.withdraw.reapal.recharge.ReapalRechargeQueryResult.RechargeState;
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

import java.util.Set;

/**
 * 融宝充值服务，提供充值发起、主动查询和待查询任务登记能力。
 */
@Slf4j
@RequiredArgsConstructor
public class ReapalRechargeService {

    private static final String REAPAL_SUCCESS_CODE = "0000";
    private static final String RECHARGE_BUSINESS_CODE = "RECHARGE";
    private static final String TERMINAL_TYPE_WEB = "web";
    private static final String NON_MEMBER_PAYMENT = "0";
    private static final String DEFAULT_RECHARGE_TITLE = "订单代付充值";
    private static final String COMPLETED_STATUS = "completed";
    private static final Set<String> PROCESSING_STATUSES = Set.of("wait", "processing");
    private static final Set<String> ACCEPTED_STATUSES = Set.of(
            "wait", "processing", COMPLETED_STATUS);
    private static final Set<String> FAILED_STATUSES = Set.of("failed", "closed");

    private final ReapalRechargeStrategyRegistry strategyRegistry;
    private final ReapalRechargeOrderStore orderStore;
    private final PlatformConfigService platformConfigService;
    private final ReapalClientFactory clientFactory;

    /**
     * 独立发起一笔融宝关联充值。
     * 处理中订单会自动登记到充值巡检存储中。
     */
    public ReapalRechargeResponse submit(ReapalRechargeRequest request) {
        String validationMessage = validateRequest(request);
        if (validationMessage != null) {
            return failedSubmitResponse(request, validationMessage);
        }
        try {
            ReapalConfig config = getConfig();
            String configValidationMessage = validate(config);
            if (configValidationMessage != null) {
                return failedSubmitResponse(request, configValidationMessage);
            }
            return doSubmit(clientFactory.create(config), config, request);
        } catch (Exception exception) {
            log.error("融宝充值提交异常，rechargeOrderNo={}",
                    request.getRechargeOrderNo(), exception);
            return failedSubmitResponse(request, exception.getMessage());
        }
    }

    /** 查询一笔融宝充值订单。 */
    public ReapalRechargeQueryResult query(String rechargeOrderNo) {
        return query(rechargeOrderNo, null);
    }

    /** 查询一笔融宝充值订单，并携带关联代付订单供终态回调使用。 */
    public ReapalRechargeQueryResult query(String rechargeOrderNo, String payoutOrderNo) {
        if (!StringUtils.hasText(rechargeOrderNo)) {
            return failedQueryResult(rechargeOrderNo, payoutOrderNo, "充值订单号不能为空", null);
        }
        try {
            ReapalConfig config = getConfig();
            Client client = clientFactory.create(config);
            return query(client, config, rechargeOrderNo, payoutOrderNo);
        } catch (Exception exception) {
            log.warn("融宝充值查询异常，rechargeOrderNo={}", rechargeOrderNo, exception);
            return failedQueryResult(
                    rechargeOrderNo, payoutOrderNo, exception.getMessage(), null);
        }
    }

    public String validate(ReapalConfig config) {
        ReapalRechargeStrategy strategy = strategyRegistry.get(config.getRechargeMode());
        if (!StringUtils.hasText(config.getMemberId())) {
            return "充值会员号不能为空";
        }
        if (!StringUtils.hasText(config.getMemberIp())) {
            return "充值用户IP不能为空";
        }
        if (config.getRechargeQueryInterval() <= 0) {
            return "充值查询间隔必须大于0秒";
        }
        if (config.getRechargeQueryTimeout() < config.getRechargeQueryInterval()) {
            return "充值查询总时长不能小于查询间隔";
        }
        return strategy.validate(config);
    }

    /** 兼容单笔代付流程，将独立充值响应映射到通用提现响应。 */
    public SingleWithdrawResponse submitForWithdraw(
            Client client, ReapalConfig config,
            SingleWithdrawRequest request,
            SingleWithdrawResponse response,
            ReapalSingleWithdrawData platformData,
            DfSingleTradeResult payoutResult) {
        ReapalRechargeRequest rechargeRequest = new ReapalRechargeRequest()
                .setRechargeOrderNo(request.getRechargeOrderNo())
                .setPayoutOrderNo(response.getOrderNo())
                .setPayoutOutOrderNo(payoutResult.getOrderId())
                .setAmount(payoutResult.getRechargeAmount())
                .setOrderTitle(request.getOrderTitle());
        ReapalRechargeResponse rechargeResponse = doSubmit(client, config, rechargeRequest);
        applyToWithdrawResponse(response, platformData, rechargeResponse);
        return response;
    }

    private ReapalRechargeResponse doSubmit(
            Client client, ReapalConfig config, ReapalRechargeRequest request) {
        String currentOrderValidationMessage = validateCurrentOrder(request);
        if (currentOrderValidationMessage != null) {
            return failedSubmitResponse(request, currentOrderValidationMessage);
        }
        ReapalRechargeResponse response = baseSubmitResponse(request);
        ReapalRechargeStrategy strategy = strategyRegistry.get(config.getRechargeMode());
        TradeRequest tradeRequest = buildRechargeRequest(config, request, strategy);
        response.setRequestBody(JSON.toJSONString(tradeRequest));
        try {
            TradeResponse tradeResponse = client.execute(tradeRequest);
            response.setResponseBody(JSON.toJSONString(tradeResponse));
            log.info("融宝订单代付充值响应结果：{}", response.getResponseBody());
            return applySubmitResult(response, tradeResponse, config);
        } catch (Exception exception) {
            log.warn("融宝订单代付充值失败：{}", request.getRechargeOrderNo(), exception);
            response.setSuccess(false);
            response.setMessage(StringUtils.hasText(exception.getMessage())
                    ? exception.getMessage() : "融宝充值提交异常");
            return response;
        }
    }

    private String validateCurrentOrder(ReapalRechargeRequest request) {
        RechargePollingOrder current = orderStore.getByPayoutOrderNo(
                request.getPayoutOrderNo());
        if (current == null) {
            return null;
        }
        return "代付订单存在待查询的充值订单，不允许再次发起充值："
                + current.rechargeOrderNo();
    }

    private ReapalRechargeQueryResult query(Client client, ReapalConfig config,
                                            String rechargeOrderNo,
                                            String payoutOrderNo) {
        OrderQueryRequest request = new OrderQueryRequest();
        request.setMerchantId(config.getMerchantId());
        request.setMerchantOrderNo(rechargeOrderNo);
        try {
            OrderQueryResponse response = client.execute(request);
            String responseBody = JSON.toJSONString(response);
            log.info("融宝充值查询响应，rechargeOrderNo={}, response={}",
                    rechargeOrderNo, responseBody);
            if (response == null || !REAPAL_SUCCESS_CODE.equals(response.getCode())
                    || response.getData() == null) {
                String message = response == null ? "融宝充值查询响应为空" : response.getMsg();
                return failedQueryResult(rechargeOrderNo, payoutOrderNo, message, responseBody);
            }
            return successfulQueryResult(
                    rechargeOrderNo, payoutOrderNo, response, responseBody);
        } catch (Exception exception) {
            log.warn("融宝充值查询异常，rechargeOrderNo={}", rechargeOrderNo, exception);
            return failedQueryResult(
                    rechargeOrderNo, payoutOrderNo, exception.getMessage(), null);
        }
    }

    private ReapalRechargeResponse applySubmitResult(ReapalRechargeResponse response,
                                                     TradeResponse tradeResponse,
                                                     ReapalConfig config) {
        if (tradeResponse == null || !REAPAL_SUCCESS_CODE.equals(tradeResponse.getCode())
                || tradeResponse.getData() == null) {
            response.setSuccess(false);
            response.setMessage(resolveSubmitMessage(tradeResponse));
            return response;
        }
        TradeResult result = tradeResponse.getData();
        populateSubmitResponse(response, result);
        String rechargeStatus = result.getOrderStatus();
        if (StringUtils.hasText(rechargeStatus)
                && FAILED_STATUSES.contains(rechargeStatus)) {
            orderStore.remove(response.getRechargeOrderNo());
            response.setSuccess(false);
            return response;
        }
        if (!StringUtils.hasText(rechargeStatus)
                || !ACCEPTED_STATUSES.contains(rechargeStatus)) {
            response.setSuccess(false);
            return response;
        }
        if (!COMPLETED_STATUS.equals(result.getOrderStatus())
                && !StringUtils.hasText(response.getPaymentUrl())) {
            response.setSuccess(false);
            response.setMessage("融宝充值已受理，但未返回支付跳转地址");
            return response;
        }
        response.setSuccess(true);
        response.setMessage(result.getResultMsg());
        registerPollingIfRequired(response, config);
        return response;
    }

    private void registerPollingIfRequired(ReapalRechargeResponse response,
                                           ReapalConfig config) {
        if (COMPLETED_STATUS.equals(response.getRechargeStatus())) {
            orderStore.removeByPayoutOrderNo(response.getPayoutOrderNo());
            response.setPollingRequired(false);
            return;
        }
        response.setPollingRequired(true);
        long now = System.currentTimeMillis();
        long intervalMillis = Math.multiplyExact(config.getRechargeQueryInterval(), 1000L);
        long timeoutMillis = Math.multiplyExact(config.getRechargeQueryTimeout(), 1000L);
        orderStore.add(new RechargePollingOrder(
                response.getRechargeOrderNo(), response.getPayoutOrderNo(),
                now + intervalMillis, now + timeoutMillis, intervalMillis));
    }

    private static TradeRequest buildRechargeRequest(ReapalConfig config,
                                                      ReapalRechargeRequest request,
                                                      ReapalRechargeStrategy strategy) {
        String title = StringUtils.hasText(request.getOrderTitle())
                ? request.getOrderTitle() : DEFAULT_RECHARGE_TITLE;
        TradeRequest rechargeRequest = new TradeRequest();
        rechargeRequest.setMerchantId(config.getMerchantId());
        rechargeRequest.setCustomerId(config.getCustomerId());
        rechargeRequest.setMemberId(config.getMemberId());
        rechargeRequest.setMerchantOrderNo(request.getRechargeOrderNo());
        rechargeRequest.setAmount(request.getAmount());
        rechargeRequest.setBusinessCode(RECHARGE_BUSINESS_CODE);
        rechargeRequest.setProductCode(strategy.productCode());
        rechargeRequest.setIsMember(NON_MEMBER_PAYMENT);
        rechargeRequest.setReturnUrl(config.getReturnUrl());
        rechargeRequest.setSubject(title);
        rechargeRequest.setBody(title);
        rechargeRequest.setExpend(strategy.buildExpend(config));
        rechargeRequest.setTerminalType(TERMINAL_TYPE_WEB);
        rechargeRequest.setMemberIp(config.getMemberIp());
        rechargeRequest.setDfOrderId(request.getPayoutOutOrderNo());
        return rechargeRequest;
    }

    private static void populateSubmitResponse(ReapalRechargeResponse response,
                                               TradeResult result) {
        if (StringUtils.hasText(result.getMerchantOrderNo())) {
            response.setRechargeOrderNo(result.getMerchantOrderNo());
        }
        response.setRechargeOutOrderNo(result.getOrderId());
        response.setRechargeStatus(result.getOrderStatus());
        response.setMessage(result.getResultMsg());
        if (result.getAmount() != null) {
            response.setAmount(result.getAmount());
        }
        if (result.getExtendMap() != null) {
            response.setPaymentUrl(result.getExtendMap().get("callbackUrl"));
            response.setPaymentToken(result.getExtendMap().get("token"));
        }
    }

    private static ReapalRechargeQueryResult successfulQueryResult(
            String rechargeOrderNo, String payoutOrderNo,
            OrderQueryResponse response, String responseBody) {
        OrderQueryResult result = response.getData();
        String status = result.getOrdersts();
        return new ReapalRechargeQueryResult()
                .setQuerySuccessful(true)
                .setState(convertState(status))
                .setPayoutOrderNo(payoutOrderNo)
                .setRechargeOrderNo(StringUtils.hasText(result.getMerchantOrderNo())
                        ? result.getMerchantOrderNo() : rechargeOrderNo)
                .setRechargeOutOrderNo(result.getOrderId())
                .setAmount(result.getAmount())
                .setRechargeStatus(status)
                .setResponseBody(responseBody)
                .setMessage(resolveQueryMessage(status, response.getMsg()));
    }

    private static ReapalRechargeQueryResult failedQueryResult(
            String rechargeOrderNo, String payoutOrderNo,
            String message, String responseBody) {
        return new ReapalRechargeQueryResult()
                .setQuerySuccessful(false)
                .setState(RechargeState.UNKNOWN)
                .setPayoutOrderNo(payoutOrderNo)
                .setRechargeOrderNo(rechargeOrderNo)
                .setResponseBody(responseBody)
                .setMessage(StringUtils.hasText(message) ? message : "融宝充值查询失败");
    }

    private static RechargeState convertState(String status) {
        if (!StringUtils.hasText(status)) {
            return RechargeState.UNKNOWN;
        }
        if (COMPLETED_STATUS.equals(status)) {
            return RechargeState.SUCCESS;
        }
        if (FAILED_STATUSES.contains(status)) {
            return RechargeState.FAILED;
        }
        if (PROCESSING_STATUSES.contains(status)) {
            return RechargeState.PROCESSING;
        }
        return RechargeState.UNKNOWN;
    }

    private static String resolveQueryMessage(String status, String responseMessage) {
        if ("failed".equals(status)) {
            return "融宝充值失败";
        }
        if ("closed".equals(status)) {
            return "融宝充值订单已关闭";
        }
        return responseMessage;
    }

    private static void applyToWithdrawResponse(
            SingleWithdrawResponse response,
            ReapalSingleWithdrawData platformData,
            ReapalRechargeResponse rechargeResponse) {
        platformData.setRechargeOrderNo(rechargeResponse.getRechargeOrderNo());
        platformData.setRechargeOutOrderNo(rechargeResponse.getRechargeOutOrderNo());
        platformData.setRechargeAmount(rechargeResponse.getAmount());
        platformData.setRechargeStatus(rechargeResponse.getRechargeStatus());
        platformData.setPaymentUrl(rechargeResponse.getPaymentUrl());
        platformData.setPaymentToken(rechargeResponse.getPaymentToken());
        platformData.setRechargeRequestBody(rechargeResponse.getRequestBody());
        platformData.setRechargeResponseBody(rechargeResponse.getResponseBody());
        response.setSuccess(rechargeResponse.isSuccess());
        response.setMessage(rechargeResponse.getMessage());
        if (rechargeResponse.isSuccess()) {
            platformData.setSubmitStage(SubmitStage.RECHARGE_ACCEPTED);
            response.setPollingRequired(
                    COMPLETED_STATUS.equals(rechargeResponse.getRechargeStatus()));
        } else if (StringUtils.hasText(rechargeResponse.getRechargeStatus())
                && FAILED_STATUSES.contains(rechargeResponse.getRechargeStatus())) {
            platformData.setSubmitStage(SubmitStage.RECHARGE_REJECTED);
            response.setPollingRequired(false);
        } else {
            platformData.setSubmitStage(SubmitStage.RECHARGE_UNKNOWN);
            response.setPollingRequired(false);
        }
    }

    private static ReapalRechargeResponse baseSubmitResponse(ReapalRechargeRequest request) {
        return new ReapalRechargeResponse()
                .setPayoutOrderNo(request.getPayoutOrderNo())
                .setRechargeOrderNo(request.getRechargeOrderNo())
                .setAmount(request.getAmount());
    }

    private static ReapalRechargeResponse failedSubmitResponse(
            ReapalRechargeRequest request, String message) {
        ReapalRechargeResponse response = request == null
                ? new ReapalRechargeResponse() : baseSubmitResponse(request);
        return response.setSuccess(false)
                .setMessage(StringUtils.hasText(message) ? message : "融宝充值提交失败");
    }

    private static String resolveSubmitMessage(TradeResponse response) {
        if (response == null) {
            return "融宝充值响应为空";
        }
        if (response.getData() != null
                && StringUtils.hasText(response.getData().getResultMsg())) {
            return response.getData().getResultMsg();
        }
        return response.getMsg();
    }

    private static String validateRequest(ReapalRechargeRequest request) {
        if (request == null) {
            return "充值请求不能为空";
        }
        if (!StringUtils.hasText(request.getRechargeOrderNo())) {
            return "充值订单号不能为空";
        }
        if (!StringUtils.hasText(request.getPayoutOrderNo())) {
            return "关联代付订单号不能为空";
        }
        if (!StringUtils.hasText(request.getPayoutOutOrderNo())) {
            return "融宝代付订单号不能为空";
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            return "充值金额必须大于0";
        }
        return null;
    }

    private ReapalConfig getConfig() {
        TradePlatformConfig config = platformConfigService.get(PlatformConstant.REAPAL);
        if (config instanceof ReapalConfig reapalConfig) {
            return reapalConfig;
        }
        throw new IllegalStateException("reapal config is null");
    }
}
