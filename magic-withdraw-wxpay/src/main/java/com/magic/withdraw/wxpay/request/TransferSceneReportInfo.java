package com.magic.withdraw.wxpay.request;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * @author lgy
 * @since 2026/3/11
 */
public class TransferSceneReportInfo implements Serializable {

    @SerializedName("info_type")
    public String infoType;

    @SerializedName("info_content")
    public String infoContent;
}
