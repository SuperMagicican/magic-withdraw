package com.magic.withdraw.wxpay.enums;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/3/11
 */
public enum TransferBillStatus implements Serializable {

    @SerializedName("ACCEPTED")
    ACCEPTED,
    @SerializedName("PROCESSING")
    PROCESSING,
    @SerializedName("WAIT_USER_CONFIRM")
    WAIT_USER_CONFIRM,
    @SerializedName("TRANSFERING")
    TRANSFERING,
    @SerializedName("SUCCESS")
    SUCCESS,
    @SerializedName("FAIL")
    FAIL,
    @SerializedName("CANCELING")
    CANCELING,
    @SerializedName("CANCELLED")
    CANCELLED
}
