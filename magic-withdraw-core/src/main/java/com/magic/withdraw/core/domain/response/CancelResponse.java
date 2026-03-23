package com.magic.withdraw.core.domain.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/3/19
 */
@Data
@Accessors(chain = true)
public class CancelResponse implements Serializable {

    private boolean success;
}
