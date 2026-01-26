package com.magic.withdraw.core.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 验证签名
 *
 * @author lgy
 * @since 2026/1/15
 */
@Data
public class ValidResponse implements Serializable {

    /** 是否通过验签 */
    private boolean isValid;

    /** 订单号 */
    private String orderNo;

    /** 是否成功 */
    private boolean success;
}
