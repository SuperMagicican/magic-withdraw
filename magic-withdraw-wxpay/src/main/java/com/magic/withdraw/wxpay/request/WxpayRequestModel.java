package com.magic.withdraw.wxpay.request;

import com.magic.withdraw.wxpay.WxpayConfig;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * R 请求数据
 * T 响应数据
 *
 * @author lgy
 * @since 2026/3/12
 */
@Data
@Accessors(chain = true)
public class WxpayRequestModel<R, T> implements Serializable {
    private R request;
    private String method;
    private String host;
    private String uri;
    private Class<T> clazz;
    /** 微信配置 */
    private WxpayConfig wxpayConfig;
}
