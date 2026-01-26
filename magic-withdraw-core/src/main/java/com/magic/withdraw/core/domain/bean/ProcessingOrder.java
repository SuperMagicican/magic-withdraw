package com.magic.withdraw.core.domain.bean;

import lombok.Data;

/**
 * @author lgy
 * @since 2026/1/22
 */
@Data
public class ProcessingOrder {
    /** 订单号 */
    private String orderNo;

    /** 平台 */
    private String platform;
}
