# DeviceMind — 智能设备运维中枢

**给设备装上大脑** —— 自研 Netty MQTT Broker + AI Agent 的物联网设备监控与智能运维平台。

## 系统架构

```
┌──────────┐   MQTT(QoS1)   ┌──────────────┐   Kafka    ┌──────────────┐
│ 设备终端  │◄──────────────►│  MQTT Broker │──────────►│   Core 核心   │
│ (MQTT)   │   Redis Session │  (1883)     │  device-* │   (8080)     │
└──────────┘                 └──────┬───────┘  topics   └───┬──────┬───┘
                                    │ Kafka                  │      │
                                    │ device-command         │ REST │ WebSocket
                                    ▼                        ▼      ▼
                             ┌──────────────┐   REST   ┌──────────┐  ┌──────────┐
                             │  Agent AI    │◄────────►│  Web 前端 │  │  Web 前端 │
                             │  (8081)      │          │  (5173)  │  │  (5173)  │
                             └──────────────┘          └──────────┘  └──────────┘
```

| 服务 | 端口 | 职责 |
|------|------|------|
| **device-mind-broker** | 1883 (MQTT) / 1884 (Actuator) | 自研 Netty MQTT Broker，Kafka 桥接，Redis Session |
| **device-mind-core** | 8080 | 设备管理、物模型、告警引擎、场景联动、指令下发 |
| **device-mind-agent** | 8081 | DeepSeek AI 助手：告警分析、通用问答（11 个工具） |
| **device-mind-web** | 5173 | Vue 3 + Element Plus 管理后台 |

### 中间件

| 组件 | 用途 |
|------|------|
| MySQL 8.0 | 业务数据（设备/产品/告警/场景/指令日志） |
| TimescaleDB (PG15) | 时序数据（设备上报指标） |
| Redis 7 | MQTT Session 持久化 + 消息存储 + 补偿队列 |
| Kafka | 设备数据/状态/指令/回执 异步消息总线 |
| Redisson 3.x | 分布式锁与分布式集合 |

## 消息可靠性（零丢失方案）

### 上行链路：设备 → Core

```
设备 PUBLISH(QoS1)
  → PublishHandler: Redis 先落盘 → PUBACK 确认
  → MessageForwarder: 异步发送 Kafka (acks=all + 幂等 + min.insync.replicas=2)
  → KafkaCompensationScheduler: 每 10s 扫描 FAILED 消息重试，最多 10 次
  → DeviceDataConsumer: 批量消费(≤20条)，按 deviceId 分组+时间戳排序，批量写 TSDB
  → 异常: KafkaConsumeFailedException → CommonErrorHandler 指数退避重试(1s→2s→4s→8s→16s)
  → 重试耗尽: DLT topic (<topic>.DLT) → 本地文件兜底(./kafka-dlq-fallback/)
```

### 下行链路：Core → 设备

```
POST /commands/send → DeviceCommandProducer → Kafka(device-command)
  → DeviceCmdConsumer(Broker) → CommandService → MQTT → 设备
  → 设备响应 → Kafka(device-response) → DeviceResponseConsumer → 更新 SUCCESS
  → 无响应: CommandRetrySupport 每 30s 扫描 SENT 指令重试
```

### 补偿机制（三层）

| 层级 | 机制 | 间隔 |
|------|------|------|
| Kafka Producer 内置 | acks=all + retries=10 + delivery.timeout=30s | 毫秒级 |
| MQTT→Kafka 补偿 | KafkaCompensationScheduler 扫描 Redis FAILED_SET | 10s |
| 指令全链路补偿 | CommandRetrySupport 扫描 DB PENDING/SENT 状态 | 30s |

## 核心功能

### 📡 MQTT Broker

- 完整 MQTT 3.1.1 协议：CONNECT / SUBSCRIBE / PUBLISH / PINGREQ / DISCONNECT
- QoS 0/1，QoS 1 消息先 Redis 持久化再 PUBACK
- MQTT Session Redis 持久化（TTL=keepAlive×2+60s，心跳续期）
- Broker 重启后可恢复 session + 订阅
- 主题通配符匹配（`+` 单层 / `#` 多层）
- 心跳超时自动踢下线
- 设备认证（白名单，通过 Kafka lifecycle 事件同步）

### 🔌 设备管理

- 产品定义 + 物模型（属性/服务/事件）
- 设备注册、编辑、删除，EAV 模型兼容任意设备类型
- 设备影子（reported/desired 状态）
- 设备在线/离线/心跳检测

### 🔔 告警体系

- 滑动窗口告警引擎（防抖判定）
- 告警规则 CRUD + 启用/禁用
- CRITICAL 告警自动短信通知
- 触发后异步调 Agent AI 分析，结果回写 `ai_analysis`

### 🤖 AI Agent（11 个工具）

**接口**: `POST /analysis/alert` | `POST /analysis/chat`

| 工具 | 类型 | 用途 |
|------|------|------|
| `deviceInfo` | 只读 | 设备基本信息 |
| `deviceData` | 只读 | N 小时时序数据 |
| `deviceShadow` | 只读 | 设备影子 |
| `alertHistory` | 只读 | 历史告警 |
| `alertRules` | 只读 | 告警规则配置 |
| `deviceStatus` | 只读 | 在线状态统计 |
| `alertSummary` | 只读 | 告警概览 |
| `commandStats` | 只读 | 指令成功率 |
| `nl2sql` | 只读 | 自然语言→SQL 查数据 |
| `projectDocs` | 只读 | 项目架构/技术方案知识库 |
| `sendCommand` | 待确认 | 指令下发（白名单+前端确认） |

