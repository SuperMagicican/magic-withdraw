package com.magic.withdraw.core.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/1/22
 */
@Data
public class QueryResponse implements Serializable {

    private boolean success;

    private String message;

    private String orderStatus;

    private String failReason;

    private String responseBody;
}
