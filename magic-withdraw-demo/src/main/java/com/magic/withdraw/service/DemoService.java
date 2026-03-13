package com.magic.withdraw.service;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.context.TradePlatformConfigContext;
import com.magic.withdraw.core.domain.request.QueryBalanceRequest;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.core.key.KeyManager;
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

    public void queryBalance() {
        ReapalConfig reapalConfig = new ReapalConfig();

        reapalConfig.setOpenApiDomain("https://testopenapi.reapal.com:8443/member/merchant/account/balance");
        reapalConfig.setMerchantId("100000001600411");
        reapalConfig.setCustomerId("100000001600411");
        reapalConfig.setSignId("2f742193b1e5c862bdf1c8f115bae3802bef3249");
        reapalConfig.setPrivateKey(FileUtil.copyResourceToTempFile("2f742193b1e5c862bdf1c8f115bae3802bef3249_itrus.sm2.pfx"));
        reapalConfig.setPrivateKeyPwd("123321");
        reapalConfig.setEncryptId("46A0936B22F446936E018ED93A0A49A6D9D8A75F");
        reapalConfig.setReapalPublicKey(FileUtil.copyResourceToTempFile("test.sm2.cer"));


        TradePlatformConfigContext.set(reapalConfig);
        QueryBalanceRequest request = new QueryBalanceRequest();
        withdrawService.queryBalance(request, "测试_reapal");
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
        singleWithdrawRequest.setCode("0a1o62Ga1rL7lL0jRvFa177Z041o62GD");
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


}
