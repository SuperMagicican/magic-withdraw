package com.magic.withdraw.reapal;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** 融宝单笔代付响应扩展数据。 */
@Data
@Accessors(chain = true)
public class ReapalSingleWithdrawData implements Serializable {

    /** 订单代付提交链路的当前阶段。 */
    private SubmitStage submitStage;

    /** 充值商户订单号。 */
    private String rechargeOrderNo;

    /** 融宝充值订单号。 */
    private String rechargeOutOrderNo;

    /** 融宝代付下单返回的应充值金额，单位：分。 */
    private Long rechargeAmount;

    /** 融宝充值订单状态。 */
    private String rechargeStatus;

    /** 融宝返回的网银或收银台付款地址。 */
    private String paymentUrl;

    /** 融宝返回的付款令牌。 */
    private String paymentToken;

    /** 融宝充值请求报文。 */
    private String rechargeRequestBody;

    /** 融宝充值响应报文；对账时同时保留提交和查询响应。 */
    private String rechargeResponseBody;

    /**
     * 从通用单笔代付响应中获取融宝扩展数据。
     * 支持直接存放的融宝实体，以及 JSON 反序列化后得到的 Map。
     *
     * @param response 通用单笔代付响应
     * @return 融宝单笔代付响应扩展数据
     * @throws IllegalArgumentException 响应或平台扩展数据为空时抛出
     */
    public static ReapalSingleWithdrawData from(SingleWithdrawResponse response) {
        if (response == null || response.getPlatformData() == null) {
            throw new IllegalArgumentException("response does not contain Reapal platform data");
        }
        Object platformData = response.getPlatformData();
        if (platformData instanceof ReapalSingleWithdrawData data) {
            return data;
        }
        return JSON.parseObject(JSON.toJSONString(platformData), ReapalSingleWithdrawData.class);
    }

    /** 融宝订单代付提交阶段。 */
    public enum SubmitStage {

        /** 本地参数或融宝配置校验未通过，尚未发起代付。 */
        VALIDATION_FAILED,

        /** 融宝未受理代付订单。 */
        PAYOUT_REJECTED,

        /** 融宝已受理代付订单，准备提交关联充值。 */
        PAYOUT_ACCEPTED,

        /** 融宝明确拒绝或关闭关联充值订单。 */
        RECHARGE_REJECTED,

        /** 关联充值结果暂时无法确认，禁止盲目重复提交。 */
        RECHARGE_UNKNOWN,

        /** 关联充值订单已受理，不代表银行卡已经到账。 */
        RECHARGE_ACCEPTED
    }
}
