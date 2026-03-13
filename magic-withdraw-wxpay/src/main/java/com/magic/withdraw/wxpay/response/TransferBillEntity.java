package com.magic.withdraw.wxpay.response;

import com.google.gson.annotations.SerializedName;
import com.magic.withdraw.wxpay.enums.TransferBillStatus;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/3/12
 */
@Data
public class TransferBillEntity implements Serializable {

    @SerializedName("mch_id")
    public String mchId;

    @SerializedName("out_bill_no")
    public String outBillNo;

    @SerializedName("transfer_bill_no")
    public String transferBillNo;

    @SerializedName("appid")
    public String appid;

    @SerializedName("state")
    public TransferBillStatus state;

    @SerializedName("transfer_amount")
    public Long transferAmount;

    @SerializedName("transfer_remark")
    public String transferRemark;

    @SerializedName("fail_reason")
    public String failReason;

    @SerializedName("openid")
    public String openid;

    @SerializedName("user_name")
    public String userName;

    @SerializedName("create_time")
    public String createTime;

    @SerializedName("update_time")
    public String updateTime;
}
