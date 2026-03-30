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
