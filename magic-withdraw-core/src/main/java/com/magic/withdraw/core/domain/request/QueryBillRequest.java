package com.magic.withdraw.core.domain.request;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/6/25
 */
@Data
@Accessors(chain = true)
public class QueryBillRequest implements Serializable {

    private String billType;

    private String billDate;

    private String smid;
}
