package com.magic.withdraw.core.service;

import com.magic.withdraw.core.domain.bean.ProcessingOrder;

import java.util.Collection;

/**
 * @author lgy
 * @since 2026/1/22
 */
public interface ProcessingOrderService {

    void add(ProcessingOrder dto);

    void remove(ProcessingOrder dto);

    Collection<ProcessingOrder> list();

    void init();
}
