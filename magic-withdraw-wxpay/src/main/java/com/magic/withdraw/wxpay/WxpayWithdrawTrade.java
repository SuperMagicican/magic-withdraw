package com.magic.withdraw.wxpay;

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
import com.magic.withdraw.wxpay.enums.TransferBillStatus;
import com.magic.withdraw.wxpay.request.*;
import com.magic.withdraw.wxpay.response.CancelTransferResponse;
import com.magic.withdraw.wxpay.response.TransferBillEntity;
import com.magic.withdraw.wxpay.response.TransferToUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static com.magic.withdraw.wxpay.WxpayConstant.*;

/**
 * @author lgy
 * @since 2026/3/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
@TradePlatform(PlatformConstant.WXPAY)
public class WxpayWithdrawTrade implements TradeService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private final PlatformConfigService platformConfigService;

    @Override
    public SingleWithdrawResponse singleWithdraw(SingleWithdrawRequest request) {
        SingleWithdrawResponse response = new SingleWithdrawResponse();
        response.setOrderNo(request.getOrderNo());
        try {
            TradePlatformConfig tradePlatformConfig = platformConfigService.get(PlatformConstant.WXPAY);
            if (tradePlatformConfig instanceof WxpayConfig wxpayConfig) {
                TransferToUserRequest transferToUserRequest = new TransferToUserRequest();
                transferToUserRequest.appid = wxpayConfig.getAppid();
                transferToUserRequest.outBillNo = request.getOrderNo();
                transferToUserRequest.transferSceneId = wxpayConfig.getTransferSceneId();
                if (!StringUtils.hasLength(request.getOpenid())) {
                    request.setOpenid(this.getOpenid(request.getCode()));
                }
                transferToUserRequest.openid = request.getOpenid();
                transferToUserRequest.userName = WXPayUtility.encrypt(wxpayConfig.getWechatPayPublicKey(), request.getCardName());
                transferToUserRequest.transferAmount = convertBigDecimalToFenLong(request.getAmount());
                transferToUserRequest.transferRemark = request.getOrderTitle();
                transferToUserRequest.notifyUrl = request.getNotifyUrl();
                transferToUserRequest.transferSceneReportInfos = new ArrayList<>();
                TransferSceneReportInfo transferSceneReportInfosItem0 = new TransferSceneReportInfo();
                transferSceneReportInfosItem0.infoType = "奖励说明";
                transferSceneReportInfosItem0.infoContent = request.getOrderTitle();
                transferToUserRequest.transferSceneReportInfos.add(transferSceneReportInfosItem0);
                TransferSceneReportInfo transferSceneReportInfosItem1 = new TransferSceneReportInfo();
                transferSceneReportInfosItem1.infoType = "活动名称";
                transferSceneReportInfosItem1.infoContent = request.getOrderTitle();
                transferToUserRequest.transferSceneReportInfos.add(transferSceneReportInfosItem1);
                try {
                    response.setRequestBody(JSON.toJSONString(transferToUserRequest));
                    WxpayRequestModel<TransferToUserRequest, TransferToUserResponse> wxpayRequestModel = new WxpayRequestModel<>();
                    wxpayRequestModel.setRequest(transferToUserRequest);
                    wxpayRequestModel.setWxpayConfig(wxpayConfig);
                    wxpayRequestModel.setUri(TRANSFER_TO_USER_PATH);
                    wxpayRequestModel.setHost(HOST);
                    wxpayRequestModel.setClazz(TransferToUserResponse.class);
                    wxpayRequestModel.setMethod(POST);
                    TransferToUserResponse transferToUserResponse = this.run(wxpayRequestModel);;
                    log.info("微信支付单笔转账响应结果， {}", transferToUserResponse);
                    response.setResponseBody(JSON.toJSONString(transferToUserResponse));
                    TransferBillStatus state = transferToUserResponse.getState();
                    if (Objects.equals(state, TransferBillStatus.FAIL) ||
                            Objects.equals(state, TransferBillStatus.CANCELLED) ||
                            Objects.equals(state, TransferBillStatus.CANCELING)){
                        response.setSuccess(false);
                    } else {
                        response.setSuccess(true);
                        response.setPackageInfo(transferToUserResponse.getPackageInfo());
                    }
                    response.setOutOrderNo(transferToUserResponse.getTransferBillNo());
                } catch (WXPayUtility.ApiException e) {
                    log.error("微信支付单笔转账异常:", e);
                    response.setSuccess(false);
                }
            } else {
                log.error("微信支付配置异常");
                response.setSuccess(false);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            response.setSuccess(false);
        }
        return response;
    }

    @Override
    public QueryBalanceResponse queryBalance(QueryBalanceRequest request) {
        return null;
    }

    @Override
    public QueryResponse queryTradingOrderNo(String orderNo) {
        QueryResponse response = new QueryResponse();
        try {
            TradePlatformConfig tradePlatformConfig = platformConfigService.get(PlatformConstant.WXPAY);
            if (tradePlatformConfig instanceof WxpayConfig wxpayConfig) {
                try {
                    WxpayRequestModel<GetTransferBillByOutNoRequest, TransferBillEntity>
                            wxpayRequestModel = new WxpayRequestModel<>();
                    wxpayRequestModel.setWxpayConfig(wxpayConfig);
                    wxpayRequestModel.setUri(GET_TRANSFER_BILL_BY_OUT_NO_PATH.replace("{out_bill_no}", WXPayUtility.urlEncode(orderNo)));
                    wxpayRequestModel.setHost(HOST);
                    wxpayRequestModel.setClazz(TransferBillEntity.class);
                    wxpayRequestModel.setMethod(GET);
                    TransferBillEntity transferBillEntity = this.run(wxpayRequestModel);
                    log.info("微信支付查询单笔转账响应结果， {}", transferBillEntity);
                    response.setResponseBody(JSON.toJSONString(transferBillEntity));
                    TransferBillStatus state = transferBillEntity.getState();
                    response.setSuccess(true);
                    if (Objects.equals(TransferBillStatus.CANCELLED, state)) {
                        response.setOrderStatus(OrderStatusConstant.REFUND);
                    } else if (Objects.equals(TransferBillStatus.SUCCESS, state)) {
                        response.setOrderStatus(OrderStatusConstant.SUCCESS);
                    } else if (Objects.equals(TransferBillStatus.FAIL, state)) {
                        response.setOrderStatus(OrderStatusConstant.FAIL);
                    } else {
                        response.setOrderStatus(OrderStatusConstant.PROCESSING);
                    }
                    response.setFailReason(transferBillEntity.getFailReason());
                } catch (WXPayUtility.ApiException e) {
                    log.error("微信支付查询单笔转账异常:", e);
                    response.setSuccess(false);
                    response.setMessage("查询单笔转账异常:" + e.getMessage());
                }
            } else {
                log.error("微信支付查询单笔转账配置异常");
                response.setSuccess(false);
                response.setMessage("微信支付查询单笔转账配置异常");
            }
        } catch (Exception e) {
            log.error("获取微信支付配置异常", e);
            response.setSuccess(false);
            response.setMessage("获取微信支付配置异常");
        }
        return response;
    }

    @Override
    public CancelResponse cancelWithdraw(CancelRequest request) {
        CancelResponse response = new CancelResponse();
        try {
            TradePlatformConfig tradePlatformConfig = platformConfigService.get(PlatformConstant.WXPAY);
            if (tradePlatformConfig instanceof WxpayConfig wxpayConfig) {
                try {
                    WxpayRequestModel<CancelTransferRequest, CancelTransferResponse> wxpayRequestModel = new WxpayRequestModel<>();
                    wxpayRequestModel.setUri(CANCEL_TRANSFER_PATH.replace("{out_bill_no}", WXPayUtility.urlEncode(request.getOrderNo())));
                    wxpayRequestModel.setWxpayConfig(wxpayConfig);
                    wxpayRequestModel.setHost(HOST);
                    wxpayRequestModel.setClazz(CancelTransferResponse.class);
                    wxpayRequestModel.setMethod(POST);
                    CancelTransferResponse cancelTransferResponse = this.run(wxpayRequestModel);
                    log.info("微信支付撤销单笔转账响应结果， {}", cancelTransferResponse);
                    response.setSuccess(true);
                } catch (Exception e) {
                    log.error("微信支付撤销单笔转账异常:", e);
                    response.setSuccess(false);
                }
            } else {
                log.error("微信支付撤销单笔转账配置异常");
                response.setSuccess(false);
            }
        } catch (Exception e) {
            log.error("获取微信支付配置异常", e);
            response.setSuccess(false);
        }
        return response;
    }

    @Override
    public String getOpenid(String code) {
        try {
            TradePlatformConfig tradePlatformConfig = platformConfigService.get(PlatformConstant.WXPAY);
            if (tradePlatformConfig instanceof WxpayConfig config) {
                String getOpenIdUrl = "https://api.weixin.qq.com/sns/jscode2session?"
                        + "appid=" + config.getAppid()
                        + "&secret=" + config.getAppSecret()
                        + "&js_code=" + code + "&grant_type=authorization_code";
                RestTemplate restTemplate = new RestTemplate();
                String respResult = restTemplate.getForObject(getOpenIdUrl, String.class);
                log.info("获取openid的url: {}, respResult：{}", getOpenIdUrl,respResult);
                if (Objects.isNull(respResult) || respResult.isEmpty()) {
                    return "";
                }
                try{
                    Map<String, String> map = JSON.parseObject(respResult, Map.class);
                    String errorCode = map.get("errcode") ;
                    if (Objects.nonNull(errorCode) && !errorCode.isEmpty()) {
                        int errorCodeInt = Integer.parseInt(errorCode);
                        log.info("获取openid的errorCode, {}", errorCodeInt);
                        if (errorCodeInt != 0) {
                            return "";
                        }
                    }
                    return map.get("openid");
                } catch (Exception ex){
                    ex.printStackTrace();
                    return "";
                }
            } else {
                return "";
            }
        } catch (Exception e) {
            log.error("获取微信支付配置异常", e);
            return "";
        }
    }

    public <R, T> T run(WxpayRequestModel<R, T> wxpayRequestModel) {
        WxpayConfig wxpayConfig = wxpayRequestModel.getWxpayConfig();
        R request = wxpayRequestModel.getRequest();
        String body = null;
        if (Objects.nonNull(request)) {
            body = WXPayUtility.toJson(request);
        }
        Request.Builder reqBuilder = new Request.Builder().url(wxpayRequestModel.getHost() + wxpayRequestModel.getUri());
        reqBuilder.addHeader("Accept", "application/json");
        reqBuilder.addHeader("Wechatpay-Serial", wxpayConfig.getWechatPayPublicKeyId());
        reqBuilder.addHeader("Authorization", WXPayUtility.buildAuthorization(
                wxpayConfig.getMchid(), wxpayConfig.getCertificateSerialNo(),
                wxpayConfig.getPrivateKey(), wxpayRequestModel.getMethod(),
                wxpayRequestModel.getUri(), body));
        RequestBody requestBody = null;
        if (StringUtils.hasLength(body)) {
            reqBuilder.addHeader("Content-Type", "application/json");
            requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body);
        }
        if (Objects.isNull(requestBody) && Objects.equals(wxpayRequestModel.getMethod(), POST)) {
            reqBuilder.addHeader("Content-Type", "application/json");
            requestBody = RequestBody.create(null, "");
        }
        reqBuilder.method(wxpayRequestModel.getMethod(), requestBody);
        Request httpRequest = reqBuilder.build();

        // 发送HTTP请求
        OkHttpClient client = new OkHttpClient.Builder().build();
        try (Response httpResponse = client.newCall(httpRequest).execute()) {
            String respBody = WXPayUtility.extractBody(httpResponse);
            if (httpResponse.code() >= 200 && httpResponse.code() < 300) {
                // 2XX 成功，验证应答签名
                WXPayUtility.validateResponse(wxpayConfig.getWechatPayPublicKeyId(), wxpayConfig.getWechatPayPublicKey(),
                        httpResponse.headers(), respBody);

                // 从HTTP应答报文构建返回数据
                return WXPayUtility.fromJson(respBody, wxpayRequestModel.getClazz());
            } else {
                throw new WXPayUtility.ApiException(httpResponse.code(), respBody, httpResponse.headers());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Sending request to " + wxpayRequestModel.getUri() + " failed.", e);
        }
    }

    private static Long convertBigDecimalToFenLong(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(HUNDRED).setScale(0, RoundingMode.DOWN).longValue();
    }
}
