package com.magic.withdraw.reapal;

import com.magic.withdraw.core.constants.OrderStatusConstant;
import com.magic.withdraw.core.constants.PlatformConstant;
import com.magic.withdraw.core.domain.request.SingleWithdrawRequest;
import com.magic.withdraw.core.domain.response.QueryResponse;
import com.magic.withdraw.core.domain.response.SingleWithdrawResponse;
import com.magic.withdraw.reapal.ReapalSingleWithdrawData.SubmitStage;
import com.magic.withdraw.core.service.PlatformConfigService;
import com.magic.withdraw.core.service.impl.InMemoryProcessingOrderServiceImpl;
import com.magic.withdraw.reapal.ReapalConfig.RechargeMode;
import com.magic.withdraw.reapal.recharge.B2bDirectRechargeStrategy;
import com.magic.withdraw.reapal.recharge.CashierRechargeStrategy;
import com.magic.withdraw.reapal.recharge.DefaultReapalRechargeCallback;
import com.magic.withdraw.reapal.recharge.InMemoryReapalRechargeOrderStore;
import com.magic.withdraw.reapal.recharge.ReapalRechargeCallback;
import com.magic.withdraw.reapal.recharge.ReapalRechargeQueryResult;
import com.magic.withdraw.reapal.recharge.ReapalRechargeRequest;
import com.magic.withdraw.reapal.recharge.ReapalRechargeResponse;
import com.magic.withdraw.reapal.recharge.ReapalRechargeOrderStore.RechargePollingOrder;
import com.magic.withdraw.reapal.recharge.ReapalRechargeService;
import com.magic.withdraw.reapal.recharge.ReapalRechargePollingService;
import com.magic.withdraw.reapal.recharge.ReapalRechargeStrategyRegistry;
import com.reapal.api.ApiException;
import com.reapal.api.Client;
import com.reapal.api.model.DfTradeQueryResult;
import com.reapal.api.model.DfSingleTradeResult;
import com.reapal.api.model.DfTradeSubResult;
import com.reapal.api.model.OrderQueryResult;
import com.reapal.api.model.TradeResult;
import com.reapal.api.request.BaseRequest;
import com.reapal.api.request.DfTradeQueryRequest;
import com.reapal.api.request.DfSingleTradeRequest;
import com.reapal.api.request.OrderQueryRequest;
import com.reapal.api.request.TradeRequest;
import com.reapal.api.response.BaseResponse;
import com.reapal.api.response.DfTradeQueryResponse;
import com.reapal.api.response.DfSingleTradeResponse;
import com.reapal.api.response.OrderQueryResponse;
import com.reapal.api.response.TradeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReapalWithdrawTradeTest {

    private static ReapalSingleWithdrawData data(SingleWithdrawResponse response) {
        return ReapalSingleWithdrawData.from(response);
    }

    @Test
    void shouldSubmitStandardPayoutThenB2bRecharge() {
        FakeClient client = new FakeClient(payoutAccepted(125L), rechargeProcessing("https://bank.example/pay"));
        ReapalWithdrawTrade trade = createTrade(client);

        SingleWithdrawResponse response = trade.singleWithdraw(validRequest());

        assertTrue(response.isSuccess());
        assertFalse(response.isPollingRequired());
        assertEquals(SubmitStage.RECHARGE_ACCEPTED, data(response).getSubmitStage());
        assertEquals("DF-100", response.getOutOrderNo());
        assertEquals("RC-100", data(response).getRechargeOrderNo());
        assertEquals("PAY-100", data(response).getRechargeOutOrderNo());
        assertEquals(125L, data(response).getRechargeAmount());
        assertEquals("https://bank.example/pay", data(response).getPaymentUrl());
        assertEquals("token-100", data(response).getPaymentToken());

        DfSingleTradeRequest payoutRequest = assertInstanceOf(
                DfSingleTradeRequest.class, client.requests.get(0));
        assertEquals("OR_PAY", payoutRequest.getProductCode());
        assertEquals("PAYMENT", payoutRequest.getBusinessCode());
        assertEquals("1", payoutRequest.getTradeType());
        assertEquals("O_PAY", payoutRequest.getRechargeType());
        assertEquals(100L, payoutRequest.getAmount());

        TradeRequest rechargeRequest = assertInstanceOf(TradeRequest.class, client.requests.get(1));
        assertEquals("DF-100", rechargeRequest.getDfOrderId());
        assertEquals("RC-100", rechargeRequest.getMerchantOrderNo());
        assertEquals(125L, rechargeRequest.getAmount());
        assertEquals("RECHARGE", rechargeRequest.getBusinessCode());
        assertEquals("O_PAY_B2B", rechargeRequest.getProductCode());
        assertEquals("directPay", rechargeRequest.getExpend().get("payMethod"));
        assertEquals("BB", rechargeRequest.getExpend().get("wgType"));
        assertEquals("0105", rechargeRequest.getExpend().get("bankNo"));
        assertEquals("20", rechargeRequest.getExpend().get("pyeeAcctType"));
    }

    @Test
    void shouldSubmitCashierRechargeWithoutBankSelection() {
        FakeClient client = new FakeClient(payoutAccepted(125L), rechargeProcessing("https://cashier.example/pay"));

        SingleWithdrawResponse response = createTrade(client, RechargeMode.CASHIER, null)
                .singleWithdraw(validRequest());

        assertTrue(response.isSuccess());
        assertFalse(response.isPollingRequired());
        assertEquals("https://cashier.example/pay", data(response).getPaymentUrl());
        TradeRequest rechargeRequest = assertInstanceOf(TradeRequest.class, client.requests.get(1));
        assertEquals("CASHIER", rechargeRequest.getProductCode());
        assertEquals(Map.of("pyeeAcctType", "20"), rechargeRequest.getExpend());
    }

    @Test
    void shouldRequireRechargeBankOnlyForB2bDirectMode() {
        FakeClient client = new FakeClient();

        SingleWithdrawResponse response = createTrade(client, RechargeMode.B2B_DIRECT, null)
                .singleWithdraw(validRequest());

        assertFalse(response.isSuccess());
        assertEquals(SubmitStage.VALIDATION_FAILED, data(response).getSubmitStage());
        assertEquals("充值银行编码不能为空", response.getMessage());
        assertTrue(client.requests.isEmpty());
    }

    @Test
    void shouldNotRechargeWhenPayoutIsRejected() {
        DfSingleTradeResponse payoutResponse = new DfSingleTradeResponse();
        payoutResponse.setCode("1001");
        payoutResponse.setMsg("系统异常");
        FakeClient client = new FakeClient(payoutResponse);

        SingleWithdrawResponse response = createTrade(client).singleWithdraw(validRequest());

        assertFalse(response.isSuccess());
        assertEquals(SubmitStage.PAYOUT_REJECTED, data(response).getSubmitStage());
        assertEquals(1, client.requests.size());
    }

    @Test
    void shouldRejectFractionalCentBeforeRemoteCall() {
        FakeClient client = new FakeClient();
        SingleWithdrawRequest request = validRequest().setAmount(new BigDecimal("1.001"));

        SingleWithdrawResponse response = createTrade(client).singleWithdraw(request);

        assertFalse(response.isSuccess());
        assertEquals(SubmitStage.VALIDATION_FAILED, data(response).getSubmitStage());
        assertTrue(client.requests.isEmpty());
    }

    @Test
    void shouldFailAmbiguousRechargeWithoutQueryingAgain() {
        TradeResponse ambiguous = new TradeResponse();
        ambiguous.setCode("1001");
        ambiguous.setMsg("系统异常");
        FakeClient client = new FakeClient(payoutAccepted(125L), ambiguous);

        SingleWithdrawResponse response = createTrade(client).singleWithdraw(validRequest());

        assertFalse(response.isSuccess());
        assertFalse(response.isPollingRequired());
        assertEquals(SubmitStage.RECHARGE_UNKNOWN, data(response).getSubmitStage());
        assertEquals(2, client.requests.size());
    }

    @Test
    void shouldAddPayoutToPollingQueueWhenRechargeSucceeds() {
        FakeClient client = new FakeClient(payoutAccepted(125L),
                rechargeProcessing("https://bank.example/pay"), rechargeQuery("completed"));
        ReapalConfig config = validConfig(RechargeMode.B2B_DIRECT, "0105");
        PlatformConfigService configService = mock(PlatformConfigService.class);
        when(configService.get(PlatformConstant.REAPAL)).thenReturn(config);
        InMemoryReapalRechargeOrderStore orderStore = new InMemoryReapalRechargeOrderStore();
        ReapalClientFactory clientFactory = new ReapalClientFactory() {
            @Override
            public Client create(ReapalConfig ignored) {
                return client;
            }
        };
        ReapalRechargeService rechargeService = new ReapalRechargeService(
                strategyRegistry(), orderStore, configService, clientFactory);

        SingleWithdrawResponse response = new ReapalWithdrawTrade(
                configService, rechargeService, clientFactory).singleWithdraw(validRequest());
        assertTrue(response.isSuccess());
        assertFalse(response.isPollingRequired());

        orderStore.reschedule("RC-100", 0L);
        InMemoryProcessingOrderServiceImpl payoutOrders = new InMemoryProcessingOrderServiceImpl();
        DefaultReapalRechargeCallback callback =
                new DefaultReapalRechargeCallback(payoutOrders);
        new ReapalRechargePollingService(orderStore, rechargeService, callback)
                .pollDueOrders();

        assertEquals(1, payoutOrders.list().size());
        assertEquals("PAYOUT-100", payoutOrders.list().iterator().next().getOrderNo());
        assertTrue(orderStore.listDue(Long.MAX_VALUE).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"failed", "closed"})
    void shouldRemovePollingOrderWithoutCallbackWhenRechargeFails(
            String rechargeStatus) {
        FakeClient client = new FakeClient(payoutAccepted(125L),
                rechargeProcessing("https://bank.example/pay"), rechargeQuery(rechargeStatus));
        ReapalConfig config = validConfig(RechargeMode.B2B_DIRECT, "0105");
        PlatformConfigService configService = mock(PlatformConfigService.class);
        when(configService.get(PlatformConstant.REAPAL)).thenReturn(config);
        InMemoryReapalRechargeOrderStore orderStore = new InMemoryReapalRechargeOrderStore();
        ReapalClientFactory clientFactory = new ReapalClientFactory() {
            @Override
            public Client create(ReapalConfig ignored) {
                return client;
            }
        };
        ReapalRechargeService rechargeService = new ReapalRechargeService(
                strategyRegistry(), orderStore, configService, clientFactory);
        SingleWithdrawResponse response = new ReapalWithdrawTrade(
                configService, rechargeService, clientFactory).singleWithdraw(validRequest());
        assertTrue(response.isSuccess());

        orderStore.reschedule("RC-100", 0L);
        RecordingRechargeCallback callback = new RecordingRechargeCallback();
        new ReapalRechargePollingService(orderStore, rechargeService, callback)
                .pollDueOrders();

        assertNull(callback.successResult);
        assertTrue(orderStore.listDue(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void shouldExposeStandaloneRechargeSubmitAndQuery() {
        FakeClient client = new FakeClient(
                rechargeProcessing("https://cashier.example/pay"),
                rechargeQuery("completed"));
        ReapalConfig config = validConfig(RechargeMode.CASHIER, null);
        PlatformConfigService configService = mock(PlatformConfigService.class);
        when(configService.get(PlatformConstant.REAPAL)).thenReturn(config);
        InMemoryReapalRechargeOrderStore orderStore = new InMemoryReapalRechargeOrderStore();
        ReapalClientFactory clientFactory = new ReapalClientFactory() {
            @Override
            public Client create(ReapalConfig ignored) {
                return client;
            }
        };
        ReapalRechargeService rechargeService = new ReapalRechargeService(
                strategyRegistry(), orderStore, configService, clientFactory);

        ReapalRechargeResponse submitResponse = rechargeService.submit(
                new ReapalRechargeRequest()
                        .setRechargeOrderNo("RC-100")
                        .setPayoutOrderNo("PAYOUT-100")
                        .setPayoutOutOrderNo("DF-100")
                        .setAmount(125L)
                        .setOrderTitle("订单代付测试"));
        ReapalRechargeQueryResult queryResult = rechargeService.query("RC-100");

        assertTrue(submitResponse.isSuccess());
        assertTrue(submitResponse.isPollingRequired());
        assertEquals("https://cashier.example/pay", submitResponse.getPaymentUrl());
        assertTrue(queryResult.isQuerySuccessful());
        assertEquals(ReapalRechargeQueryResult.RechargeState.SUCCESS,
                queryResult.getState());
        assertInstanceOf(TradeRequest.class, client.requests.get(0));
    }

    @Test
    void shouldRejectDirectReplacementWhileCurrentRechargeExists() {
        InMemoryReapalRechargeOrderStore orderStore =
                new InMemoryReapalRechargeOrderStore();
        orderStore.add(new RechargePollingOrder(
                "RC-OLD", "PAYOUT-100", 10L, 100L, 10L));

        assertThrows(IllegalStateException.class, () -> orderStore.add(
                new RechargePollingOrder(
                        "RC-NEW", "PAYOUT-100", 20L, 200L, 10L)));

        RechargePollingOrder current =
                orderStore.getByPayoutOrderNo("PAYOUT-100");
        assertEquals("RC-OLD", current.rechargeOrderNo());
    }

    @Test
    void shouldStartNewRechargeAfterPreviousPollingOrderIsRemoved() {
        FakeClient client = new FakeClient(
                rechargeProcessing("https://cashier.example/new"),
                rechargeQuery("completed"));
        ReapalConfig config = validConfig(RechargeMode.CASHIER, null);
        PlatformConfigService configService = mock(PlatformConfigService.class);
        when(configService.get(PlatformConstant.REAPAL)).thenReturn(config);
        InMemoryReapalRechargeOrderStore orderStore =
                new InMemoryReapalRechargeOrderStore();
        orderStore.add(new RechargePollingOrder(
                "RC-OLD", "PAYOUT-100", 10L, 1_000L, 10L));
        orderStore.remove("RC-OLD");
        ReapalClientFactory clientFactory = new ReapalClientFactory() {
            @Override
            public Client create(ReapalConfig ignored) {
                return client;
            }
        };
        ReapalRechargeService rechargeService = new ReapalRechargeService(
                strategyRegistry(), orderStore, configService, clientFactory);

        ReapalRechargeResponse response = rechargeService.submit(
                new ReapalRechargeRequest()
                        .setRechargeOrderNo("RC-100")
                        .setPayoutOrderNo("PAYOUT-100")
                        .setPayoutOutOrderNo("DF-100")
                        .setAmount(125L));

        assertTrue(response.isSuccess());
        assertEquals("https://cashier.example/new", response.getPaymentUrl());
        assertEquals("RC-100", orderStore.getByPayoutOrderNo("PAYOUT-100")
                .rechargeOrderNo());
        assertFalse(orderStore.listDue(Long.MAX_VALUE).stream()
                .anyMatch(order -> "RC-OLD".equals(order.rechargeOrderNo())));

        orderStore.reschedule("RC-100", 0L);
        InMemoryProcessingOrderServiceImpl payoutOrders =
                new InMemoryProcessingOrderServiceImpl();
        new ReapalRechargePollingService(
                orderStore,
                rechargeService,
                new DefaultReapalRechargeCallback(payoutOrders))
                .pollDueOrders();

        assertEquals(1, payoutOrders.list().size());
        assertEquals("PAYOUT-100", payoutOrders.list().iterator().next().getOrderNo());
        assertTrue(orderStore.listDue(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void shouldRejectNewRechargeWithoutQueryWhenPollingOrderExists() {
        FakeClient client = new FakeClient();
        ReapalConfig config = validConfig(RechargeMode.CASHIER, null);
        PlatformConfigService configService = mock(PlatformConfigService.class);
        when(configService.get(PlatformConstant.REAPAL)).thenReturn(config);
        InMemoryReapalRechargeOrderStore orderStore =
                new InMemoryReapalRechargeOrderStore();
        orderStore.add(new RechargePollingOrder(
                "RC-OLD", "PAYOUT-100", 10L, 1_000L, 10L));
        ReapalClientFactory clientFactory = new ReapalClientFactory() {
            @Override
            public Client create(ReapalConfig ignored) {
                return client;
            }
        };
        ReapalRechargeService rechargeService = new ReapalRechargeService(
                strategyRegistry(), orderStore, configService, clientFactory);

        ReapalRechargeResponse response = rechargeService.submit(
                new ReapalRechargeRequest()
                        .setRechargeOrderNo("RC-100")
                        .setPayoutOrderNo("PAYOUT-100")
                        .setPayoutOutOrderNo("DF-100")
                        .setAmount(125L));

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("存在待查询的充值订单"));
        assertEquals("RC-OLD", orderStore.getByPayoutOrderNo("PAYOUT-100")
                .rechargeOrderNo());
        assertTrue(client.requests.isEmpty());
    }

    @Test
    void shouldRemoveRechargePollingOrderWhenQueryTimesOut() {
        FakeClient client = new FakeClient();
        ReapalConfig config = validConfig(RechargeMode.CASHIER, null);
        PlatformConfigService configService = mock(PlatformConfigService.class);
        when(configService.get(PlatformConstant.REAPAL)).thenReturn(config);
        InMemoryReapalRechargeOrderStore orderStore =
                new InMemoryReapalRechargeOrderStore();
        orderStore.add(new RechargePollingOrder(
                "RC-100", "PAYOUT-100", 0L, 0L, 10L));
        ReapalClientFactory clientFactory = new ReapalClientFactory() {
            @Override
            public Client create(ReapalConfig ignored) {
                return client;
            }
        };
        ReapalRechargeService rechargeService = new ReapalRechargeService(
                strategyRegistry(), orderStore, configService, clientFactory);
        RecordingRechargeCallback callback = new RecordingRechargeCallback();

        new ReapalRechargePollingService(orderStore, rechargeService, callback)
                .pollDueOrders();

        assertNull(orderStore.getByPayoutOrderNo("PAYOUT-100"));
        assertNull(callback.successResult);
        assertTrue(client.requests.isEmpty());
    }

    @Test
    void shouldKeepPollingOrderWhileRechargeIsProcessing() {
        FakeClient client = new FakeClient(rechargeQuery("processing"));
        InMemoryReapalRechargeOrderStore orderStore =
                new InMemoryReapalRechargeOrderStore();
        orderStore.add(new RechargePollingOrder(
                "RC-100", "PAYOUT-100", 0L, Long.MAX_VALUE, 10_000L));
        RecordingRechargeCallback callback = new RecordingRechargeCallback();

        new ReapalRechargePollingService(
                orderStore, rechargeService(client, orderStore), callback)
                .pollDueOrders();

        assertNotNull(orderStore.getByPayoutOrderNo("PAYOUT-100"));
        assertNull(callback.successResult);
        assertEquals(1, client.requests.size());
    }

    @Test
    void shouldKeepPollingOrderWhenRechargeQueryFails() {
        FakeClient client = new FakeClient(new ApiException("query timeout"));
        InMemoryReapalRechargeOrderStore orderStore =
                new InMemoryReapalRechargeOrderStore();
        orderStore.add(new RechargePollingOrder(
                "RC-100", "PAYOUT-100", 0L, Long.MAX_VALUE, 10_000L));
        RecordingRechargeCallback callback = new RecordingRechargeCallback();

        new ReapalRechargePollingService(
                orderStore, rechargeService(client, orderStore), callback)
                .pollDueOrders();

        assertNotNull(orderStore.getByPayoutOrderNo("PAYOUT-100"));
        assertNull(callback.successResult);
        assertEquals(1, client.requests.size());
    }

    @Test
    void shouldRemoveSuccessfulRechargeEvenWhenCallbackFails() {
        FakeClient client = new FakeClient(rechargeQuery("completed"));
        InMemoryReapalRechargeOrderStore orderStore =
                new InMemoryReapalRechargeOrderStore();
        orderStore.add(new RechargePollingOrder(
                "RC-100", "PAYOUT-100", 0L, Long.MAX_VALUE, 10_000L));

        new ReapalRechargePollingService(
                orderStore, rechargeService(client, orderStore),
                result -> {
                    throw new IllegalStateException("callback failed");
                })
                .pollDueOrders();

        assertNull(orderStore.getByPayoutOrderNo("PAYOUT-100"));
        assertEquals(1, client.requests.size());
    }

    @Test
    void shouldStartPayoutPollingWhenRechargeAlreadyCompleted() {
        FakeClient client = new FakeClient(payoutAccepted(125L), rechargeCompleted());

        SingleWithdrawResponse response = createTrade(client).singleWithdraw(validRequest());

        assertTrue(response.isSuccess());
        assertTrue(response.isPollingRequired());
        assertEquals(SubmitStage.RECHARGE_ACCEPTED, data(response).getSubmitStage());
        assertEquals("completed", data(response).getRechargeStatus());
        assertEquals(2, client.requests.size());
    }

    @Test
    void shouldKeepPayoutIdentifiersWhenRechargeRemainsUnknown() {
        FakeClient client = new FakeClient(payoutAccepted(125L), new ApiException("timeout"));

        SingleWithdrawResponse response = createTrade(client).singleWithdraw(validRequest());

        assertFalse(response.isSuccess());
        assertEquals(SubmitStage.RECHARGE_UNKNOWN, data(response).getSubmitStage());
        assertEquals("PAYOUT-100", response.getOrderNo());
        assertEquals("DF-100", response.getOutOrderNo());
        assertEquals("RC-100", data(response).getRechargeOrderNo());
        assertEquals(125L, data(response).getRechargeAmount());
        assertEquals(2, client.requests.size());
    }

    @Test
    void shouldRejectFailedRechargeWithoutQueryingAgain() {
        FakeClient client = new FakeClient(payoutAccepted(125L), rechargeFailed());

        SingleWithdrawResponse response = createTrade(client).singleWithdraw(validRequest());

        assertFalse(response.isSuccess());
        assertEquals(SubmitStage.RECHARGE_REJECTED, data(response).getSubmitStage());
        assertEquals(2, client.requests.size());
    }

    @Test
    void shouldReportMissingPaymentUrlAsUnknown() {
        FakeClient client = new FakeClient(payoutAccepted(125L), rechargeProcessing(null));

        SingleWithdrawResponse response = createTrade(client).singleWithdraw(validRequest());

        assertFalse(response.isSuccess());
        assertEquals(SubmitStage.RECHARGE_UNKNOWN, data(response).getSubmitStage());
        assertEquals(2, client.requests.size());
    }

    @ParameterizedTest
    @CsvSource({
            "5,PROCESSING",
            "12,PROCESSING",
            "6,SUCCESS",
            "7,FAIL",
            "4,FAIL",
            "10,FAIL",
            "11,FAIL",
            "99,"
    })
    void shouldMapPayoutQueryStatuses(String reapalStatus, String expectedStatus) {
        FakeClient client = new FakeClient(payoutQuery(reapalStatus));

        QueryResponse response = createTrade(client).queryTradingOrderNo("PAYOUT-100");

        assertTrue(response.isSuccess());
        assertEquals(expectedStatus, response.getOrderStatus());
        assertInstanceOf(DfTradeQueryRequest.class, client.requests.get(0));
    }

    private static ReapalWithdrawTrade createTrade(FakeClient client) {
        return createTrade(client, RechargeMode.B2B_DIRECT, "0105");
    }

    private static ReapalRechargeService rechargeService(
            FakeClient client, InMemoryReapalRechargeOrderStore orderStore) {
        ReapalConfig config = validConfig(RechargeMode.CASHIER, null);
        PlatformConfigService configService = mock(PlatformConfigService.class);
        when(configService.get(PlatformConstant.REAPAL)).thenReturn(config);
        ReapalClientFactory clientFactory = new ReapalClientFactory() {
            @Override
            public Client create(ReapalConfig ignored) {
                return client;
            }
        };
        return new ReapalRechargeService(
                strategyRegistry(), orderStore, configService, clientFactory);
    }

    private static ReapalWithdrawTrade createTrade(FakeClient client, RechargeMode rechargeMode,
                                                    String rechargeBankNo) {
        PlatformConfigService platformConfigService = mock(PlatformConfigService.class);
        ReapalConfig config = validConfig(rechargeMode, rechargeBankNo);
        when(platformConfigService.get(PlatformConstant.REAPAL)).thenReturn(config);
        ReapalClientFactory clientFactory = new ReapalClientFactory();
        ReapalRechargeService rechargeService = new ReapalRechargeService(
                strategyRegistry(), new InMemoryReapalRechargeOrderStore(),
                platformConfigService, clientFactory);
        return new ReapalWithdrawTrade(platformConfigService, rechargeService,
                clientFactory) {
            @Override
            protected Client buildClient(ReapalConfig ignored) {
                return client;
            }
        };
    }

    private static ReapalConfig validConfig(RechargeMode rechargeMode, String rechargeBankNo) {
        return new ReapalConfig()
                .setOpenApiDomain("https://testopenapi.reapal.com:8443")
                .setMerchantId("M-100")
                .setCustomerId("C-100")
                .setRechargeMode(rechargeMode)
                .setRechargeBankNo(rechargeBankNo)
                .setMemberId("MEMBER-100")
                .setMemberIp("127.0.0.1")
                .setRechargeQueryInterval(10L)
                .setRechargeQueryTimeout(1800L)
                .setReturnUrl("https://merchant.example/return");
    }

    private static ReapalRechargeStrategyRegistry strategyRegistry() {
        return new ReapalRechargeStrategyRegistry(List.of(
                new B2bDirectRechargeStrategy(), new CashierRechargeStrategy()));
    }

    private static SingleWithdrawRequest validRequest() {
        return new SingleWithdrawRequest()
                .setOrderNo("PAYOUT-100")
                .setRechargeOrderNo("RC-100")
                .setCardNo("6212260200133751211")
                .setCardName("测试用户")
                .setAccountType(SingleWithdrawRequest.EnumAccountType.PERSONAL.getCode())
                .setBankNo("0102")
                .setOrderTitle("订单代付测试")
                .setNotifyUrl("https://merchant.example/notify")
                .setAmount(new BigDecimal("1.00"));
    }

    private static DfSingleTradeResponse payoutAccepted(long rechargeAmount) {
        DfSingleTradeResult result = new DfSingleTradeResult();
        result.setResultCode("2000");
        result.setResultMsg("待支付");
        result.setMerchantOrderNo("PAYOUT-100");
        result.setOrderId("DF-100");
        result.setRechargeAmount(rechargeAmount);
        DfSingleTradeResponse response = new DfSingleTradeResponse();
        response.setCode("0000");
        response.setMsg("操作成功");
        response.setData(result);
        return response;
    }

    private static TradeResponse rechargeProcessing(String paymentUrl) {
        TradeResult result = new TradeResult();
        result.setResultCode("3081");
        result.setResultMsg("交易处理中");
        result.setMerchantOrderNo("RC-100");
        result.setOrderId("PAY-100");
        result.setAmount(125L);
        result.setOrderStatus("processing");
        result.setExtendMap(paymentUrl == null ? Map.of() : Map.of(
                "callbackUrl", paymentUrl, "token", "token-100"));
        TradeResponse response = new TradeResponse();
        response.setCode("0000");
        response.setMsg("操作成功");
        response.setData(result);
        return response;
    }

    private static TradeResponse rechargeFailed() {
        TradeResult result = new TradeResult();
        result.setResultCode("4006");
        result.setResultMsg("充值失败");
        result.setMerchantOrderNo("RC-100");
        result.setOrderId("PAY-100");
        result.setOrderStatus("failed");
        TradeResponse response = new TradeResponse();
        response.setCode("0000");
        response.setData(result);
        return response;
    }

    private static TradeResponse rechargeCompleted() {
        TradeResult result = new TradeResult();
        result.setResultCode("0000");
        result.setResultMsg("充值成功");
        result.setMerchantOrderNo("RC-100");
        result.setOrderId("PAY-100");
        result.setAmount(125L);
        result.setOrderStatus("completed");
        TradeResponse response = new TradeResponse();
        response.setCode("0000");
        response.setData(result);
        return response;
    }

    private static OrderQueryResponse rechargeQuery(String status) {
        OrderQueryResult result = new OrderQueryResult();
        result.setMerchantOrderNo("RC-100");
        result.setOrderId("PAY-100");
        result.setAmount(125L);
        result.setOrdersts(status);
        OrderQueryResponse response = new OrderQueryResponse();
        response.setCode("0000");
        response.setMsg("查询成功");
        response.setData(result);
        return response;
    }

    private static DfTradeQueryResponse payoutQuery(String status) {
        DfTradeSubResult detail = new DfTradeSubResult();
        detail.setMerchantOrderNo("PAYOUT-100");
        detail.setStatus(status);
        detail.setResultMsg("状态查询成功");
        DfTradeQueryResult result = new DfTradeQueryResult();
        result.setResultCode("0000");
        result.setDetails(List.of(detail));
        DfTradeQueryResponse response = new DfTradeQueryResponse();
        response.setCode("0000");
        response.setData(result);
        return response;
    }

    private static final class FakeClient implements Client {

        private final Queue<Object> results = new ArrayDeque<>();
        private final List<BaseRequest<?>> requests = new ArrayList<>();

        private FakeClient(Object... results) {
            for (Object result : results) {
                this.results.add(result);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BaseResponse> T execute(BaseRequest<T> request) throws ApiException {
            requests.add(request);
            Object result = results.remove();
            if (result instanceof ApiException exception) {
                throw exception;
            }
            return (T) result;
        }
    }

    private static final class RecordingRechargeCallback
            implements ReapalRechargeCallback {

        private ReapalRechargeQueryResult successResult;

        @Override
        public void successRecharge(ReapalRechargeQueryResult result) {
            successResult = result;
        }
    }
}
