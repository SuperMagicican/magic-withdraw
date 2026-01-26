package com.magic.withdraw.alipay;

import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import com.magic.withdraw.core.key.KeyManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

/**
 * @author lgy
 * @since 2026/1/17
 */
@Getter
@Setter
@Accessors(chain = true)
@Component
@RequiredArgsConstructor
public class AlipayConfig extends TradePlatformConfig{

    private String userId;
    private String appId;
    private String privateKey;
    private String certPath;
    private String alipayPublicCertPath;
    private String rootCertPath;

    private final KeyManager keyManager;

    public String getPrivateKey(){
        return keyManager.getKey(privateKey);
    }

    public String getCertPath() {
        return keyManager.getCertPath(certPath);
    }

    public String getAlipayPublicCertPath() {
        return keyManager.getCertPath(alipayPublicCertPath);
    }

    public String getRootCertPath() {
        return keyManager.getCertPath(rootCertPath);
    }
}
