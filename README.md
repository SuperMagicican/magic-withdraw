# magic-withdraw

一个基于 Spring Boot 的多渠道提现组件，提供统一接口对接多家支付渠道。

当前仓库包含融宝、支付宝、微信三个渠道实现，并提供 demo 工程用于接入演示。

## 特性

- 统一提现接口：对业务方只暴露一套提现能力。
- 多渠道扩展：通过渠道实现类 + 注解可扩展新平台。
- 自动装配：依赖引入后可自动注册核心 Bean。
- 回调处理：支持回调验签与结果分发。
- 定时巡检：可选开启处理中订单轮询补偿。

## 环境要求

- JDK 17+
- Maven 3.8+
- Spring Boot 3.x

## 模块说明

- `magic-withdraw-core`
	- 核心抽象、统一服务入口、回调控制器、默认实现。
- `magic-withdraw-alipay`
	- 支付宝提现实现。
- `magic-withdraw-reapal`
	- 融宝提现实现。
- `magic-withdraw-wxpay`
	- 微信提现实现。
- `magic-withdraw-all`
	- 聚合依赖模块（建议业务工程直接依赖它）。
- `magic-withdraw-demo`
	- 示例工程。

## 快速开始

### 1. 引入依赖

建议直接引入聚合模块：

```xml
<dependency>
		<groupId>com.magic</groupId>
		<artifactId>magic-withdraw-all</artifactId>
		<version>1.0.4</version>
</dependency>
```

### 2. 注入统一服务

```java
@RequiredArgsConstructor
@Service
public class WithdrawAppService {

		private final WithdrawService withdrawService;
		private final PlatformConfigService platformConfigService;

		public SingleWithdrawResponse withdraw(SingleWithdrawRequest request) {
				// 业务启动时或渠道配置变更时，先写入渠道配置
				// platformConfigService.set(PlatformConstant.WXPAY, wxpayConfig);
				return withdrawService.singleWithdraw(request, PlatformConstant.WXPAY);
		}
}
```

### 3. 初始化平台配置

项目运行时通过 `PlatformConfigService` 读取渠道配置，业务方需要在调用前完成配置写入。

示例（微信）：

```java
WxpayConfig wxpayConfig = new WxpayConfig(
				appSecret,
				appid,
				mchid,
				certificateSerialNo,
				keyManager.getCertPath("apiclient_key.pem"),
				wechatPayPublicKeyId,
				keyManager.getCertPath("pub_key.pem")
);
platformConfigService.set(PlatformConstant.WXPAY, wxpayConfig);
```

### 4. 融宝标准订单代付

融宝单笔代付会先创建标准订单代付，再使用融宝返回的充值金额创建关联的网银 B2B 充值订单：

```java
reapalConfig.setRechargeMode(ReapalConfig.RechargeMode.B2B_DIRECT)
        .setRechargeBankNo("0102")
        .setMemberId("业务会员号")
        .setMemberIp("用户IP")
        .setRechargeQueryInterval(10)
        .setRechargeQueryTimeout(1800)
        .setReturnUrl("https://merchant.example/reapal/return");

SingleWithdrawRequest request = new SingleWithdrawRequest()
        .setOrderNo("PAYOUT-20260811001")
        .setRechargeOrderNo("RECHARGE-20260811001")
        .setCardNo("收款银行卡号")
        .setCardName("收款人姓名")
        .setAccountType(SingleWithdrawRequest.EnumAccountType.PERSONAL.getCode())
        .setBankNo("0102")
        .setAmount(new BigDecimal("100.00"))
        .setNotifyUrl("https://merchant.example/withdrawCallBack/notify/reapal")
        .setOrderTitle("订单代付充值");

SingleWithdrawResponse response = withdrawService.singleWithdraw(request, PlatformConstant.REAPAL);
ReapalSingleWithdrawData reapalData = ReapalSingleWithdrawData.from(response);
String paymentUrl = reapalData.getPaymentUrl();
```

`SingleWithdrawResponse` 只保留通用字段；融宝提交阶段、充值订单和付款 URL 等数据位于
`platformData`。调用方通过 `ReapalSingleWithdrawData.from(response)` 转成融宝实体。
该方法同时支持内存中的实体对象和 JSON 反序列化后得到的 `Map`。

`response.success=true` 表示代付订单和关联充值订单均已受理，业务方应引导付款方访问
`paymentUrl` 完成网银支付。银行卡最终到账结果仍以代付查询或代付回调为准。