安全设计：`sendCommand` 只生成参数不执行，返回 `pendingAction` 等前端用户二次确认。

### ⚡ 场景联动

- 条件触发 + 动作链：COMMAND → DELAY → SMS
- 指令通过 Kafka 异步下发，状态追踪
- 场景 CRUD + 启用/禁用

### 📊 Web 管理后台

- 实时大屏（WebSocket + ECharts 动态曲线）
- 设备管理、产品与物模型、告警列表与规则、场景联动
- 指令下发页面（模板快捷填充 + JSON 参数校验）
- AI 助手对话界面（含 pendingAction 确认弹窗）
- 指令日志查询

## 快速启动

### 前提条件

- JDK 17+ / Maven 3.8+ / Node.js 18+
- Docker & Docker Compose（或本地安装 MySQL/Redis/Kafka/TimescaleDB）

### 1. 启动中间件

```bash
docker-compose up -d mysql timescaledb redis zookeeper kafka
```

### 2. 编译

```bash
mvn clean install -DskipTests
```

### 3. 启动后端

```bash
# 三个终端分别启动
mvn spring-boot:run -pl device-mind-broker -Dspring-boot.run.profiles=dev
mvn spring-boot:run -pl device-mind-core -Dspring-boot.run.profiles=dev
mvn spring-boot:run -pl device-mind-agent -Dspring-boot.run.profiles=dev
```

### 4. 启动前端

```bash
cd device-mind-web
npm install && npm run dev
# 访问 http://localhost:5173
```

### 5. 配置 AI（可选）

```bash
export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_ROOT_PASSWORD` | — | MySQL root 密码 |
| `MYSQL_DATABASE` | `devicemind` | MySQL 库名 |
| `POSTGRES_PASSWORD` | — | TimescaleDB 密码 |
| `REDIS_PASSWORD` | — | Redis 密码 |
| `DEEPSEEK_API_KEY` | — | DeepSeek API Key |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 地址 |
| `DEVICE_OFFLINE_ALERT_PHONE` | — | 设备离线通知手机号 |
| `ALERT_CRITICAL_PHONE` | — | 严重告警通知手机号 |

## 项目结构

```
device-mind/
├── docker-compose.yml
├── db/
│   ├── init-mysql.sql
│   └── init-timescaledb.sql
├── device-mind-common/             # 共享模块
│   └── config/                     # Kafka/Topic/ErrorHandler/Redisson/线程池
│   └── kafka/producer/             # 5 个 Producer (Data/Status/Lifecycle/Cmd/Response)
│   └── support/                    # IdempotentGuard (幂等守卫)
├── device-mind-broker/             # MQTT Broker
│   ├── codec/                      # MQTT 编解码
│   ├── handler/                    # Connect/Subscribe/Publish/Disconnect/Heartbeat
│   ├── session/                    # SessionManager + SessionStore(Redis)
│   ├── kafka/forwarder/            # MessageForwarder (MQTT→Kafka)
│   ├── kafka/consumer/             # DeviceCmdConsumer + DeviceLifecycleConsumer
│   ├── kafka/compensation/         # KafkaCompensationScheduler (补偿定时器)
│   └── service/                    # MqttMessageStore / CommandService / DeviceAuthService
├── device-mind-core/               # 核心服务
│   ├── business/                   # IDeviceBusiness / IDeviceShadowBusiness 等
│   ├── controller/                 # REST API (设备/产品/告警/场景/指令)
│   ├── kafka/consumer/             # DeviceDataConsumer(批量) / Status / Response
│   ├── support/                    # ActionSupport / CommandRetrySupport
│   └── client/                     # AlertAnalysisClient (调 Agent)
├── device-mind-agent/              # AI Agent
│   ├── business/                   # IAnalysisBusiness → AnalysisBusiness
│   ├── client/                     # DeepSeekClient / CoreApiClient
│   ├── function/                   # FunctionHandler + FunctionRegistry + ToolDefinition
│   │   └── handler/                # 11 个 Function Handler
│   └── model/                      # ChatRequest/Response, AlertAnalysis 等
└── device-mind-web/                # Vue 3 前端
    ├── src/api/                    # agent / command / device / alert / scene 等
    ├── src/views/                  # dashboard / device / alert / command / agent / scene
    └── src/router/                 # 路由配置
```

## API 文档

| 服务 | Swagger |
|------|---------|
| Core | http://localhost:8080/swagger-ui.html |
| Agent | http://localhost:8081/swagger-ui.html |

Broker 为内部服务，不对外暴露 Swagger。

## 技术栈

**后端**: Java 17 / Spring Boot 3.2 / Netty 4.1 / MyBatis-Plus 3.5 / Redisson 3.50  
**数据**: MySQL 8.0 / TimescaleDB (PG15) / Redis 7 / Kafka (cp-kafka 7.6)  
**AI**: DeepSeek API (Function Calling)  
**前端**: Vue 3 / TypeScript / Vite / Element Plus / ECharts

## 开源协议

MIT License
