package com.magic.withdraw.core.context;

import com.magic.withdraw.core.domain.bean.TradePlatformConfig;

/**
 * 上下文注入认证信息
 * @author lgy
 * @since 2026/1/13
 */
public class TradePlatformConfigContext {

    private static final ThreadLocal<TradePlatformConfig> CONTEXT = new ThreadLocal<>();

    public static void set(TradePlatformConfig credential) {
        CONTEXT.set(credential);
    }

    public static TradePlatformConfig get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
