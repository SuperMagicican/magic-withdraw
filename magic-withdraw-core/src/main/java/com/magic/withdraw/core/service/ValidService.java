package com.magic.withdraw.core.service;

import com.magic.withdraw.core.domain.response.ValidResponse;
import com.magic.withdraw.core.exception.TradeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;

/**
 * @author lgy
 * @since 2026/1/13
 */
public interface ValidService {

    ValidResponse validWithdraw(HttpEntity<String> httpEntity, HttpServletRequest request) throws TradeException;

    String successResult();

    String failResult();
}
