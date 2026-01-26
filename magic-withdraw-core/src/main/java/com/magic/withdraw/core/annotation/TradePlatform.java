package com.magic.withdraw.core.annotation;

import java.lang.annotation.*;

/**
 * 提现平台注解
 * @author lgy
 * @since 2026/1/13
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TradePlatform {

    String value();
}
