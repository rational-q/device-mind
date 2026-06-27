# DeviceMind 架构设计文档

> 本文档从技术选型、架构设计、协议实现、数据模型、AI 集成等多个维度，完整阐述 DeviceMind 平台的设计思路与实现细节。适合作为面试项目介绍的话术素材。

---

## 一、项目概述

**DeviceMind** 是一个面向中小型工厂、机房等 IoT 场景的**设备监控与智能运维平台**。核心能力：

- **自研 MQTT Broker**：从零实现 MQTT 3.1.1 协议，不依赖 EMQX/VerneMQ 等开源 Broker
- **EAV 物模型**：一套表结构兼容任意设备类型，新增设备类型零代码改动
- **滑动窗口告警引擎**：防抖告警判定，杜绝瞬时波动误报
- **场景联动**：设备条件触发 → 自动执行动作链（指令下发 + 短信通知）
- **AI 智能运维**：集成 DeepSeek 大模型，Function Calling 实现告警根因分析 + NL2SQL

### 解决的问题

传统 IoT 平台（如阿里云 IoT、腾讯云 IoT Hub）是商业闭源产品，自研方案的优势：

1. **完全可控**：MQTT 协议细节可定制，不受厂商限制
2. **成本可控**：无商业授权费用，适合中小规模部署
3. **技术展示**：完整呈现物联网全栈技术能力（端侧协议、服务端架构、数据管道、AI 集成）

---

## 二、技术选型与原因

### 2.1 为什么选 Java 17 + Spring Boot 3.2？

| 因素 | 说明 |
|------|------|
| **生态成熟** | Spring Boot 是 Java 领域最成熟的生产框架，Netty、Kafka、MyBatis-Plus 等库支持完善 |
| **Java 17 特性** | Records、Sealed Classes、Pattern Matching 简化代码，ZGC 低延迟 GC 适合 IoT 服务端 |
| **虚拟线程** | Spring Boot 3.2 + JDK 21（可升级）支持虚拟线程，大幅提升 Netty 以外的 IO 密集型任务吞吐 |
| **团队背景** | Java 开发者供给充足，维护成本低 |

### 2.2 为什么自研 MQTT Broker，而不是用 EMQX/VerneMQ？

| 方案 | 优势 | 劣势 |
|------|------|------|
| **EMQX** | 成熟、高并发、集群成熟 | 开源版功能受限，Erlang 难以定制，部署重 |
| **VerneMQ** | 兼容 MQTT 5.0 | 同样是 Erlang，社区不如 EMQX |
| **自研 Netty** | **完全可控、代码量证明能力、面试可以深入聊** | 开发周期长，极端性能不如 C/Erlang |

**选择自研的核心考量**：这是一个**简历项目**。使用 EMQX 只是配个配置文件，没有任何技术亮点。自研 MQTT Broker 展示了：
- 对 **MQTT 协议规范**的深入理解（报文编码、变长编码、QoS 流程）
- **Netty 网络编程**能力（ChannelPipeline、EventLoop、ByteBuf）
- **高并发设计**能力（线程模型、内存管理）

### 2.3 为什么选 Netty？

Netty 是 Java 领域最主流的 NIO 框架：

- **事件驱动模型**：ChannelPipeline 责任链模式，每个协议处理阶段独立
- **零拷贝**：FileRegion + CompositeByteBuf 减少内存拷贝
- **线程模型**：EventLoopGroup 分离 Boss/Worker，Reactor 模式
- **编解码分离**：ByteToMessageDecoder / MessageToByteEncoder，编解码与业务分离

> 面试话术："Netty 是 Java NIO 的标杆框架，很多中间件（Dubbo、RocketMQ、Spring WebFlux）底层都依赖它。"

### 2.4 为什么数据存储用 MySQL + TimescaleDB 双库？

| 数据类型 | 存储 | 原因 |
|----------|------|------|
| **业务数据**（设备/产品/告警/场景） | MySQL 8.0 | 关系型数据，事务支持，MyBatis-Plus 便捷 CRUD |
| **时序数据**（设备上报值） | TimescaleDB | 自动分区、自动压缩、自动过期，SQL 兼容 |

**为什么不用 InfluxDB / TDengine？**

- TimescaleDB 是 **PostgreSQL 插件**，完全兼容 SQL，学习成本低
- InfluxDB 使用 Flux 查询语言，TDengine 有自己的 SQL 方言，生态不如 PostgreSQL
- TimescaleDB 的自动压缩和保留策略对 IoT 场景非常实用

### 2.5 为什么用 Kafka 做消息队列？

