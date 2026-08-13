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

    /** 充值交互模式，默认企业网银直连 */
    private RechargeMode rechargeMode = RechargeMode.CASHIER;

    /** 企业网银直连时使用的充值银行编码 */
    private String rechargeBankNo;

    /** 充值外部会员号 */
    private String memberId;

    /** 充值用户IP */
    private String memberIp;

    /** 网银支付完成后的同步跳转地址 */
    private String returnUrl;

    /** 充值订单主动查询间隔，单位秒。 */
    private long rechargeQueryInterval = 10;

    /** 充值订单主动查询总时长，单位秒。 */
    private long rechargeQueryTimeout = 1800;

    public enum RechargeMode {
        B2B_DIRECT,
        CASHIER
    }
}
