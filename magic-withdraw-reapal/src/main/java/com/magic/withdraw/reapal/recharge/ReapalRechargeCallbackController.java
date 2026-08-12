package com.magic.withdraw.reapal.recharge;

import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import com.magic.withdraw.core.service.PlatformConfigService;
import com.magic.withdraw.reapal.ReapalConfig;
import com.reapal.api.internal.util.ApiUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 融宝充值结果异步通知入口。 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/withdrawCallBack/notify/reapal/recharge")
public class ReapalRechargeCallbackController {

    private static final String SUCCESS = "success";
    private static final String FAILURE = "";

    private final PlatformConfigService platformConfigService;
    private final ReapalRechargeNotificationHandler notificationHandler;

    @PostMapping
    public String notify(HttpEntity<String> httpEntity) {
        try {
            String body = httpEntity.getBody();
            if (!verify(body, httpEntity.getHeaders())) {
                return FAILURE;
            }
            return notificationHandler.handle(body) ? SUCCESS : FAILURE;
        } catch (Exception e) {
            log.error("融宝充值异步通知处理失败", e);
            return FAILURE;
        }
    }

    private boolean verify(String body, HttpHeaders headers) throws Exception {
        String sign = headers.getFirst("sign");
        String merchantId = headers.getFirst("merchantId");
        ReapalConfig config = getConfig();
        return StringUtils.hasText(body)
                && StringUtils.hasText(sign)
                && StringUtils.hasText(merchantId)
                && merchantId.equals(config.getMerchantId())
                && ApiUtils.checkSignTopRequestSM(body, sign, config.getReapalPublicKey());
    }

    private ReapalConfig getConfig() {
        TradePlatformConfig config = platformConfigService.get(PlatformConstant.REAPAL);
        if (config instanceof ReapalConfig reapalConfig) {
            return reapalConfig;
        }
        throw new IllegalStateException("reapal config is null");
    }
}
