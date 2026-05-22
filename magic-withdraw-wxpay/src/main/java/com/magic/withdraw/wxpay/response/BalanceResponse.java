package com.magic.withdraw.wxpay.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/5/22
 */
@Data
public class BalanceResponse implements Serializable {

    @SerializedName("available_amount")
    private Long availableAmount;

    @SerializedName("pending_amount")
    private Long pendingAmount;
}