充值模式支持：

- `B2B_DIRECT`：指定 `rechargeBankNo`，直接进入对应企业网银。
- `CASHIER`：进入融宝收银台后选择付款方式，不需要传 `rechargeBankNo`；这是默认模式。

`SingleWithdrawRequest` 中仅 `rechargeOrderNo` 是融宝专用的逐笔参数；充值模式、付款银行、会员信息和返回地址统一配置在 `ReapalConfig`。

收银台模式只需调整融宝配置：

```java
reapalConfig.setRechargeMode(ReapalConfig.RechargeMode.CASHIER)
        .setRechargeBankNo(null);
```

两种模式都返回 `paymentUrl`，均需要付款人手动完成充值；充值成功后融宝才会继续执行关联代付。

融宝订单在充值状态为 `wait` 或 `processing` 时进入充值主动巡检队列。组件按
`rechargeQueryInterval` 间隔查询充值订单，最长查询 `rechargeQueryTimeout` 秒；查询到
`completed` 后默认将关联代付订单加入代付巡检；充值为 `failed`、`closed`
等其他终态时只移除充值巡检任务，不触发提现失败回调；查询超时同样只移除任务。
同一代付订单只要在充值巡检存储中已有任务，就不允许再次发起充值。默认使用内存队列，
生产环境可提供 `ReapalRechargeOrderStore` Bean 替换为 Redis、数据库等持久化实现。

`ReapalRechargeService` 也可以脱离组合代付流程，独立发起和查询关联充值：

```java
ReapalRechargeResponse submitResponse = reapalRechargeService.submit(
        new ReapalRechargeRequest()
                .setRechargeOrderNo("RECHARGE-20260811001")
                .setPayoutOrderNo("PAYOUT-20260811001")
                .setPayoutOutOrderNo("融宝代付orderId")
                .setAmount(10000L)
                .setOrderTitle("订单代付充值"));

ReapalRechargeQueryResult queryResult = reapalRechargeService.query(
        "RECHARGE-20260811001");
```

业务方需要替换充值成功处理时，可提供 `ReapalRechargeCallback` Bean，实现
`successRecharge`。没有自定义实现时，充值成功会默认进入代付巡检。手动调用 `query`
只返回查询结果，不触发回调；由内存或外部巡检存储驱动的查询在充值成功时才会触发回调。

## 回调与巡检配置

在业务工程中可配置：

```yaml
magic:
	withdraw:
		callback:
			watch: true
			cycle: 10
```

- `watch`: 是否开启处理中订单巡检任务。
- `cycle`: 巡检间隔（秒）。

默认回调入口：

- `POST /withdrawCallBack/notify/{platform}`

## 核心调用流程

1. 业务方调用 `WithdrawService`。
2. 核心根据 `platform` 路由到对应 `TradeService` 实现。
3. 渠道实现调用外部支付 API。
4. 回调或巡检触发 `CallBackService` 处理业务通知。

## 渠道能力矩阵（当前版本）

| 渠道 | 单笔提现 | 查询余额 | 查询订单 | 撤销提现 | 获取 openid | 回调验签 |
|---|---|---|---|---|---|---|
| Reapal | 支持 | 支持 | 未实现 | 未实现 | 未实现 | 支持 |
| Alipay | 支持 | 支持 | 支持 | 未实现 | 支持 | 未提供 |
| Wxpay | 支持 | 未实现 | 支持 | 支持 | 支持 | 未提供 |

说明：未实现能力会在对应实现中返回空或未提供逻辑，接入前请按业务需要评估并补齐。

## 本地构建

```bash
mvn clean install
```

仅运行 demo：

```bash
mvn -pl magic-withdraw-demo spring-boot:run
```

## 扩展新渠道

新增渠道建议遵循：

1. 新增模块并依赖 `magic-withdraw-core`。
2. 实现 `TradeService`（如需回调验签再实现 `ValidService`）。
3. 在实现类标注 `@TradePlatform("your-platform")`。
4. 在模块的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册实现类。
5. 业务方启动时通过 `PlatformConfigService` 写入该渠道配置。

## 注意事项

- 不要在代码中硬编码真实商户密钥、证书口令、AppSecret。
- 生产环境建议替换默认内存订单存储，实现持久化 `ProcessingOrderService`。
- 建议为每个渠道实现统一错误码与错误信息，避免上层处理分支不一致。

## 版本

当前版本：`1.0.4`