| 因素 | 说明 |
|------|------|
| **削峰填谷** | 设备上报速率不可控，Kafka 作为缓冲区，Core 按自身能力消费 |
| **异步解耦** | Broker 只负责协议解析，不关心数据怎么处理，Core 独立部署和扩缩 |
| **持久化** | Kafka 日志持久化，Broker 崩溃不丢数据 |
| **分区并行** | Core 配置 3 个并发线程，对应 3 个分区，水平扩展 |

### 2.6 为什么 AI 选 DeepSeek 而不是 ChatGPT/通义千问？

| 模型 | 优势 | 劣势 |
|------|------|------|
| **DeepSeek** | 价格极低（1/20 的 GPT-4），中文能力强，支持 Function Calling | 极端场景不如 GPT-4 |
| **GPT-4** | 综合能力最强 | 价格高，中国区访问受限 |
| **通义千问** | 阿里云生态 | 不支持 Function Calling（或支持不完善） |

DeepSeek 的 **API 格式兼容 OpenAI**，切换成本几乎为零，可以在 `application.yml` 换一个 URL 和 Key 就切换模型。

### 2.7 前端为什么选 Vue 3 + Element Plus？

团队技术栈偏好 + 生态成熟。Vue 3 Composition API + TypeScript 开发体验好，Element Plus 组件库开箱即用。

---

## 三、MQTT Broker 设计

### 3.1 协议实现范围

| 报文类型 | 状态 | 说明 |
|----------|------|------|
| CONNECT / CONNACK | ✅ | 含设备认证、ClientId 校验 |
| PUBLISH (QoS 0/1) | ✅ | QoS 1 回复 PUBACK |
| PUBACK | ✅ | 适配 QoS 1 流程 |
| SUBSCRIBE / SUBACK | ✅ | 支持通配符 + 和 # |
| UNSUBSCRIBE / UNSUBACK | ✅ | 支持批量取消 |
| PINGREQ / PINGRESP | ✅ | 心跳响应 |
| DISCONNECT | ✅ | 清理会话和订阅 |
| 心跳超时 | ✅ | IdleStateHandler + 自动踢下线 |

### 3.2 编解码设计

```
ByteBuf → MqttDecoder → MqttMessage → Handler → 业务处理
                                        ←
ByteBuf ← MqttEncoder ←  ByteBuf    ← 手动构造
```

**关键点**：解码器按 MQTT 协议规范解析固定头部、剩余长度（变长编码）、可变头部、有效载荷。每个报文类型对应一个 Handler，通过 ChannelPipeline 串联。

```java
pipeline.addLast(new IdleStateHandler(heartbeatTimeout, 0, 0));
pipeline.addLast(new HeartbeatTimeoutHandler());  // 心跳超时踢下线
pipeline.addLast(new MqttDecoder());              // 协议解码
pipeline.addLast(new MqttEncoder());              // 协议编码
pipeline.addLast(new ConnectHandler());            // 连接处理 + 认证
pipeline.addLast(new SubscribeHandler());          // 订阅处理
pipeline.addLast(new PublishHandler());            // 消息发布
pipeline.addLast(new PingReqHandler());            // 心跳处理
pipeline.addLast(new DisconnectHandler());         // 断开处理
```

### 3.3 订阅管理

订阅关系核心数据结构：
```java
ConcurrentHashMap<String, Set<Channel>> subscriptions  // topicFilter → [Channel]
ConcurrentHashMap<String, Set<String>> channelTopics    // channelId → [topicFilter]
```

通配符匹配算法：将 topic filter 和设备实际 topic 按 `/` 分割后逐段比较，`+` 匹配任意一段，`#` 匹配剩余所有段。

### 3.4 指令下发

两种模式：
1. **直接发送**：Core → Broker API → 根据 deviceId 查 Session → 直接写入 Channel
2. **主题发布**：Core → Broker API → SubscriptionManager 匹配订阅者 → 写入所有匹配 Channel

**MQTT PUBLISH 报文手动构造**（不依赖任何 MQTT 库）：

```java
ByteBuf buf = Unpooled.buffer();
buf.writeByte(0x30);                    // 固定报头: QoS 0 PUBLISH
writeRemainingLength(buf, remaining);   // 剩余长度变长编码
buf.writeShort(topicBytes.length);      // 主题长度
buf.writeBytes(topicBytes);             // 主题
buf.writeBytes(payloadBytes);           // 有效载荷
```

---

## 四、物模型与数据模型

### 4.1 EAV 模型

传统方案：每种设备类型一张表 → 新增类型需要 DDL 变更。

EAV 方案：
```
dm_product               ← 产品定义（温湿度传感器/智能门锁/电表）
dm_thing_attribute       ← 属性定义（temperature/humidity/voltage）
dm_thing_service         ← 服务定义（开机/关机/重置）
dm_thing_event           ← 事件定义（告警/故障）

dm_device                ← 设备实例（A-102/风扇01）
device_data (TimescaleDB) ← 时序数据（time/deviceId/attrName/value/valueText）
```

