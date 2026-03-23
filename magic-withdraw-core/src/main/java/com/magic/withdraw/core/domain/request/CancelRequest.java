package com.magic.withdraw.core.domain.request;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/3/19
 */
@Data
@Accessors(chain = true)
public class CancelRequest implements Serializable {

    /** 订单号 */
    private String orderNo;
}
