# DeviceMind 架构设计文档

## 一、系统概览

DeviceMind 是一个面向中小型工厂/机房的物联网设备监控与智能运维平台。核心特色是自研 MQTT Broker（完整 MQTT 3.1.1）和 AI Agent（DeepSeek Function Calling）。

```
┌─────────────────────────────────────────────────────────────────────┐
│                        DeviceMind Platform                          │
├──────────┬──────────┬──────────┬──────────┬──────────┬─────────────┤
│ MQTT     │ Kafka    │ MySQL    │ TSDB     │ Redis    │ DeepSeek    │
│ Broker   │ 消息总线  │ 业务库   │ 时序库   │ 缓存/存储 │ AI 大模型   │
│ (Netty)  │          │          │(PG15)    │(Redisson)│             │
└────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴──────┬──────┘
     │          │          │          │          │            │
     ▼          ▼          ▼          ▼          ▼            ▼
┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐
│ Broker  ││  Core   ││  Core   ││  Core   ││Broker/  ││ Agent   │
│ MQTT    ││ Kafka   ││Business ││ TSDB    ││Core/    ││ AI      │
│ 接入    ││Consumer ││ Logic   ││ Query   ││Agent    ││ Service │
│ 1883    ││         ││         ││         ││         ││ 8081    │
└─────────┘└─────────┘└─────────┘└─────────┘└─────────┘└─────────┘
```

## 二、模块职责

### 2.1 device-mind-broker — MQTT 接入服务

| 组件 | 说明 |
|------|------|
| `BrokerServer` | Netty TCP Server，监听 1883 |
| Pipeline | IdleStateHandler → HeartbeatTimeout → MqttDecoder → MqttEncoder → 业务 Handler |
| `SessionManager` | 设备会话管理（内存 ConcurrentHashMap + Redis 持久化双写） |
| `SessionStore` | Redis 会话存储（Redisson RMap/RSet） |
| `SubscriptionManager` | MQTT 主题订阅管理 |
| `MessageForwarder` | MQTT → Kafka 桥接，按 topic 前缀路由 |
| `MqttMessageStore` | QoS 1 消息 Redis 持久化（ACK_PENDING→DELIVERED/FAILED 状态机） |
| `KafkaCompensationScheduler` | 10s 定时扫描 FAILED 消息重试 |
| `CommandService` | 向在线设备发送 MQTT PUBLISH |
| `DeviceAuthService` | 设备白名单认证，通过 Kafka lifecycle 事件同步 |

**协议支持**：MQTT 3.1.1，QoS 0/1，cleanSession，sessionPresent，通配符订阅（+/#）

### 2.2 device-mind-core — 核心业务服务

| 组件 | 说明 |
|------|------|
| `DeviceDataConsumer` | Kafka 批量消费，≤20 条/批，按 deviceId 分组+时间戳排序后批量写 TSDB |
| `DeviceStatusConsumer` | 设备上下线事件 → 更新数据库状态 |
| `DeviceResponseConsumer` | 设备指令回执 → 更新指令日志 SUCCESS |
| `CommandRetrySupport` | 30s 定时扫描 PENDING/SENT 指令，设备上线触发投递 |
| `ActionSupport` | 场景联动动作执行器（COMMAND/DELAY/SMS） |
| `AlertEngine` | 滑动窗口告警判定 |
| `CommandController` | `POST /commands/send` 指令下发 API |
| Business 层 | 设备/产品/告警/场景/物模型/影子的 CRUD 业务逻辑 |

### 2.3 device-mind-agent — AI 智能服务

| 组件 | 说明 |
|------|------|
| `AnalysisBusiness` | 核心编排，统一告警分析和通用问答 |
| `DeepSeekClient` | OpenAI 兼容的 DeepSeek API 客户端，支持 Function Calling |
| `CoreApiClient` | 调 Core REST API 获取设备/数据/告警/指令信息 |
| `FunctionRegistry` | 自动发现和调度 FunctionHandler |
| `ConversationStore` | Redis 滑动窗口 + AI 摘要记忆（30min TTL） |
| 11 个 Handler | deviceInfo/deviceData/deviceShadow/alertHistory/alertRules/deviceStatus/alertSummary/commandStats/nl2sql/projectDocs/sendCommand |

