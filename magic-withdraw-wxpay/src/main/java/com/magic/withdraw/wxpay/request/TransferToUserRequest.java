package com.magic.withdraw.wxpay.request;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lgy
 * @since 2026/3/11
 */
public class TransferToUserRequest {

    @SerializedName("appid")
    public String appid;

    @SerializedName("out_bill_no")
    public String outBillNo;

    @SerializedName("transfer_scene_id")
    public String transferSceneId;

    @SerializedName("openid")
    public String openid;

    @SerializedName("user_name")
    public String userName;

    @SerializedName("transfer_amount")
    public Long transferAmount;

    @SerializedName("transfer_remark")
    public String transferRemark;

    @SerializedName("notify_url")
    public String notifyUrl;

    @SerializedName("user_recv_perception")
    public String userRecvPerception;

    @SerializedName("transfer_scene_report_infos")
    public List<TransferSceneReportInfo> transferSceneReportInfos = new ArrayList<TransferSceneReportInfo>();
}
