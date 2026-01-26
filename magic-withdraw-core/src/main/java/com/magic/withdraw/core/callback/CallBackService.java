package com.magic.withdraw.core.callback;

import com.magic.withdraw.core.domain.bean.WithdrawResult;

/**
 * 回调接口
 *
 * @author liguangyuan
 */
public interface CallBackService
{
    /**
     * 成功提现--处理业务逻辑
     */
    void successWithdraw(WithdrawResult withdrawResult);

    /**
     * 失败提现-处理业务逻辑
     */
    void failWithdraw(WithdrawResult withdrawResult);
}
