# DeviceMind — 智能设备运维中枢

**给设备装上大脑** —— 基于自研 Netty MQTT Broker 与 AI Agent 的物联网设备监控与智能运维平台。

## 项目简介

DeviceMind 是一个面向中小型工厂、机房等场景的轻量级物联网监控与智能运维平台。核心特色是 **完全自研 MQTT Broker**（完整实现 MQTT 3.1.1 协议），并集成了 **AI 大模型（DeepSeek）** 实现告警智能分析、NL2SQL 自然语言查询和场景联动自动化。

## 系统架构

项目拆分为四个模块 + 一个前端：

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│  设备终端    │◄───►│  MQTT Broker │────►│   Kafka      │
│  (MQTT)     │     │   (1883)     │     │              │
└─────────────┘     └──────┬───────┘     └──────┬───────┘
                           │ REST API           │
                           ▼                    ▼
                    ┌─────────────────────────────────┐
                    │        Core 设备核心服务          │
                    │   (8080) 物模型/告警/场景/影子    │
                    └──────┬────────────┬──────────────┘
                           │            │
                           ▼            ▼
                    ┌──────────┐  ┌──────────┐
                    │ Agent AI │  │  Web 前端 │
                    │ (8081)   │  │ (5173)   │
                    └──────────┘  └──────────┘
```

| 服务 | 端口 | 职责 |
|------|------|------|
| **device-mind-broker** | 1883 (MQTT) / 1884 (REST) | 自研 Netty MQTT 协议网关 |
| **device-mind-core** | 8080 | 设备管理、物模型、告警引擎、场景联动 |
| **device-mind-agent** | 8081 | AI 告警分析、NL2SQL 查询、Function Calling |
| **device-mind-web** | 5173 | Vue 3 + Element Plus 管理后台 |

### 中间件

| 组件 | 用途 |
|------|------|
| MySQL 8.0 | 业务数据（设备/产品/告警/场景） |
| TimescaleDB (PG15) | 时序数据（设备上报数值） |
| Redis 7 | 缓存（预留） |
| Kafka + ZooKeeper | 设备数据消息队列 |

## 核心功能

### 📡 MQTT Broker（完整协议实现）

- 完整 MQTT 3.1.1：CONNECT / SUBSCRIBE / UNSUBSCRIBE / PUBLISH / PINGREQ / DISCONNECT
- QoS 0/1 支持，PUBACK 回复
- 主题通配符匹配（`+` 单层 / `#` 多层）
- 订阅关系管理（topic → Channel 映射）
- 心跳超时自动踢下线
- 设备连接认证（白名单 + Core 同步）
- 设备上下线状态实时同步 Core
- 指令下发（直接发送 / 按订阅路由）

### 🔌 设备管理

- 产品定义 + 物模型（属性/服务/事件）
- 设备注册、编辑、删除
- EAV 物模型：一套表兼容任意设备类型
- 设备影子（reported/desired 状态管理）
- 设备在线/离线/数据级心跳检测

### 🔔 告警体系

- 滑动窗口告警引擎（防抖判定，抑制瞬时波动误报）
- 告警规则 CRUD + 启用/禁用
- 告警确认 / 恢复
- CRITICAL 告警自动短信通知
- 告警自动 AI 分析（触发后异步调 Agent 分析，结果回写到 `ai_analysis` 字段）

### 🤖 AI Agent

- **DeepSeek API 集成**（支持超时重试、降级返回）
- **告警根因分析**：Function Calling 自动获取设备信息、时序数据、告警历史
- **NL2SQL 自然语言查数据**：自然语言 → SQL → 执行 TimescaleDB 查询
- **5 个工具函数**：deviceInfo / deviceData / deviceShadow / alertHistory / alertRules
- **Vue 3 对话界面**：支持快捷查询、告警分析弹窗

### ⚡ 场景联动

- 条件触发（设备属性 + 运算符 + 阈值）
- 动作链：COMMAND（指令下发）+ DELAY（延迟）+ SMS（短信通知）
- 执行记录日志
- 场景 CRUD + 启用/禁用

### 📊 Web 管理后台