**新增设备类型只需 INSERT，不需要 ALTER TABLE**。

### 4.2 数据类型处理

| 物模型类型 | 存储位置 | 示例 |
|-----------|----------|------|
| INT / LONG / DOUBLE / FLOAT | `value` 列 | `temperature: 30.5` |
| STRING / ENUM / BOOLEAN | `value_text` 列 | `lock_status: "LOCKED"` |

### 4.3 设备影子

```
reported: 设备最近一次上报的状态（设备 → 平台）
desired:  平台期望设备达到的状态（平台 → 设备）
```

影子机制用于：
- 设备离线时暂存期望状态，上线后同步
- 读取设备最新状态不需查时序数据库

---

## 五、告警引擎设计

### 5.1 滑动窗口算法

```
触发条件: temperature > 35, duration = 60s

时间轴:  30°  36°  37°  38°  39°  (now)
         ↑__________60s窗口_________│
         窗口内所有点 > 35 → 触发

时间轴:  33°  36°  37°  (now)
         ↑_____60s窗口____│
         窗口内有 33° 未违反阈值 → 不触发（防抖）
```

实现步骤：
1. 当前数据点触发阈值 → 进入滑动窗口检查
2. 查询 TimescaleDB 窗口期内该设备该属性的所有数据
3. 全部违反阈值 → 确认持续异常 → 创建告警
4. 存在非违反点 → 认为是瞬时波动 → 不触发

### 5.2 告警去重

同一设备同一规则下，已有 `TRIGGERED` 状态的告警时不重复创建。

### 5.3 AI 分析自动触发

```
告警创建
  → 异步线程调 Agent API
  → Agent 通过 Function Calling 获取:
     - deviceInfo（设备基本信息）
     - deviceData（最近1小时时序数据）
     - alertHistory（最近24小时告警历史）
  → DeepSeek 综合分析
  → 结果回写 dm_alert.ai_analysis
```

---

## 六、场景联动设计

### 6.1 触发条件

```json
[
  {"attr": "temperature", "operator": ">", "value": 40, "duration": 30}
]
```

- 支持多条件组合（数组，满足任一即触发）
- 支持持续时长判定（同告警引擎滑动窗口）

### 6.2 动作链

```json
[
  {"type": "COMMAND", "targetDeviceId": "FAN-01", "command": "turnOn", "params": {}},
  {"type": "DELAY", "seconds": 10},
  {"type": "SMS", "phoneNumbers": ["13800138000"], "content": "温度异常，已开风扇"}
]
```

动作类型：
| 类型 | 说明 | 实现 |
|------|------|------|
| COMMAND | MQTT 指令下发 | Core → Broker API → 设备 |
| DELAY | 延迟执行 | Thread.sleep |
| SMS | 短信通知 | SmsService（可对接真实网关） |

---

## 七、AI Agent 设计

### 7.1 Function Calling 工作流

```
用户请求分析
  → 发送消息 + 5 个工具定义给 DeepSeek
  → DeepSeek 决定调用 deviceInfo 获取设备信息
  → Agent 执行函数，调 Core API
  → 把结果送回 DeepSeek
  → DeepSeek 决定调用 deviceData 获取时序数据
  → Agent 执行函数
  → DeepSeek 综合所有数据产出分析
  → 返回结构化结果 (摘要/原因/建议/严重程度)
```

### 7.2 工具函数

| 函数名 | Core API | 用途 |
|--------|----------|------|
| `deviceInfo` | `POST /device-mind/devices/list` | 设备名称、产品、状态、位置 |
| `deviceData` | `POST /device-mind/device-data/list` | 最近 N 小时时序数据 |
| `deviceShadow` | `GET /device-mind/shadows` | 最新上报/期望值 |
| `alertHistory` | `POST /device-mind/alerts/list` | 最近 N 小时告警历史 |
| `alertRules` | `POST /device-mind/alert-rules/list` | 告警规则配置 |

### 7.3 降级策略

- API Key 未配置 → 返回 `success: false` + 提示信息
- API 超时/失败 → 最多重试 2 次，全部失败返回 null
- JSON 解析失败 → 直接返回 AI 原始文本作为摘要

---

## 八、数据管道与实时性

### 8.1 数据流

```
设备 → MQTT PUBLISH → Broker
  → PublishHandler 解码 + 回复 PUBACK
  → MessageForwarder 发送到 Kafka (device-data topic)
  → DeviceDataConsumer 消费 Kafka
    ├─ DeviceDataProcessor.process()
    │   ├─ saveData() → TimescaleDB
    │   ├─ updateShadow() → MySQL
    │   ├─ alertEngine.evaluate() → 告警引擎
    │   └─ sceneEngine.evaluate() → 场景联动
    ├─ webSocketHandler.broadcast() → 实时大屏
    └─ ack.acknowledge() → 提交 offset
```

