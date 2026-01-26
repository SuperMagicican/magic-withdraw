package com.magic.withdraw.core.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * 交易异常
 *
 * @author lgy
 * @since 2026/1/13
 */
@Getter
@Setter
public class TradeException extends RuntimeException
{
    private String code;

    private String msg;

    public TradeException(String code, String msg)
    {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public TradeException(String msg)
    {
        super(msg);
    }
}
