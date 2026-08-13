package com.magic.withdraw.reapal;

import com.reapal.api.Client;
import com.reapal.api.DefaultClient;

/**
 * 融宝客户端工厂。
 * 每次调用均创建独立客户端，避免多商户并发时共享可变配置。
 */
public class ReapalClientFactory {

    public Client create(ReapalConfig config) {
        com.reapal.api.ReapalConfig clientConfig = new com.reapal.api.ReapalConfig();
        clientConfig.setServerUrl(normalizeOpenApiDomain(config.getOpenApiDomain()));
        clientConfig.setMerchantId(config.getMerchantId());
        clientConfig.setSignType(config.getSignType());
        clientConfig.setSignId(config.getSignId());
        clientConfig.setReapalPublicCertPath(config.getReapalPublicKey());
        clientConfig.setMerchantprivateCertPath(config.getPrivateKey());
        clientConfig.setMerchantprivateCertPwd(config.getPrivateKeyPwd());
        clientConfig.setEncryptId(config.getEncryptId());
        clientConfig.setEncryptType(config.getEncryptType());
        return new DefaultClient(clientConfig);
    }

    private static String normalizeOpenApiDomain(String openApiDomain) {
        if (openApiDomain == null) {
            return null;
        }
        return openApiDomain
                .replace("/dforder/df/singleTrade", "")
                .replace("/dforder/df/batchTrade", "")
                .replace("/dforder/df/query", "")
                .replace("/order/trade", "")
                .replace("/order/query", "")
                .replace("/member/merchant/account/balance", "")
                .replaceAll("/+$", "");
    }
}
