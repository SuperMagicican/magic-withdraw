package com.magic.withdraw.core.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 单笔代付响应。
 *
 * @author lgy
 * @since 2026/1/13
 */
@Data
public class SingleWithdrawResponse implements Serializable {

    /** 是否成功。 */
    private boolean success;

    /** 响应信息。 */
    private String message;

    /** 商户订单号。 */
    private String orderNo;

    /** 支付平台订单号。 */
    private String outOrderNo;

    /** 支付平台专属扩展数据，由对应平台模块定义具体类型。 */
    private Object platformData;

    /** 是否在本次提交完成后立即加入订单状态巡检，默认加入。 */
    private boolean pollingRequired = true;

    /** 确认收款参数字段，目前为微信专用。 */
    private String packageInfo;

    /** 支付平台请求报文。 */
    private String requestBody;

    /** 支付平台响应报文。 */
    private String responseBody;

    /** 请求耗时，单位：毫秒。 */
    private Integer cost;
}
