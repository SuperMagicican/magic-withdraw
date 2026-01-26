package com.magic.withdraw.core.domain.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/1/22
 */
@Data
@Accessors(chain = true)
public class WithdrawResult implements Serializable {

    private String orderNo;

    private String queryBody;

    private String callBackBody;

    private String failReason;
}