- 实时大屏（WebSocket + ECharts 动态曲线，告警实时推送）
- 设备管理（列表/详情/创建/编辑）
- 产品与物模型管理
- 告警列表与规则配置
- 场景联动配置
- AI 助手对话界面
- 指令日志查询

### 🏗️ 基础设施

- 多数据源（MySQL MyBatis-Plus / TimescaleDB JDBC）
- Kafka 手动 ACK，不丢不重
- WebSocket 实时数据推送
- 指令重试（定时重试 + 设备上线投递）
- 定时数据清理（场景日志/指令日志/已恢复告警）
- 全链路 TraceId 追踪
- OpenAPI (Swagger) 接口文档

## 快速启动

### 前提条件

- Docker & Docker Compose
- JDK 17+（本地编译）
- Maven 3.8+
- Node.js 18+（前端）

### 1. 启动中间件

```bash
docker-compose up -d
```

启动 MySQL + TimescaleDB + Redis + Kafka + ZooKeeper。

### 2. 编译

```bash
mvn clean package -DskipTests
```

### 3. 启动服务

**方式一：本地 IDE**
- 启动 `BrokerApplication`（端口 1883/1884）
- 启动 `CoreApplication`（端口 8080）
- 启动 `AgentApplication`（端口 8081）

**方式二：Docker**

```bash
docker-compose --profile all up -d
```

### 4. 启动前端

```bash
cd device-mind-web
npm install
npm run dev
# 访问 http://localhost:5173
```

### 5. 配置 AI（可选）

```bash
export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_ROOT_PASSWORD` | `root123` | MySQL 密码 |
| `POSTGRES_PASSWORD` | `root123` | TimescaleDB 密码 |
| `REDIS_PASSWORD` | `root123` | Redis 密码 |
| `DEEPSEEK_API_KEY` | `(空)` | DeepSeek API Key |
| `DEVICE_OFFLINE_ALERT_PHONE` | `(空)` | 设备离线告警手机号 |
| `ALERT_CRITICAL_PHONE` | `(空)` | 严重告警通知手机号 |

## 项目结构

```
device-mind/
├── docker-compose.yml              # 中间件 + 服务编排
├── db/
│   ├── init.sql                    # 完整数据库 DDL（参考）
│   ├── init-mysql.sql              # MySQL 业务表 + 预置数据
│   └── init-timescaledb.sql        # TimescaleDB 时序超表
├── device-mind-common/             # 公共模块（DTO、工具类、Kafka 配置）
├── device-mind-broker/             # MQTT Broker 接入服务
│   ├── codec/                      # MQTT 编解码器
│   ├── handler/                    # 报文处理器（Connect/Publish/Subscribe 等）
│   ├── model/                      # 报文模型
│   ├── session/                    # 会话管理与订阅管理
│   └── service/                    # 指令下发、设备认证、消息转发
├── device-mind-core/               # 设备核心服务
│   ├── business/                   # 业务逻辑层
│   ├── controller/                 # REST API
│   ├── kafka/                      # Kafka 消费 + 数据处理器
│   ├── model/                      # 实体、DTO、VO
│   ├── service/                    # 告警引擎、场景引擎、清理任务
│   └── client/                     # 外部服务客户端（Broker/Agent）
├── device-mind-agent/              # AI Agent 服务
│   ├── client/                     # DeepSeek API 客户端 + Core API 客户端
│   ├── function/                   # Function Calling 框架
│   ├── service/                    # 告警分析、NL2SQL
│   └── controller/                 # AI 分析 API
└── device-mind-web/                # Vue 3 + TypeScript 前端
    ├── src/api/                    # API 接口层
    ├── src/views/                  # 页面组件
    └── src/router/                 # 路由配置
```

## API 文档

启动服务后访问：

| 服务 | Swagger 地址 |
|------|-------------|
| Core | http://localhost:8080/swagger-ui.html |
| Agent | http://localhost:8081/swagger-ui.html |

## 技术栈

**后端**
- Java 17 / Spring Boot 3.2 / Netty 4.1
- MyBatis-Plus 3.5 / MySQL 8.0 / TimescaleDB (PG15)
- Redis / Kafka / WebSocket
- DeepSeek API (AI)

**前端**
- Vue 3 / TypeScript / Vite
- Element Plus / ECharts / Axios

## 截图

（待补充）

## 开源协议

MIT License
