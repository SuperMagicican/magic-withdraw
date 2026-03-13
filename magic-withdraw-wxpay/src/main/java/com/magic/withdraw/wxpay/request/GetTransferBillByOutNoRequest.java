package com.magic.withdraw.wxpay.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * @author lgy
 * @since 2026/3/12
 */
@Data
public class GetTransferBillByOutNoRequest {

    @SerializedName("out_bill_no")
    @Expose(serialize = false)
    public String outBillNo;
}
