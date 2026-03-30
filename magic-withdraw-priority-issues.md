# magic-withdraw 问题优先级清单

## 说明

本文档将已发现问题按优先级分组，包含影响、代码定位与建议动作，便于直接纳入迭代计划。

优先级定义：

- P0：高概率影响线上正确性/稳定性，建议立即处理。
- P1：会显著影响可维护性或接入体验，建议本期处理。
- P2：工程规范与质量改进，建议排期优化。

## P0（立即处理）

### 1. 渠道路由使用 contains 匹配，存在误匹配风险

- 影响：平台标识包含关系可能命中多个实现或错误实现，导致提现/验签路由不稳定。
- 定位：
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/loader/MagicLoader.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/loader/MagicLoader.java#L57)
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/loader/MagicLoader.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/loader/MagicLoader.java#L77)
- 建议：改为严格等值匹配；若必须模糊匹配，定义唯一前缀规则并增加冲突检测。

### 2. TradeService 多处方法返回 null，接口契约不完整

- 影响：统一调用链上容易出现空指针或业务分支异常，且各渠道能力行为不一致。
- 定位：
  - [magic-withdraw-wxpay/src/main/java/com/magic/withdraw/wxpay/WxpayWithdrawTrade.java](magic-withdraw-wxpay/src/main/java/com/magic/withdraw/wxpay/WxpayWithdrawTrade.java#L122)
  - [magic-withdraw-reapal/src/main/java/com/magic/withdraw/reapal/ReapalWithdrawTrade.java](magic-withdraw-reapal/src/main/java/com/magic/withdraw/reapal/ReapalWithdrawTrade.java#L135)
  - [magic-withdraw-reapal/src/main/java/com/magic/withdraw/reapal/ReapalWithdrawTrade.java](magic-withdraw-reapal/src/main/java/com/magic/withdraw/reapal/ReapalWithdrawTrade.java#L140)
  - [magic-withdraw-alipay/src/main/java/com/magic/withdraw/alipay/AlipayWithdrawTrade.java](magic-withdraw-alipay/src/main/java/com/magic/withdraw/alipay/AlipayWithdrawTrade.java#L192)
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/TradeService.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/TradeService.java#L26)
- 建议：统一返回明确失败响应对象（success=false + message + code），禁止 return null。

### 3. 轮询任务使用 Timer，线程与生命周期管理不足

- 影响：异常隔离、线程池管理、停机回收与可观测性较弱，生产稳定性风险高。
- 定位：
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/impl/HandleProcessingOrderServiceImpl.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/impl/HandleProcessingOrderServiceImpl.java#L36)
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/impl/HandleProcessingOrderServiceImpl.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/impl/HandleProcessingOrderServiceImpl.java#L42)
- 建议：改为 Spring TaskScheduler 或 @Scheduled，并配置独立线程池与优雅停机。

## P1（本期建议处理）

### 4. 配置注入链路不一致，Demo 使用方式与核心读取方式不一致

- 影响：接入方按 Demo 编写后可能配置不生效，增加接入成本和排障时间。
- 定位：
  - [magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java](magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java#L50)
  - [magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java](magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java#L65)
  - [magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java](magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java#L103)
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/impl/PlatformConfigServiceImpl.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/impl/PlatformConfigServiceImpl.java#L20)
- 建议：统一只保留 PlatformConfigService 作为配置入口，移除或弃用 ThreadLocal 配置上下文方案。

### 5. 核心配置容器使用 HashMap，线程安全与可见性不足

- 影响：并发场景下可能出现竞态问题；静态全局状态也会降低测试可控性。
- 定位：
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/impl/PlatformConfigServiceImpl.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/service/impl/PlatformConfigServiceImpl.java#L20)
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/loader/MagicLoader.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/loader/MagicLoader.java#L25)
- 建议：改用 ConcurrentHashMap；限制 map 可见性为 private，提供只读访问。

### 6. Callback 配置类混用 @ConfigurationProperties 与 @Value

- 影响：配置来源不统一，维护复杂度增加，默认值策略分散。
- 定位：
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/domain/bean/CallbackConfig.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/domain/bean/CallbackConfig.java#L13)
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/domain/bean/CallbackConfig.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/domain/bean/CallbackConfig.java#L19)
  - [magic-withdraw-core/src/main/java/com/magic/withdraw/core/domain/bean/CallbackConfig.java](magic-withdraw-core/src/main/java/com/magic/withdraw/core/domain/bean/CallbackConfig.java#L22)
- 建议：统一使用 @ConfigurationProperties，默认值放字段初始值或构造参数。

## P2（排期优化）

### 7. 异常日志风格不统一，存在日志信息丢失

- 影响：线上排障效率下降，错误上下文不完整。
- 定位：
  - [magic-withdraw-wxpay/src/main/java/com/magic/withdraw/wxpay/WxpayWithdrawTrade.java](magic-withdraw-wxpay/src/main/java/com/magic/withdraw/wxpay/WxpayWithdrawTrade.java#L115)
  - [magic-withdraw-alipay/src/main/java/com/magic/withdraw/alipay/AlipayWithdrawTrade.java](magic-withdraw-alipay/src/main/java/com/magic/withdraw/alipay/AlipayWithdrawTrade.java#L214)
- 建议：统一使用 log.error("...", e)；禁止 printStackTrace。

### 8. Demo 出现敏感配置硬编码，不利于示例安全实践

- 影响：示例被复制到真实环境时存在泄漏风险。
- 定位：
  - [magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java](magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java#L45)
  - [magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java](magic-withdraw-demo/src/main/java/com/magic/withdraw/service/DemoService.java#L58)
- 建议：改为环境变量或配置中心读取，并在示例中使用占位符。

## 建议落地顺序

1. 先做 P0：路由匹配、非 null 契约、调度模型。
2. 再做 P1：配置入口统一、并发容器升级、配置绑定统一。
3. 最后做 P2：日志规范与 Demo 安全化。

## 验收建议

- 功能验收：三渠道的单笔提现、查询、撤销、回调验签行为一致且可预期。
- 稳定性验收：轮询任务支持可控线程池、重启/停机无残留任务。
- 代码验收：禁止 return null 出现在 TradeService 实现中。
