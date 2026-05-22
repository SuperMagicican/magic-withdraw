package com.magic.withdraw.core.domain.request;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/1/13
 */
@Data
@Accessors(chain = true)
public class QueryBalanceRequest implements Serializable {

    /** 登录凭证 */
    private String code;

    /** 账户类型: BASIC, OPERATION, FEES （微信必传） */
    private String accountType;
}
