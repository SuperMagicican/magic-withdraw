package com.magic.withdraw.core.domain.bean;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author lgy
 * @since 2026/1/22
 */
@Component
@ConfigurationProperties("magic.withdraw.callback")
@Data
public class CallbackConfig {

    private String domain;

    @Value("${magic.withdraw.callback.watch:true}")
    private boolean enabled;

    @Value("${magic.withdraw.callback.cycle:10}")
    private long cycle;
}
