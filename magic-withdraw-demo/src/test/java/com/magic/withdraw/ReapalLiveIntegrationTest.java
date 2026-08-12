package com.magic.withdraw;

import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.reapal.ReapalConfig.RechargeMode;
import com.magic.withdraw.reapal.ReapalSingleWithdrawData;
import com.magic.withdraw.service.DemoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 融宝测试环境手工集成测试，默认不访问外部支付环境。
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "magic.withdraw.reapal.live", matches = "true")
class ReapalLiveIntegrationTest {

    @Autowired
    private DemoService demoService;

    @Test
    void shouldCreateB2bAndCashierPaymentUrls() throws IOException {
        SingleWithdrawResponse b2bResponse = demoService.reapalSingleWithdraw(RechargeMode.B2B_DIRECT);
        SingleWithdrawResponse cashierResponse = demoService.reapalSingleWithdraw(RechargeMode.CASHIER);

        assertPaymentUrl(b2bResponse);
        assertPaymentUrl(cashierResponse);
        Files.writeString(Path.of("target", "reapal-live-result.txt"),
                formatResult("b2b", b2bResponse) + formatResult("cashier", cashierResponse),
                StandardCharsets.UTF_8);
    }

    private static void assertPaymentUrl(SingleWithdrawResponse response) {
        assertTrue(response.isSuccess(), response.getMessage());
        assertTrue(StringUtils.hasText(ReapalSingleWithdrawData.from(response).getPaymentUrl()),
                "融宝未返回付款 URL");
    }

    private static String formatResult(String prefix, SingleWithdrawResponse response) {
        ReapalSingleWithdrawData platformData = ReapalSingleWithdrawData.from(response);
        return prefix + ".stage=" + platformData.getSubmitStage() + System.lineSeparator()
                + prefix + ".orderNo=" + response.getOrderNo() + System.lineSeparator()
                + prefix + ".outOrderNo=" + response.getOutOrderNo() + System.lineSeparator()
                + prefix + ".rechargeOrderNo=" + platformData.getRechargeOrderNo() + System.lineSeparator()
                + prefix + ".paymentUrl=" + platformData.getPaymentUrl() + System.lineSeparator();
    }
}
