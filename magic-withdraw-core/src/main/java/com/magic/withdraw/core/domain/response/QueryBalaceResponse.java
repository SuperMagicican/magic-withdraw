package com.magic.withdraw.core.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/1/13
 */
@Data
public class QueryBalaceResponse implements Serializable {

    /** 是否成功 */
    private boolean success;

    /** 响应信息 */
    private String message;

    /** 可用余额 (单位:分) */
    private Long availableBalance;


}
