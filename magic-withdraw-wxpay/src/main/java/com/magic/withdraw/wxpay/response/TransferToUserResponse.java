package com.magic.withdraw.wxpay.response;

import com.google.gson.annotations.SerializedName;
import com.magic.withdraw.wxpay.enums.TransferBillStatus;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/3/11
 */
@Data
public class TransferToUserResponse implements Serializable {

    @SerializedName("out_bill_no")
    public String outBillNo;

    @SerializedName("transfer_bill_no")
    public String transferBillNo;

    @SerializedName("create_time")
    public String createTime;

    @SerializedName("state")
    public TransferBillStatus state;

    @SerializedName("package_info")
    public String packageInfo;
}
