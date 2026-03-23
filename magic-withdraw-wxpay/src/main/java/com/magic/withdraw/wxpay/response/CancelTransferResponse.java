package com.magic.withdraw.wxpay.response;

import com.google.gson.annotations.SerializedName;
import com.magic.withdraw.wxpay.enums.TransferBillStatus;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/3/19
 */
@Data
public class CancelTransferResponse implements Serializable {

    @SerializedName("out_bill_no")
    public String outBillNo;

    @SerializedName("transfer_bill_no")
    public String transferBillNo;

    @SerializedName("state")
    public TransferBillStatus state;

    @SerializedName("update_time")
    public String updateTime;
}
