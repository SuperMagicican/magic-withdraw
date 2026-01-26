package com.magic.withdraw.core.key;

/**
 * @author lgy
 * @since 2026/1/17
 */
public interface KeyManager {

    String getKey(String name);

    String getCertPath(String name);
}
