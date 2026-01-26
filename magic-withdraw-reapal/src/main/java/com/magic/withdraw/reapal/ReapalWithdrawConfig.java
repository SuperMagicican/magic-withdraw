package com.magic.withdraw.reapal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author lgy
 * @since 2026/1/15
 */
@Component
@ConfigurationProperties("magic.withdraw.reapal")
@Data
public class ReapalWithdrawConfig {

    private String privateKeyPwd;

    private String reapalPublicKey;
}
