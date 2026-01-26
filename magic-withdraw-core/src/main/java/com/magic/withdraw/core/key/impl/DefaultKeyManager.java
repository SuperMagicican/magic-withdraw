package com.magic.withdraw.core.key.impl;

import com.magic.withdraw.core.key.KeyManager;
import com.magic.withdraw.core.utils.FileUtil;
import org.springframework.stereotype.Service;

/**
 * @author lgy
 * @since 2026/1/17
 */
@Service
public class DefaultKeyManager implements KeyManager {
    @Override
    public String getKey(String name) {
        return FileUtil.readToStr(name);
    }

    @Override
    public String getCertPath(String name) {
        return FileUtil.copyResourceToTempFile(name);
    }

}
