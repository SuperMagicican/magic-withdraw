package com.magic.withdraw.core.callback;

import com.magic.withdraw.core.constants.WithdrawConstant;
import com.magic.withdraw.core.domain.bean.WithdrawResult;
import com.magic.withdraw.core.domain.response.ValidResponse;
import com.magic.withdraw.core.loader.MagicLoader;
import com.magic.withdraw.core.service.ValidService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lgy
 * @since 2026/1/15
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(WithdrawConstant.CALLBACK_PATH)
public class CallbackController {

    private final CallBackService callBackService;

    @RequestMapping(WithdrawConstant.NOTIFY + "/{platform}")
    public String notify(HttpEntity<String> httpEntity, HttpServletRequest request,
                         HttpServletResponse response, @PathVariable String platform) {
        ValidService valid = MagicLoader.getValidService(platform);
        try {
            ValidResponse validResponse = valid.validWithdraw(httpEntity, request);
            String orderNo = validResponse.getOrderNo();
            if(validResponse.isValid() && validResponse.isSuccess()){
                callBackService.successWithdraw(
                        new WithdrawResult()
                                .setCallBackBody(httpEntity.getBody())
                                .setOrderNo(orderNo));
                return valid.successResult();
            } else {
                callBackService.failWithdraw(
                        new WithdrawResult()
                                .setCallBackBody(httpEntity.getBody())
                                .setOrderNo(orderNo));
                return valid.failResult();
            }
        }catch (Exception e){
            log.error("支付回调处理失败",e);
            return valid.failResult();
        }
    }
}
