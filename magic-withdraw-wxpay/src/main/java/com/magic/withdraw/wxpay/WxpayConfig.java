package com.magic.withdraw.wxpay;

import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author lgy
 * @since 2026/3/11
 */
@Getter
@Setter
@Accessors(chain = true)
public class WxpayConfig extends TradePlatformConfig {

    private String appSecret;
    private String appid;
    /** 商户号，是由微信支付系统生成并分配给每个商户的唯一标识符，商户号获取方式参考 https://pay.weixin.qq.com/doc/v3/merchant/4013070756 */
    private String mchid;
    /** 商户API证书序列号，如何获取请参考 https://pay.weixin.qq.com/doc/v3/merchant/4013053053 */
    private String certificateSerialNo;
    /** 商户API证书私钥文件路径，本地文件路径 */
    private PrivateKey privateKey;
    /** 微信支付公钥ID，如何获取请参考 https://pay.weixin.qq.com/doc/v3/merchant/4013038816 */
    private String wechatPayPublicKeyId;
    /** 微信支付公钥文件路径，本地文件路径 */
    private PublicKey wechatPayPublicKey;
    /** 固定值：1000（现金营销） 1006 企业报销 */
    private String transferSceneId = "1000";

    public WxpayConfig() {
    }

    public WxpayConfig(String appSecret, String appid, String mchid, String certificateSerialNo, String privateKeyFileName, String wechatPayPublicKeyId, String wechatPayPublicKeyFileName) {
        this.appSecret = appSecret;
        this.appid = appid;
        this.mchid = mchid;
        this.certificateSerialNo = certificateSerialNo;
        this.privateKey = WXPayUtility.loadPrivateKeyFromPath(privateKeyFileName);
        this.wechatPayPublicKeyId = wechatPayPublicKeyId;
        this.wechatPayPublicKey = WXPayUtility.loadPublicKeyFromPath(wechatPayPublicKeyFileName);
    }
}
