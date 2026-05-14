package com.magic.withdraw;

import com.magic.withdraw.service.DemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author lgy
 * @since 2026/1/30
 */
@SpringBootTest
public class DemoTest {

    @Autowired
    private DemoService demoService;

    @Test
    public void test() throws Exception {
//        demoService.queryBalance();
//        demoService.singleWithdraw();
//        demoService.queryTradingOrderNo();
//        demoService.cancelWithdraw();
//        demoService.reapalSingleWithdraw();
//        demoService.reapalQueryTradingOrderNo();
        demoService.reapalQueryCardBin();
    }
}
