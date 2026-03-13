package com.magic.withdraw.wxpay;

/**
 * @author lgy
 * @since 2026/3/12
 */
public class WxpayConstant {

    public final static String GET = "GET";

    public final static String POST = "POST";

    public final static String HOST = "https://api.mch.weixin.qq.com";

    public final static String TRANSFER_TO_USER_PATH = "/v3/fund-app/mch-transfer/transfer-bills";

    public final static String GET_TRANSFER_BILL_BY_OUT_NO_PATH = "/v3/fund-app/mch-transfer/transfer-bills/out-bill-no/{out_bill_no}";
}