### 2.4 device-mind-common — 共享模块

| 组件 | 说明 |
|------|------|
| `KafkaProducerConfig` | 5 个 Producer Bean + 全局 ProducerListener |
| `KafkaTopicConfig` | 显式创建 5 业务 topic + 5 DLT topic（3分区/3副本） |
| `KafkaErrorHandlerConfig` | 指数退避重试 + DLT + 本地文件兜底 |
| `RedissonConfig` | 纯 Redisson 客户端（无 Lettuce） |
| `TaskExecutorConfig` | Kafka Consumer 线程池 + 通用异步池 |
| `IdempotentGuard` | Caffeine 内存幂等守卫 |
| `JsonUtil` | 全局唯一 ObjectMapper（Long→String 防 JS 溢出） |

## 三、消息链路

### 3.1 上行：设备 → Core

```
Device ──PUBLISH(QoS1)──▶ PublishHandler
  ├─ MqttMessageStore.save() → Redis (ACK_PENDING)
  ├─ writeAndFlush(PUBACK) → 确认收到
  └─ MessageForwarder.forwardWithStore()
       ├─ topic 匹配 device/command/* → DeviceResponseProducer → device-response
       └─ 其他 → DeviceDataProducer → device-data

Kafka 发送成功 → markDelivered() (DELIVERED)
Kafka 发送失败 → markFailed() → FAILED_SET → KafkaCompensationScheduler(10s) 重试
重试 10 次仍失败 → markDead() 死信

Kafka → DeviceDataConsumer(batch=true)
  ├─ 解析 JSON → DeviceDataRequest
  ├─ 按 deviceId 分组 → 按时间戳排序（保证同设备有序）
  ├─ 按 productKey 路由 Processor → 批量写 TimescaleDB
  └─ WebSocket 广播 → ack.acknowledge()

异常处理:
  抛出 KafkaConsumeFailedException
  → CommonErrorHandler 指数退避 (1s→2s→4s→8s→16s)
  → 5次耗尽 → DLT topic (<topic>.DLT)
  → DLT 失败 → 本地文件 (./kafka-dlq-fallback/)
  → setCommitRecovered(true) 提交 offset
```

### 3.2 下行：Core → 设备

```
前端/Agent → POST /commands/send
  → DeviceCommandProducer.sendCommandAsync()
  → Kafka device-command

Broker DeviceCmdConsumer
  → 解析 DeviceCommandEvent
  → CommandService.sendCommand(deviceId, topic, payload)
  → 查 SessionManager → 设备在线 → MQTT PUBLISH
  → 设备离线 → 抛异常 → Kafka 重试

设备响应 → MQTT PUBLISH device/command/{deviceId}
  → MessageForwarder → DeviceResponseProducer → device-response
  → Core DeviceResponseConsumer → 更新 DmCommandLog.status=SUCCESS

无响应 → CommandRetrySupport(30s) 扫描 SENT 指令
  → 重试发送 → 超过 5 次 → EXPIRED
```

### 3.3 补偿机制（三层）

| 层级 | 组件 | 间隔 | 存储 |
|------|------|------|------|
| Producer 内置 | KafkaTemplate (acks=all + retries=10) | 毫秒 | Kafka 缓冲区 |
| MQTT→Kafka | KafkaCompensationScheduler | 10s | Redis FAILED_SET |
| 指令全链路 | CommandRetrySupport | 30s | MySQL dm_command_log |

## 四、Kafka 可靠性设计

### Producer 配置
```yaml
acks: all                        # 等所有 ISR 副本确认
enable.idempotence: true         # 幂等生产者
max.in.flight.requests: 5        # 幂等开启后安全
min.insync.replicas: 2           # 生产环境 ≥2
retries: 10                      # 重试上限
delivery.timeout.ms: 30000       # 含重试的总超时
```

### Consumer 配置
```yaml
enable-auto-commit: false        # 手动提交
ack-mode: manual                 # 显式 ack
max-poll-records: 20             # 每批 ≤20 条
max-poll-interval-ms: 600000     # 10min 防止踢出
concurrency: 3                   # 3 分区并行
```

