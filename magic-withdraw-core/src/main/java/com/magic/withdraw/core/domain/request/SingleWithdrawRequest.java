package com.magic.withdraw.core.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author lgy
 * @since 2026/1/13
 */
@Data
@Accessors(chain = true)
public class SingleWithdrawRequest implements Serializable {

    private String orderNo;
    private String cardNo;
    private String cardName;
    private Integer accountType;
    /** 开户行行号 */
    private String bankNo;
    /** 金额 (单位:分) */
    private BigDecimal amount;
    private String notifyUrl;
    /** 转账标题 */
    private String orderTitle;

    @Getter
    @AllArgsConstructor
    public enum EnumAccountType {

        COMPANY(1, "对公"),
        PERSONAL(2, "对私");

        private final Integer code;

        private final String desc;
    }
}
