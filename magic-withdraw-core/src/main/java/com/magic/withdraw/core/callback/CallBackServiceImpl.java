package com.magic.withdraw.core.callback;

import com.magic.withdraw.core.domain.bean.WithdrawResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * @author lgy
 * @since 2026/1/15
 */
@Slf4j
@Service
@ConditionalOnMissingBean(CallBackService.class)
public class CallBackServiceImpl implements CallBackService{

    @Override
    public void successWithdraw(WithdrawResult withdrawResult) {
        log.info("提现成功！{}", withdrawResult);
    }

    @Override
    public void failWithdraw(WithdrawResult withdrawResult) {
        log.info("提现失败！{}", withdrawResult);
    }
}