### 8.2 Kafka 手动 ACK

配置 `enable-auto-commit: false` + `ack-mode: manual`，在代码中显式调用 `ack.acknowledge()`：
- 处理成功 → ack，提交 offset
- 处理失败 → 不 ack，消息重新投递
- 无效消息（设备不存在）→ ack 跳过

### 8.3 WebSocket 实时推送

```
Core → WebSocketHandler → ws://host:8080/ws/monitor → 前端大屏

推送消息类型:
1. device_data: 实时设备上报数据 → ECharts 曲线更新
2. alert: 告警事件 → 右上角 Notification 弹窗
```

---

## 九、可靠性设计

### 9.1 指令重试

```java
@Scheduled(fixedDelay = 30000)
public void retryPendingCommands() {
    // 查询 PENDING/SENT 状态的指令
    // 重试发送（上限 5 次）
    // 超限标记 EXPIRED
}
```

### 9.2 设备上线投递

设备上线时，Core 自动查询该设备是否有待发送指令并立即投递。

### 9.3 数据级心跳检测

每 5 分钟扫描 ONLINE 设备：
```
查询 TimescaleDB 中该设备最近一条数据时间
→ 超过 30 分钟无数据 → 标记 OFFLINE → 短信通知
```

### 9.4 定时数据清理

每天凌晨 2 点：
- 场景日志 > 30 天 → 删除
- 指令日志 > 30 天 → 删除
- 已恢复告警 > 7 天 → 删除

---

## 十、安全设计

| 层面 | 措施 |
|------|------|
| 设备接入 | Broker 启动时从 Core 同步设备白名单，连接时校验 |
| 设备注册 | Core 创建/删除设备时主动通知 Broker |
| 设备状态 | 上下线自动同步 Core，离线短信通知 |
| API 鉴权 | （TODO：待接入 JWT） |

---

## 十一、部署架构

### 最小部署（1 台服务器 + Docker）

```
Docker Compose:
├── MySQL 8.0
├── TimescaleDB PG15
├── Redis 7
├── Kafka + ZooKeeper
├── Broker (1883/1884)
├── Core (8080)
├── Agent (8081)
└── Web (Nginx 转发 80 → 5173)
```

### 扩展方式

| 组件 | 扩展方式 |
|------|----------|
| Broker | 无状态，水平扩展（需加负载均衡） |
| Core | 无状态，水平扩展（Kafka 分区数决定了最大并行度） |
| Kafka | 增加分区数提升并行消费能力 |
| TimescaleDB | 按时间分区，可启用 chunk 级并行查询 |

---

## 十二、面试常见问题

### Q: 为什么不用 EMQX 要自己写 MQTT Broker？

> 这是一个展示技术深度的项目。使用 EMQX 只是配置几个参数，没有技术含量。自研 MQTT Broker 展示了我对 MQTT 协议规范、Netty 网络编程、高并发 IO 处理的理解。包含完整的报文编解码、变长编码解析、QoS 流程、订阅通配符匹配等。同时，自研方案可以灵活定制认证逻辑（设备白名单），与业务系统的集成更紧密。

### Q: MQTT Broker 的性能如何？能支持多少设备？

> 当前实现基于 Netty 的 Reactor 模型，单机可以支持数千并发连接。性能瓶颈主要在内存和文件描述符限制。如果生产环境需要更高并发，可以在 Netty 层面调优（调整 IO 线程数、TCP 参数、ByteBuf 分配器），也可以水平扩展多个 Broker 实例加负载均衡。

### Q: 为什么用 TimescaleDB 不用 InfluxDB？

> TimescaleDB 是 PostgreSQL 插件，完全兼容 SQL，学习成本和迁移成本极低。团队成员不需要学习新的查询语言。InfluxDB 的 Flux 查询语法相对小众。TimescaleDB 的自动压缩和保留策略对 IoT 场景非常适用，90 天前的数据自动清理，7 天前的数据自动压缩。

### Q: AI 分析的准确率怎么样？

> Function Calling 机制让 AI 能获取实时设备数据，基于真实数据的分析比纯文本推理准确得多。但 AI 不是规则引擎，适合辅助决策而不是自动化决策。我们的设计是：告警引擎负责确定性规则判断，AI 负责根因分析和建议，分工明确。

### Q: 这个项目还有什么可以改进的？

> 1. 用户认证和权限管理（目前 userId 写死）
> 2. MQTT QoS 2 支持
> 3. 消息持久化（设备离线时暂存消息，上线后推送）
> 4. 多租户支持
> 5. 单元测试和集成测试覆盖
> 6. CI/CD 流水线

---

> **本文档对应项目版本**: v1.0.0-SNAPSHOT  
> **最后更新**: 2026-06-27
