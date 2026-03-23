package com.magic.withdraw.wxpay.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/3/19
 */
@Data
public class CancelTransferRequest implements Serializable {

    @SerializedName("out_bill_no")
    @Expose(serialize = false)
    public String outBillNo;
}
