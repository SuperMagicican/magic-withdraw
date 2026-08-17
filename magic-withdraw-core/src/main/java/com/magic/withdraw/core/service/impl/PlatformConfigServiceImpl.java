package com.magic.withdraw.core.service.impl;

import com.magic.withdraw.core.domain.bean.TradePlatformConfig;
import com.magic.withdraw.core.exception.TradeException;
import com.magic.withdraw.core.service.PlatformConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author lgy
 * @since 2026/3/14
 */
@Slf4j
@Service
public class PlatformConfigServiceImpl implements PlatformConfigService {

    private final ConcurrentMap<String, TradePlatformConfig> platformConfigMap =
            new ConcurrentHashMap<>();

    @Override
    public void set(String platform, TradePlatformConfig config) {
        platformConfigMap.put(platform, config);
    }

    @Override
    public TradePlatformConfig get(String platform) {
        TradePlatformConfig tradePlatformConfig = platformConfigMap.get(platform);
        if (tradePlatformConfig == null) {
            throw new TradeException("获取" + platform + "配置失败");
        }
        return tradePlatformConfig;
    }
}

