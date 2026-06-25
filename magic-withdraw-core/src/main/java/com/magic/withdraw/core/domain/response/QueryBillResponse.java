package com.magic.withdraw.core.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/6/25
 */
@Data
public class QueryBillResponse implements Serializable {

    private boolean success;

    private String message;

    private String billDownloadUrl;

    private String billFileCode;

    private String requestBody;

    private String responseBody;
}
