package com.magic.withdraw.reapal;

import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/1/14
 */
@Getter
@Setter
@Accessors(chain = true)
public class ReapalConfig
        extends TradePlatformConfig implements Serializable {

    private String openApiDomain;
    private String merchantId;
    private String customerId;
    private String signType = "SM2";
    private String signId;
    private String privateKey;
    private String privateKeyPwd;
    private String encryptType = "SM4";
    private String encryptId;
    private String reapalPublicKey;

}
