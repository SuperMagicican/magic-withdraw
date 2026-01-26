package com.magic.withdraw.alipay;

import com.alibaba.fastjson2.JSON;
import com.alipay.api.*;
import com.alipay.api.domain.AlipayFundAccountQueryModel;
import com.alipay.api.domain.AlipayFundTransCommonQueryModel;
import com.alipay.api.domain.AlipayFundTransUniTransferModel;
import com.alipay.api.domain.Participant;
import com.alipay.api.request.AlipayFundAccountQueryRequest;
import com.alipay.api.request.AlipayFundTransCommonQueryRequest;
import com.alipay.api.request.AlipayFundTransUniTransferRequest;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.response.AlipayFundAccountQueryResponse;
import com.alipay.api.response.AlipayFundTransCommonQueryResponse;
import com.alipay.api.response.AlipayFundTransUniTransferResponse;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import static com.alipay.api.AlipayConstants.*;

/**
 * @author lgy
 * @since 2026/1/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
@TradePlatform(PlatformConstant.ALIPAY)
public class AlipayWithdrawTrade implements TradeService {

    /** 支付宝网关 */
    private final static String SERVERURL = "https://openapi.alipay.com/gateway.do";
    private final static String BIZSCENE = "DIRECT_TRANSFER";
    private final static String PRODUCT_CODE = "TRANS_ACCOUNT_NO_PWD";
    private final static String IDENTITY_TYPE = "ALIPAY_LOGON_ID";
    private final static String GRANTTYPE = "authorization_code";


    private DefaultAlipayClient alipayClient;

    @Override
    public SingleWithdrawResponse singleWithdraw(SingleWithdrawRequest request) {
        SingleWithdrawResponse response = new SingleWithdrawResponse();
        AlipayFundTransUniTransferRequest alipayFundTransUniTransferRequest = new AlipayFundTransUniTransferRequest();
        AlipayFundTransUniTransferModel model = new AlipayFundTransUniTransferModel();

        model.setOutBizNo(request.getOrderNo());
        model.setTransAmount(convertBigDecimalToString(request.getAmount()));
        model.setBizScene(BIZSCENE);
        model.setProductCode(PRODUCT_CODE);
        model.setOrderTitle(request.getOrderTitle());
        Participant payeeInfo = new Participant();
        payeeInfo.setIdentity(request.getCardNo());
        payeeInfo.setName(request.getCardName());
        payeeInfo.setIdentityType(IDENTITY_TYPE);
        model.setPayeeInfo(payeeInfo);
        model.setRemark(request.getOrderTitle());
        // 设置转账业务请求的扩展参数
        model.setBusinessParams("{\"payer_show_name_use_alias\":\"true\"}");
        alipayFundTransUniTransferRequest.setBizModel(model);

        try {
            if (alipayClient == null) {
                TradePlatformConfig tradePlatformConfig = TradePlatformConfigContext.get();
                if (tradePlatformConfig instanceof AlipayConfig config) {
                    alipayClientBulider(config);
                } else {
                    log.error("阿里支付配置异常");
                    response.setSuccess(false);
                }
            }
            response.setRequestBody(JSON.toJSONString(alipayFundTransUniTransferRequest));
            AlipayFundTransUniTransferResponse alipayFundTransUniTransferResponse =
                    alipayClient.certificateExecute(alipayFundTransUniTransferRequest);
            if (Objects.equals("10000", alipayFundTransUniTransferResponse.getCode())) {
                response.setSuccess(true);
            } else {
                response.setSuccess(false);
                response.setMessage(alipayFundTransUniTransferResponse.getSubMsg());
            }
            response.setOutOrderNo(alipayFundTransUniTransferResponse.getOrderId());
            response.setOrderNo(alipayFundTransUniTransferResponse.getOutBizNo());
            response.setResponseBody(JSON.toJSONString(alipayFundTransUniTransferResponse));
        } catch (AlipayApiException e) {
            log.error("阿里支付单笔转账异常:", e);
            response.setSuccess(false);
        }
        return response;
    }

    @Override
    public QueryBalaceResponse queryBalance(QueryBalaceRequest request) {
        QueryBalaceResponse response = new QueryBalaceResponse();
        AlipayFundAccountQueryRequest alipayFundAccountQueryRequest = new AlipayFundAccountQueryRequest();
        AlipayFundAccountQueryModel model = new AlipayFundAccountQueryModel();

        try {
            // uid参数未来计划废弃，存量商户可继续使用，新商户请使用openid。请根据应用-开发配置-openid配置选择支持的字段。
            String openid = getOpenid(request.getCode());
            if (!StringUtils.hasLength(openid)) {
                TradePlatformConfig tradePlatformConfig = TradePlatformConfigContext.get();
                if (tradePlatformConfig instanceof AlipayConfig config) {
                    model.setAlipayUserId(config.getUserId());
                } else {
                    response.setSuccess(false);
                    response.setMessage("支付宝配置异常");
                    return response;
                }
            }
            model.setAlipayOpenId(openid);
            // 设置查询的账号类型
            model.setAccountType("ACCTRANS_ACCOUNT");
            alipayFundAccountQueryRequest.setBizModel(model);
            AlipayFundAccountQueryResponse alipayFundAccountQueryResponse = alipayClient.certificateExecute(alipayFundAccountQueryRequest);
            if (Objects.equals("10000", alipayFundAccountQueryResponse.getCode())) {
                response.setAvailableBalance(
                        new BigDecimal(alipayFundAccountQueryResponse.getAvailableAmount())
                                .setScale(2, RoundingMode.DOWN).longValue() * 100);
                response.setSuccess(true);
            } else {
                response.setSuccess(false);
                response.setMessage(alipayFundAccountQueryResponse.getSubMsg());
            }
        } catch (Exception e) {
            log.error("阿里支付查询余额异常:", e);
            response.setSuccess(false);
        }
        return response;
    }

    @Override
    public QueryResponse queryTradingOrderNo(String orderNo) {
        QueryResponse response = new QueryResponse();
        // 构造请求参数以调用接口
        AlipayFundTransCommonQueryRequest alipayFundTransCommonQueryRequest =
                new AlipayFundTransCommonQueryRequest();
        AlipayFundTransCommonQueryModel model = new AlipayFundTransCommonQueryModel();
        model.setProductCode(PRODUCT_CODE);
        model.setBizScene(BIZSCENE);
        model.setOutBizNo(orderNo);
        alipayFundTransCommonQueryRequest.setBizModel(model);

        try {
            if (alipayClient == null) {
                TradePlatformConfig tradePlatformConfig = TradePlatformConfigContext.get();
                if (tradePlatformConfig instanceof AlipayConfig config) {
                    alipayClientBulider(config);
                } else {
                    log.error("阿里支付配置异常");
                    response.setSuccess(false);
                }
            }
            AlipayFundTransCommonQueryResponse alipayFundTransCommonQueryResponse =
                    alipayClient.certificateExecute(alipayFundTransCommonQueryRequest);
            response.setResponseBody(JSON.toJSONString(alipayFundTransCommonQueryResponse));
            if (Objects.equals("10000", alipayFundTransCommonQueryResponse.getCode())) {
                response.setSuccess(true);
                response.setOrderStatus(alipayFundTransCommonQueryResponse.getStatus());
                response.setFailReason(alipayFundTransCommonQueryResponse.getFailReason());
            } else {
                response.setSuccess(false);
                response.setMessage(alipayFundTransCommonQueryResponse.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("阿里支付查询订单异常:", e);
            response.setSuccess(false);
        }
        return response;
    }

    @Override
    public String getOpenid(String code) {
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setCode(code);
        request.setGrantType(GRANTTYPE);
        try {
            if (alipayClient == null) {
                TradePlatformConfig tradePlatformConfig = TradePlatformConfigContext.get();
                if (tradePlatformConfig instanceof AlipayConfig config) {
                    alipayClientBulider(config);
                } else {
                    return "";
                }
            }
            AlipaySystemOauthTokenResponse oauthTokenResponse = alipayClient.certificateExecute(request);
            return oauthTokenResponse.getOpenId();
        } catch (Exception e) {
            //处理异常
            e.printStackTrace();
            return "";
        }
    }

    private void alipayClientBulider(AlipayConfig config) throws AlipayApiException{
        CertAlipayRequest certAlipayRequest = new CertAlipayRequest();
        certAlipayRequest.setServerUrl(SERVERURL);
        certAlipayRequest.setAppId(config.getAppId());
        certAlipayRequest.setPrivateKey(config.getPrivateKey());
        certAlipayRequest.setFormat("json");
        certAlipayRequest.setCharset(CHARSET_UTF8);
        certAlipayRequest.setSignType(SIGN_TYPE_RSA2);
        certAlipayRequest.setCertPath(config.getCertPath());
        certAlipayRequest.setAlipayPublicCertPath(config.getAlipayPublicCertPath());
        certAlipayRequest.setRootCertPath(config.getRootCertPath());
        alipayClient = new DefaultAlipayClient(certAlipayRequest);
    }

    public static String convertBigDecimalToString(BigDecimal amountBigDecimal) {
        if (amountBigDecimal == null) {
            return "0.00";
        }
        return amountBigDecimal.setScale(2, RoundingMode.DOWN).toString();
    }
}
