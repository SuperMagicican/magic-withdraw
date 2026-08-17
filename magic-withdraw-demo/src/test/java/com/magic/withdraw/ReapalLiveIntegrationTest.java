package com.magic.withdraw;

import com.alibaba.fastjson2.JSON;
import com.magic.withdraw.core.callback.CallBackService;
import com.magic.withdraw.core.domain.bean.WithdrawResult;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.reapal.ReapalConfig.RechargeMode;
import com.magic.withdraw.reapal.ReapalSingleWithdrawData;
import com.magic.withdraw.service.DemoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 融宝测试环境手工集成测试，默认不访问外部支付环境。
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "magic.withdraw.reapal.live", matches = "true")
@Import(ReapalLiveIntegrationTest.LiveCallbackConfiguration.class)
class ReapalLiveIntegrationTest {

    @Autowired
    private DemoService demoService;

    @Autowired
    private RecordingCallBackService recordingCallBackService;

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

    @Test
    void shouldCompleteSingleB2bWithdrawalFlow() throws Exception {
        SingleWithdrawResponse response = demoService.reapalSingleWithdraw(RechargeMode.B2B_DIRECT);
        assertPaymentUrl(response);
        Path resultFile = Path.of("target", "reapal-live-result.txt");
        Files.writeString(resultFile, formatResult("b2b", response), StandardCharsets.UTF_8);

        long timeoutSeconds = Long.getLong("magic.withdraw.reapal.live.timeout-seconds",
                Duration.ofMinutes(30).toSeconds());
        RecordedResult result = recordingCallBackService.await(timeoutSeconds);
        assertNotNull(result, "等待融宝代付最终结果超时");
        Files.writeString(resultFile,
                "withdraw.status=" + result.status() + System.lineSeparator()
                        + "withdraw.result=" + JSON.toJSONString(result.result()) + System.lineSeparator(),
                StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
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

    @TestConfiguration(proxyBeanMethods = false)
    static class LiveCallbackConfiguration {

        @Bean
        RecordingCallBackService recordingCallBackService() {
            return new RecordingCallBackService();
        }
    }

    static class RecordingCallBackService implements CallBackService {

        private final LinkedBlockingQueue<RecordedResult> results = new LinkedBlockingQueue<>();

        @Override
        public void successWithdraw(WithdrawResult withdrawResult) {
            results.offer(new RecordedResult("SUCCESS", withdrawResult));
        }

        @Override
        public void failWithdraw(WithdrawResult withdrawResult) {
            results.offer(new RecordedResult("FAIL", withdrawResult));
        }

        RecordedResult await(long timeoutSeconds) throws InterruptedException {
            return results.poll(timeoutSeconds, TimeUnit.SECONDS);
        }
    }

    record RecordedResult(String status, WithdrawResult result) {
    }
}
