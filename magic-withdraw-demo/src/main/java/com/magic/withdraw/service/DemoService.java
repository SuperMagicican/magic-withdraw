package com.magic.withdraw.service;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.context.TradePlatformConfigContext;
import com.magic.withdraw.core.domain.request.CancelRequest;
import com.magic.withdraw.core.domain.request.QueryBalanceRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.CancelResponse;
import com.magic.withdraw.core.domain.response.QueryBalanceResponse;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.core.key.KeyManager;
import com.magic.withdraw.core.service.PlatformConfigService;
import com.magic.withdraw.core.service.WithdrawService;
import com.magic.withdraw.core.utils.FileUtil;
import com.magic.withdraw.reapal.ReapalConfig;
import com.magic.withdraw.wxpay.WxpayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author lgy
 * @since 2026/1/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoService {

    private final WithdrawService withdrawService;
    private final KeyManager keyManager;
    private final PlatformConfigService platformConfigService;

    public void queryBalance() {
        ReapalConfig reapalConfig = new ReapalConfig();

        reapalConfig.setOpenApiDomain("https://testopenapi.reapal.com:8443");
        reapalConfig.setMerchantId("100000001600411");
        reapalConfig.setCustomerId("100000001600411");
        reapalConfig.setSignId("2f742193b1e5c862bdf1c8f115bae3802bef3249");
        reapalConfig.setPrivateKey(FileUtil.copyResourceToTempFile("2f742193b1e5c862bdf1c8f115bae3802bef3249_itrus.sm2.pfx"));
        reapalConfig.setPrivateKeyPwd("123321");
        reapalConfig.setEncryptId("46A0936B22F446936E018ED93A0A49A6D9D8A75F");
        reapalConfig.setReapalPublicKey(FileUtil.copyResourceToTempFile("test.sm2.cer"));

        platformConfigService.set(PlatformConstant.REAPAL, reapalConfig);
        QueryBalanceRequest request = new QueryBalanceRequest();
        QueryBalanceResponse response = withdrawService.queryBalance(request, "测试_reapal");
        log.info("查询余额响应：{}", JSON.toJSONString(response));
    }

    public void singleWithdraw() {
        WxpayConfig wxpayConfig = new WxpayConfig(
                "21352098791e4379e555bd6acd3b3d4c",
                "wxab761348763b6b33",
                "1651221292",
                "77A21F4B976F29434F6FD5FE811B1559C1CAD885",
                keyManager.getCertPath("apiclient_key.pem"),
                "PUB_KEY_ID_0116512212922026031200182085001400",
                keyManager.getCertPath("pub_key.pem")
        );
        TradePlatformConfigContext.set(wxpayConfig);
        SingleWithdrawRequest singleWithdrawRequest = new SingleWithdrawRequest();
        singleWithdrawRequest.setOpenid("o99tk13ZiBUSFfw3dhlSLM1ck5vQ");
        singleWithdrawRequest.setAmount(new BigDecimal("1"));
        singleWithdrawRequest.setOrderNo("202603131039");
        singleWithdrawRequest.setCardName("肖伟");
        singleWithdrawRequest.setOrderTitle("测试微信单笔转账");
        singleWithdrawRequest.setNotifyUrl(null);
        SingleWithdrawResponse singleWithdrawResponse = withdrawService.singleWithdraw(singleWithdrawRequest, "wxpay");
        log.info("单笔转账响应：{}", singleWithdrawResponse);
        log.info("单笔转账响应：{}", JSON.toJSONString(singleWithdrawResponse));
    }

    public void queryTradingOrderNo() {
        WxpayConfig wxpayConfig = new WxpayConfig(
                "21352098791e4379e555bd6acd3b3d4c",
                "wxab761348763b6b33",
                "1651221292",
                "77A21F4B976F29434F6FD5FE811B1559C1CAD885",
                keyManager.getCertPath("apiclient_key.pem"),
                "PUB_KEY_ID_0116512212922026031200182085001400",
                keyManager.getCertPath("pub_key.pem")
        );
        TradePlatformConfigContext.set(wxpayConfig);
        QueryResponse response = withdrawService.queryTradingOrderNo("202603131039", "wxpay");
        log.info("查询微信单笔转账响应：{}", JSON.toJSONString(response));
    }

    public void cancelWithdraw() {
        WxpayConfig wxpayConfig = new WxpayConfig(
                "21352098791e4379e555bd6acd3b3d4c",
                "wxab761348763b6b33",
                "1651221292",
                "77A21F4B976F29434F6FD5FE811B1559C1CAD885",
                keyManager.getCertPath("apiclient_key.pem"),
                "PUB_KEY_ID_0116512212922026031200182085001400",
                keyManager.getCertPath("pub_key.pem")
        );
        platformConfigService.set(PlatformConstant.WXPAY, wxpayConfig);
        CancelResponse response = withdrawService.cancelWithdraw(new CancelRequest().setOrderNo("WR1232065311457411072"), "wxpay");
        log.info("查询微信撤销单笔转账响应：{}", JSON.toJSONString(response));
    }


    public void reapalSingleWithdraw() {
        setReapalConfig();
        SingleWithdrawRequest singleWithdrawRequest = new SingleWithdrawRequest();
        singleWithdrawRequest.setAmount(new BigDecimal("1"));
        singleWithdrawRequest.setOrderNo("20260514111");
        singleWithdrawRequest.setCardNo("6212260200133751211");
        singleWithdrawRequest.setCardName("王月");
        singleWithdrawRequest.setBankNo("0102");
        singleWithdrawRequest.setAccountType(SingleWithdrawRequest.EnumAccountType.PERSONAL.getCode());
        SingleWithdrawResponse singleWithdrawResponse = withdrawService.singleWithdraw(singleWithdrawRequest, "测试_reapal");
        log.info("融宝单笔代付响应：{}", JSON.toJSONString(singleWithdrawResponse));
    }

    public void reapalQueryTradingOrderNo() {
        setReapalConfig();
        QueryResponse response = withdrawService.queryTradingOrderNo("20260514111", "测试_reapal");
        log.info("融宝代付查询响应：{}", JSON.toJSONString(response));
    }

    private void setReapalConfig() {
        ReapalConfig reapalConfig = new ReapalConfig();
        reapalConfig.setOpenApiDomain("https://testopenapi.reapal.com:8443");
        reapalConfig.setMerchantId("100000001600411");
        reapalConfig.setCustomerId("100000001600411");
        reapalConfig.setReapalPublicKey(FileUtil.copyResourceToTempFile("test.sm2.cer"));
        reapalConfig.setPrivateKey(FileUtil.copyResourceToTempFile("2f742193b1e5c862bdf1c8f115bae3802bef3249_itrus.sm2.pfx"));
        reapalConfig.setPrivateKeyPwd("123321");
        reapalConfig.setEncryptId("46A0936B22F446936E018ED93A0A49A6D9D8A75F");
        reapalConfig.setSignId("2f742193b1e5c862bdf1c8f115bae3802bef3249");
        platformConfigService.set(PlatformConstant.REAPAL, reapalConfig);
    }
}
