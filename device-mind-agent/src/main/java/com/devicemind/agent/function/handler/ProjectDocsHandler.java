package com.devicemind.agent.function.handler;

import com.devicemind.agent.function.FunctionHandler;
import com.devicemind.agent.function.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 项目知识库工具
 * <p>
 * 当用户询问 DeviceMind 平台本身的架构、设计、技术方案、问题解决等问题时，
 * AI 调用此工具获取项目相关知识。
 */
@Slf4j
@Component
public class ProjectDocsHandler implements FunctionHandler {
    /** 项目知识库 */
    private static final Map<String, String> KNOWLEDGE = Map.ofEntries(
            Map.entry("overview", """
                    DeviceMind 是一个物联网设备管理平台，四个模块：
                    - device-mind-broker：MQTT Broker 接入服务，基于 Netty 实现，端口 1883。接收设备上报数据，
                      通过 Kafka 转发到 Core。支持 QoS 0/1，Session 持久化到 Redis。
                    - device-mind-core：核心业务服务，端口 8080。管理设备/产品/告警/场景联动，消费 Kafka 消息
                      写入 TimescaleDB（时序数据）和 MySQL（业务数据）。
                    - device-mind-agent：AI 智能助手，端口 8081。基于 DeepSeek 大模型，提供告警根因分析和
                      通用问答，支持 10 个 Function Calling 工具。
                    - device-mind-web：Vue 3 + TypeScript 前端，Vite 构建，Element Plus UI。
                    基础设施：MySQL 8.0 + TimescaleDB (PostgreSQL 15) + Redis 7 + Kafka + ZooKeeper。
                    """),
            Map.entry("message-chain", """
                    上行（设备上报）：
                    Device → MQTT(QoS1) → PublishHandler → Redis 持久化 → PUBACK 确认
                      → MessageForwarder → Kafka(acks=all+幂等) → DeviceDataConsumer(批量)
                      → TSDB 写入 → ack 提交 offset

                    下行（指令下发）：
                    Core API(/commands/send) → DeviceCommandProducer → Kafka(device-command)
                      → DeviceCmdConsumer(Broker) → CommandService → MQTT → 设备

                    异常处理：
                    - MQTT→Kafka 发送失败：消息标记 FAILED，KafkaCompensationScheduler 每 10s 扫描重试，
                      最多 10 次。超限进死信 + 告警。
                    - Kafka→Consumer 处理失败：抛 KafkaConsumeFailedException → CommonErrorHandler
                      指数退避重试(1s→2s→4s→8s→16s) → 失败写 DLT(<topic>.DLT)
                      → DLT 失败写本地文件(./kafka-dlq-fallback/) 兜底。
                    - 指令无响应：CommandRetrySupport 每 30s 扫描 SENT 状态指令，超时回退 PENDING 重试。
                    """),
            Map.entry("kafka-reliability", """
                    Kafka 消息零丢失方案（Producer + Broker + Consumer 三层防护）：

                    Producer 端：
                    - acks=all：等待所有 ISR 副本确认
                    - enable.idempotence=true：幂等生产者，防网络重试重复
                    - min.insync.replicas=2（生产）/1（本地）：至少 N 个副本在 ISR 中
                    - retries=10 + delivery.timeout.ms=30000
                    - @PreDestroy flush：优雅关闭时刷出缓冲区

                    Broker 端：
                    - 副本因子 3（生产），offset topic 3 副本
                    - unclean.leader.election=false：禁止非 ISR 副本选主
                    - 保留 72h + 10GB 上限

                    Consumer 端：
                    - enable-auto-commit=false + ack-mode=manual：手动提交
                    - 异常抛出 KafkaConsumeFailedException → ErrorHandler 指数退避重试
                    - setCommitRecovered(true)：DLT 写入成功后提交 offset
                    - 本地文件兜底：DLT 也写不进去时落本地 JSON

                    补偿机制：
                    - MQTT→Kafka 段：KafkaCompensationScheduler 10s 扫描 Redis FAILED_SET
                    - Kafka→Consumer 段：CommonErrorHandler 秒级重试
                    - 指令全链路：CommandRetrySupport 30s 扫描 DB
                    """),
            Map.entry("mqtt-session", """
                    MQTT Session 持久化方案（Redis）：

                    数据结构：
                    - session:{clientId}：Hash，存 clientId/connectedAt/lastHeartbeatAt/keepAlive
                    - subs:{clientId}：Set，存 topic filter 列表
                    - sessions:active：Set，所有活跃 session 的 clientId

                    TTL 机制：keepAlive × 2 + 60s，心跳续期。设备断开自动清理。
                    Broker 重启可从 Redis 恢复 session + 订阅。

                    QoS 1 消息处理：
                    1. PublishHandler 收到 PUBLISH → MqttMessageStore.save() 写 Redis
                    2. 发送 PUBACK 给设备（确认收到）
                    3. 异步转发 Kafka → 成功标记 DELIVERED，失败标记 FAILED
                    4. KafkaCompensationScheduler 定时补偿 FAILED 消息
                    """),
            Map.entry("agent-design", """
                    Agent 模块技术设计：

                    10 个 Function Calling 工具：
                    只读（AI 自主调用）：deviceInfo/deviceData/deviceShadow/alertHistory/alertRules/
                      deviceStatus/alertSummary/commandStats/nl2sql
                    待确认（写操作）：sendCommand → 返回 pendingAction，前端弹窗确认后才执行

                    安全措施：
                    - sendCommand 白名单：只允许 set_threshold/set_interval/set_mode/reboot 等安全命令
                    - AI 输出边界：超出平台范围的问题拒绝回答
                    - SQL 注入防护：NL2SQL 只允许 SELECT，自动加 LIMIT 200
                    - 最多 5 轮工具调用防止死循环

                    接口收敛后的结构：
                    POST /analysis/alert → 告警根因分析
                    POST /analysis/chat  → 通用问答（含自然语言查数据、指令下发确认）
                    （原 /analysis/query 已删除，收敛到 /chat）

                    业务层：IAnalysisBusiness → AnalysisBusiness（合并了 AlertAnalysisService 和 AgentService）
                    """),
            Map.entry("config-decisions", """
                    关键技术决策：

                    1. Kafka 替代 REST 直连：Core↔Broker 指令下发不直调 REST，走 Kafka 异步解耦
                       删除了 BrokerCommandClient 和 broker 侧 CommandController

                    2. DeviceDataConsumer 批量模式：batch=true，每批 ≤20 条，按 deviceId 分组、
                       时间戳排序后批量写 TSDB。相比逐条写入效率高 10-50 倍，同时保证单设备有序。

                    3. DeviceDataConsumer 不用线程池：同一设备消息必须顺序处理，否则后续数据可能
                       被旧数据覆盖。Kafka concurrency=3 已提供跨设备并行能力。

                    4. Redisson 替代 Lettuce 连接池：RedissonClient 提供分布式锁(RLock)、
                       分布式集合(RMap/RSet)，用于后续 MQTT session 跨 broker 节点共享。

                    5. Common 模块配置 @Import 显式加载：KafkaTopicConfig/KafkaErrorHandlerConfig/
                       RedissonConfig/TaskExecutorConfig 通过 @SpringBootApplication 的 @Import
                       显式导入，因为 common 不在默认组件扫描路径下。

                    6. 指令状态机：PENDING → SENT(已发 Kafka) → SUCCESS(设备响应)，
                       不再直接标 SUCCESS（等设备确认）。
                    """)
    );

    @Override
    public String getFunctionName() {
        return "projectDocs";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("projectDocs")
                        .description("""
                                查询 DeviceMind 平台的项目知识，包括架构设计、技术方案、消息链路设计、
                                Kafka 可靠性方案、MQTT Session 持久化、Agent 设计、关键决策等。
                                当用户问'这个项目是怎么设计的''消息怎么保证不丢''为什么要这样设计'等问题时调用。""")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("topic", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("查询主题：overview(概览)/message-chain(消息链路)/kafka-reliability(Kafka可靠性)/mqtt-session(MQTT会话)/agent-design(Agent设计)/config-decisions(关键决策)")
                                        .enumValues(KNOWLEDGE.keySet().stream().toList())
                                        .build())
                                .required(List.of("topic"))
                                .build())
                        .build())
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = JsonUtil.readTree(argumentsJson);
            String topic = args.get("topic").asText();
            String content = KNOWLEDGE.getOrDefault(topic,
                    "未知主题: " + topic + "，可选: " + String.join(", ", KNOWLEDGE.keySet()));
            return "{\"topic\":\"" + topic + "\",\"content\":\"" + escapeJson(content) + "\"}";
        } catch (Exception e) {
            return "{\"error\":\"查询失败: " + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
