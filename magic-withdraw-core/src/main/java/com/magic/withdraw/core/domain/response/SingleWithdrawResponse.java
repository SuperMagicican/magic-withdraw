package com.magic.withdraw.core.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/1/13
 */
@Data
public class SingleWithdrawResponse implements Serializable {

    /** 是否成功 */
    private boolean success;

    /** 响应信息 */
    private String message;

    /** 订单号 */
    private String orderNo;

    /** 外部订单号 */
    private String outOrderNo;

    /** 确认收款参数字段 （目前是微信独有）*/
    private String packageInfo;

    private String requestBody;

    private String responseBody;

    /** 耗时 ms */
    private Integer cost;
}
