package com.magic.withdraw.core.service.impl;

import com.magic.withdraw.core.domain.bean.ProcessingOrder;
import com.magic.withdraw.core.service.ProcessingOrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author lgy
 * @since 2026/1/22
 */
@Component
@ConditionalOnMissingBean(ProcessingOrderService.class)
public class InMemoryProcessingOrderServiceImpl implements ProcessingOrderService {

    private final CopyOnWriteArraySet<ProcessingOrder> processingOrderSet = new CopyOnWriteArraySet<>();

    @Override
    public void init() {

    }

    @Override
    public void add(ProcessingOrder processingOrder) {
        processingOrderSet.add(processingOrder);
    }

    @Override
    public void remove(ProcessingOrder processingOrder) {
        processingOrderSet.remove(processingOrder);

    }

    @Override
    public Collection<ProcessingOrder> list() {
        return processingOrderSet;
    }
}