### Topic 设计
| Topic | 分区 | 副本 | 保留 | 用途 |
|-------|------|------|------|------|
| device-data | 3 | 3 | 7d | 设备上报数据 |
| device-status | 3 | 3 | 7d | 上下线状态 |
| device-lifecycle | 3 | 3 | 7d | 注册/注销 |
| device-command | 3 | 3 | 7d | 指令下发 |
| device-response | 3 | 3 | 7d | 设备回执 |
| *.DLT | 3 | 3 | 7d | 死信队列 |

## 五、Redis 使用

| 模块 | Key | 类型 | TTL | 用途 |
|------|-----|------|-----|------|
| SessionStore | `session:{clientId}` | RMap | keepAlive×2+60s | MQTT 会话 |
| SessionStore | `subs:{clientId}` | RSet | 同 session | 订阅列表 |
| SessionStore | `sessions:active` | RSet | 永久 | 活跃设备集合 |
| MqttMessageStore | `msg:store:{uuid}` | RMap | 1h | QoS 1 消息 |
| MqttMessageStore | `msg:failed:set` | RSet | 永久 | 失败消息 ID |
| MqttMessageStore | `msg:inflight:{clientId}` | RList | 1h | 设备 inflight |
| MqttMessageStore | `msg:dedup:{key}` | RBucket | 30min | 去重 |
| ConversationStore | `chat:session:{id}:msgs` | RList | 30min | 对话消息 |
| ConversationStore | `chat:session:{id}:summary` | RBucket | 30min | AI 摘要 |

全部使用 Redisson 3.50，JSON 序列化，无 Lettuce。

## 六、Agent 对话记忆设计

```
用户发消息
  → ConversationStore.getMessages(sessionId, systemPrompt, userMessage)
  → 构建上下文: system + [summary] + 滑动窗口消息 + 当前消息
  → DeepSeek Function Calling
  → AI 回复
  → ConversationStore.saveRound(sessionId, userMsg, assistantMsg)
    → Redis RList 追加 2 条
    → 超过 20 条 → summarizeInBackground()
      → 取最早 10 条 → DeepSeek 压缩为摘要
      → 摘要存入 RBucket
      → RList.trim() 保留最近 10 条

前端 sessionId → localStorage 持久化 → 刷新/新 tab 不丢
Pinia store messages → localStorage 双向同步
```

## 七、前端架构

```
Vue 3 + TypeScript + Vite + Element Plus + ECharts
├── Pinia stores (chat, app)
├── Axios + 拦截器 (Result<T> 统一解包)
├── 路由: 仪表盘/产品/设备/告警/场景/指令/AI助手/实时大屏/设备数据
└── Vite proxy: /device-mind/core → 8080, /device-mind/agent → 8081
```

## 八、关键技术决策

| 决策 | 原因 |
|------|------|
| Kafka 替代 REST 直连 | Broker↔Core 异步解耦，削峰填谷 |
| DeviceDataConsumer 批量模式 | 批量写 TSDB 效率高 10-50 倍，`batch=true` |
| 不用线程池处理同设备消息 | 必须保序，否则旧数据覆盖新数据 |
| Snowflake ID → JSON String | 防止 JavaScript 数字溢出（>2^53） |
| Redisson 替代 Lettuce | 统一 API，内置分布式锁/集合 |
| ConversationStore 后端管理 | 前端只传 sessionId，不传历史，节省带宽 |
| sendCommand 待确认模式 | 写操作需用户二次确认，AI 不能自主执行 |
| 指令状态 PENDING→SENT→SUCCESS | 等设备确认，不提前标成功 |

## 九、部署架构

```
docker-compose up -d
├── mysql:3306          (业务库)
├── timescaledb:5432    (时序库)
├── redis:6379          (缓存/会话/消息存储)
├── zookeeper:2181      (Kafka 依赖)
├── kafka:9092          (消息队列)
├── broker:1883/1884    (MQTT 接入)
├── core:8080           (核心服务)
└── agent:8081          (AI 服务)
```

本地开发：中间件 docker-compose / 后端 IDEA / 前端 `npm run dev`（Vite 5173 反向代理后端）

## 十、API 文档

| 服务 | Swagger UI |
|------|-----------|
| Core | http://localhost:8080/swagger-ui.html |
| Agent | http://localhost:8081/swagger-ui.html |
| Broker | 内部服务，不对外暴露 |
