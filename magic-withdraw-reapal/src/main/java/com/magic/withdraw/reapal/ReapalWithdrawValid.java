package com.magic.withdraw.reapal;

import com.alibaba.fastjson2.JSONObject;
import com.magic.withdraw.core.annotation.TradePlatform;
import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.domain.response.ValidResponse;
import com.magic.withdraw.core.exception.TradeException;
import com.magic.withdraw.core.service.ValidService;
import com.reapal.api.internal.util.ApiUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author lgy
 * @since 2026/1/15
 */
@Slf4j
@Service
@RequiredArgsConstructor
@TradePlatform(PlatformConstant.REAPAL)
public class ReapalWithdrawValid implements ValidService {

    private final ReapalWithdrawConfig reapalWithdrawConfig;

    @Override
    public ValidResponse validWithdraw(HttpEntity<String> httpEntity, HttpServletRequest request) throws TradeException {
        ValidResponse validResponse = new ValidResponse();

        try {
            HttpHeaders headers = httpEntity.getHeaders();
            String body = httpEntity.getBody();
            Map<String, Object> bodyMap = JSONObject.parseObject(body, Map.class);
            String merchantOrderNo = (String) bodyMap.getOrDefault("merchantOrderNo", "");
            String status = (String) bodyMap.getOrDefault("status", "7");
            validResponse.setOrderNo(merchantOrderNo);
            validResponse.setSuccess("6".equals(status));
            String sign = headers.getFirst("sign");
            String merchantId = headers.getFirst("merchantId");
            if (!StringUtils.hasLength(sign) || !StringUtils.hasLength(merchantId)) {
                log.error("fail:服务商号或签名数据不存在");
                validResponse.setValid(false);
                return validResponse;
            }
            String encryptKey =  headers.getFirst("encryptKey");
            if (StringUtils.hasLength(encryptKey)) {
                //暂无敏感字段
            }
            boolean verify = false;
            try {
                verify = ApiUtils.checkSignTopRequestSM(body, sign, reapalWithdrawConfig.getReapalPublicKey());
            } catch (Exception e) {
                log.error("验签失败", e);
                throw new TradeException("验签失败");
            }
            validResponse.setValid(verify);
        } catch (Exception e) {
            validResponse.setValid(false);
            return validResponse;
        }
        return validResponse;
    }

    @Override
    public String successResult() {
        return "success";
    }

    @Override
    public String failResult() {
        return "";
    }
}
