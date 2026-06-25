# DeviceMind - 智能设备运维中枢

**给设备装上大脑** —— 基于自研 Netty MQTT Broker 与 AI Agent 的物联网设备监控与智能运维平台。

## 项目简介

DeviceMind 是一个面向中小型工厂、机房等场景的轻量级物联网监控与智能运维平台。核心特色是 **完全从零手写 MQTT Broker**，并集成了 **AI 大模型** 实现告警智能分析与自然语言数据查询。

## 系统架构

项目拆分为三个独立服务 + 一个公共模块：

- **device-mind-broker** (端口 1883)：自研 Netty MQTT 协议网关，负责设备长连接、协议解析、心跳维护
- **device-mind-core** (端口 8080)：设备管理、EAV 物模型、滑动窗口告警引擎、指令可靠下发
- **device-mind-agent** (端口 8081)：AI 告警分析、NL2SQL 自然语言查询、DeepSeek API 调用
- **device-mind-common**：公共 DTO 与工具类

## 核心功能

- **自研 MQTT Broker**：基于 Netty 从零实现 MQTT 3.1.1 协议，支持 QoS 0/1
- **EAV 物模型**：一套表结构兼容任意类型设备，新增设备类型零代码改动
- **滑动窗口告警引擎**：防抖告警判定，杜绝瞬时波动误报
- **指令可靠投递**：本地消息表 + 幂等键，保证指令不丢不重
- **AI 智能运维 Agent**：集成 DeepSeek，自动告警根因分析 + NL2SQL 自然语言查询
- **全链路容错**：AI 服务超时、重试、降级多层保护

## 快速启动

环境要求：JDK 17+、Maven 3.8+、Docker

1. 克隆项目
   git clone https://github.com/你的用户名/device-mind.git
   cd device-mind

2. 启动中间件
   docker-compose up -d

3. 配置 DeepSeek API Key（可选）
   export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx

4. 编译并启动三个服务
   mvn clean package -DskipTests
# 分别启动 broker、core、agent 三个模块

5. 验证：用 MQTTX 连接 localhost:1883，发送数据到 device/data/{deviceId}

## 技术栈

Java 17 / Spring Boot 3.2 / Netty 4.1 / MQTT 3.1.1 / TimescaleDB / MySQL 8.0 / Redis / DeepSeek API / Docker

## 项目结构

device-mind/
├── device-mind-common/          # 公共模块
├── device-mind-broker/          # MQTT 接入服务
│   ├── codec/                   # 编解码器
│   ├── handler/                 # 报文处理器
│   └── session/                 # 会话管理
├── device-mind-core/            # 设备核心服务
│   ├── controller/              # REST 接口
│   ├── service/                 # 业务逻辑
│   └── repository/              # 数据访问
├── device-mind-agent/           # AI Agent 服务
│   ├── service/                 # 告警分析、NL2SQL
│   └── client/                  # DeepSeek API 客户端
├── docker-compose.yml
└── pom.xml

## 开源协议

MIT License