package com.magic.withdraw.core.service;

import com.magic.withdraw.core.domain.bean.TradePlatformConfig;

/**
 * @author lgy
 * @since 2026/3/14
 */
public interface PlatformConfigService {

    void set(String platform, TradePlatformConfig config);

    TradePlatformConfig get(String platform);
}
