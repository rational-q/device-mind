-- ============================================================
-- DeviceMind 数据库初始化脚本 (dm_ 前缀统一版)
-- 执行顺序：
--   1. MySQL 中创建 devicemind 库，执行第一部分和第二部分
--   2. TimescaleDB(PostgreSQL) 中创建 devicemind_ts 库，执行第三部分
-- ============================================================

-- ============================================================
-- 第一部分：MySQL 业务表（在 devicemind 数据库中执行）
-- ============================================================

-- 1. 产品表
CREATE TABLE IF NOT EXISTS dm_product (
                                          ID BIGINT NOT NULL COMMENT '主键，应用层雪花ID',
                                          PRODUCT_KEY VARCHAR(32) NOT NULL COMMENT '产品标识',
                                          NAME VARCHAR(100) NOT NULL COMMENT '产品名称',
                                          DESCRIPTION VARCHAR(500) COMMENT '产品描述',
                                          PROTOCOL_TYPE VARCHAR(20) DEFAULT 'MQTT' COMMENT '接入协议',
                                          DATA_FORMAT VARCHAR(20) DEFAULT 'JSON' COMMENT '数据格式',
                                          STATUS VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
                                          CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          CREATED_BY BIGINT COMMENT '创建人ID',
                                          UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          UPDATED_BY BIGINT COMMENT '更新人ID',
                                          PRIMARY KEY (ID),
                                          UNIQUE KEY UK_PRODUCT_KEY (PRODUCT_KEY)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品定义表';

-- 2. 物模型-属性定义表
CREATE TABLE IF NOT EXISTS dm_thing_attribute (
                                                  ID BIGINT NOT NULL COMMENT '主键，应用层雪花ID',
                                                  PRODUCT_ID BIGINT NOT NULL COMMENT '所属产品ID',
                                                  IDENTIFIER VARCHAR(50) NOT NULL COMMENT '属性标识符',
                                                  NAME VARCHAR(100) NOT NULL COMMENT '属性名称',
                                                  DATA_TYPE VARCHAR(20) NOT NULL COMMENT '数据类型',
                                                  UNIT VARCHAR(20) COMMENT '单位',
                                                  ACCESS_MODE VARCHAR(20) DEFAULT 'RW' COMMENT '读写权限',
                                                  DESCRIPTION VARCHAR(200) COMMENT '属性描述',
                                                  CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                  CREATED_BY BIGINT COMMENT '创建人ID',
                                                  UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                  UPDATED_BY BIGINT COMMENT '更新人ID',
                                                  PRIMARY KEY (ID),
                                                  INDEX IDX_PRODUCT_ID (PRODUCT_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物模型属性定义表';

-- 3. 物模型-服务定义表
CREATE TABLE IF NOT EXISTS dm_thing_service (
                                                ID BIGINT NOT NULL COMMENT '主键，应用层雪花ID',
                                                PRODUCT_ID BIGINT NOT NULL COMMENT '所属产品ID',
                                                IDENTIFIER VARCHAR(50) NOT NULL COMMENT '服务标识',
                                                NAME VARCHAR(100) NOT NULL COMMENT '服务名称',
                                                CALL_TYPE VARCHAR(10) DEFAULT 'ASYNC' COMMENT '调用类型',
                                                DESCRIPTION VARCHAR(200) COMMENT '服务描述',
                                                CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                CREATED_BY BIGINT COMMENT '创建人ID',
                                                UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                UPDATED_BY BIGINT COMMENT '更新人ID',
                                                PRIMARY KEY (ID),
                                                INDEX IDX_PRODUCT_ID (PRODUCT_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物模型服务定义表';

-- 4. 服务参数表
CREATE TABLE IF NOT EXISTS dm_thing_service_param (
                                                      ID BIGINT NOT NULL COMMENT '主键，应用层雪花ID',
                                                      SERVICE_ID BIGINT NOT NULL COMMENT '所属服务ID',
                                                      IDENTIFIER VARCHAR(50) NOT NULL COMMENT '参数标识',
                                                      NAME VARCHAR(100) NOT NULL COMMENT '参数名称',
                                                      DATA_TYPE VARCHAR(20) NOT NULL COMMENT '数据类型',
                                                      REQUIRED TINYINT DEFAULT 0 COMMENT '是否必填',
                                                      UNIT VARCHAR(20) COMMENT '单位',
                                                      DESCRIPTION VARCHAR(200) COMMENT '参数描述',
                                                      CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                      CREATED_BY BIGINT COMMENT '创建人ID',
                                                      UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                      UPDATED_BY BIGINT COMMENT '更新人ID',
                                                      PRIMARY KEY (ID),
                                                      INDEX IDX_SERVICE_ID (SERVICE_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务参数表';

-- 5. 物模型-事件定义表
CREATE TABLE IF NOT EXISTS dm_thing_event (
                                              ID BIGINT NOT NULL COMMENT '主键，应用层雪花ID',
                                              PRODUCT_ID BIGINT NOT NULL COMMENT '所属产品ID',
                                              IDENTIFIER VARCHAR(50) NOT NULL COMMENT '事件标识',
                                              NAME VARCHAR(100) NOT NULL COMMENT '事件名称',
                                              TYPE VARCHAR(20) NOT NULL COMMENT '事件类型：INFO/ALERT/ERROR',
                                              DESCRIPTION VARCHAR(200) COMMENT '事件描述',
                                              CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              CREATED_BY BIGINT COMMENT '创建人ID',
                                              UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                              UPDATED_BY BIGINT COMMENT '更新人ID',
                                              PRIMARY KEY (ID),
                                              INDEX IDX_PRODUCT_ID (PRODUCT_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物模型事件定义表';

-- 6. 设备表
CREATE TABLE IF NOT EXISTS dm_device (
                                         ID BIGINT NOT NULL COMMENT '主键，应用层雪花ID',
                                         DEVICE_ID VARCHAR(64) NOT NULL COMMENT '设备唯一标识',
                                         PRODUCT_ID BIGINT NOT NULL COMMENT '所属产品ID',
                                         NAME VARCHAR(100) COMMENT '设备名称',
                                         LOCATION VARCHAR(200) COMMENT '安装位置',
                                         STATUS VARCHAR(20) DEFAULT 'OFFLINE' COMMENT '在线状态',
                                         LAST_ONLINE_TIME TIMESTAMP NULL COMMENT '最后上线时间',
                                         FIRMWARE_VERSION VARCHAR(50) COMMENT '固件版本',
                                         TAGS VARCHAR(500) COMMENT '标签',
                                         CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         CREATED_BY BIGINT COMMENT '创建人ID',
                                         UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         UPDATED_BY BIGINT COMMENT '更新人ID',
                                         PRIMARY KEY (ID),
                                         UNIQUE KEY UK_DEVICE_ID (DEVICE_ID),
                                         INDEX IDX_PRODUCT_ID (PRODUCT_ID),
                                         INDEX IDX_STATUS (STATUS)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

-- 7. 设备影子表
CREATE TABLE IF NOT EXISTS dm_device_shadow (
                                                DEVICE_ID VARCHAR(64) PRIMARY KEY COMMENT '设备ID',
                                                REPORTED TEXT COMMENT '设备上报的最新状态',
                                                DESIRED TEXT COMMENT '平台期望状态',
                                                REPORTED_VERSION INT DEFAULT 0,
                                                DESIRED_VERSION INT DEFAULT 0,
                                                UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                UPDATED_BY BIGINT COMMENT '更新人ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备影子表';

-- 8. 告警规则表
CREATE TABLE IF NOT EXISTS dm_alert_rule (
                                             ID BIGINT NOT NULL COMMENT '主键，应用层雪花ID',
                                             RULE_NAME VARCHAR(100) NOT NULL COMMENT '规则名称',
                                             DEVICE_TYPE VARCHAR(50) NOT NULL COMMENT '适用产品标识',
                                             ATTR_NAME VARCHAR(50) NOT NULL COMMENT '监控属性标识',
                                             OPERATOR VARCHAR(10) NOT NULL COMMENT '比较运算符',
                                             THRESHOLD DOUBLE NOT NULL COMMENT '阈值',
                                             DURATION_SECONDS INT NOT NULL DEFAULT 60 COMMENT '持续时间窗口',
                                             LEVEL VARCHAR(20) NOT NULL COMMENT '告警等级',
                                             ENABLED TINYINT DEFAULT 1 COMMENT '是否启用',
                                             CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             CREATED_BY BIGINT COMMENT '创建人ID',
                                             UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             UPDATED_BY BIGINT COMMENT '更新人ID',
                                             PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- 9. 告警事件表
CREATE TABLE IF NOT EXISTS dm_alert (
                                        ID BIGINT NOT NULL COMMENT '主键，应用层雪花ID',
                                        DEVICE_ID VARCHAR(64) NOT NULL COMMENT '设备ID',
                                        RULE_ID BIGINT NOT NULL COMMENT '规则ID',
                                        RULE_NAME VARCHAR(100) COMMENT '规则名称',
                                        LEVEL VARCHAR(20) NOT NULL COMMENT '告警等级',
                                        METRIC VARCHAR(50) COMMENT '监控属性',
                                        CURRENT_VALUE DOUBLE COMMENT '当前值',
                                        THRESHOLD DOUBLE COMMENT '阈值',
                                        TRIGGERED_AT TIMESTAMP NOT NULL COMMENT '触发时间',
                                        CONFIRMED_AT TIMESTAMP NULL COMMENT '确认时间',
                                        RESOLVED_AT TIMESTAMP NULL COMMENT '恢复时间',
                                        STATUS VARCHAR(20) NOT NULL DEFAULT 'TRIGGERED' COMMENT '状态',
                                        AI_ANALYSIS TEXT COMMENT 'AI分析结果',
                                        CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        CREATED_BY BIGINT COMMENT '创建人ID',
                                        UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        UPDATED_BY BIGINT COMMENT '更新人ID',
                                        PRIMARY KEY (ID),
                                        INDEX IDX_DEVICE_ID (DEVICE_ID),
                                        INDEX IDX_STATUS (STATUS)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警事件表';

-- 10. 指令记录表
CREATE TABLE IF NOT EXISTS dm_command_log (
                                              ID BIGINT NOT NULL COMMENT '主键，应用层雪花ID',
                                              DEVICE_ID VARCHAR(64) NOT NULL COMMENT '设备ID',
                                              COMMAND VARCHAR(100) NOT NULL COMMENT '指令标识',
                                              PARAMS JSON COMMENT '指令参数',
                                              IDEMPOTENCY_KEY VARCHAR(64) NOT NULL COMMENT '幂等键',
                                              STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
                                              RETRY_COUNT INT DEFAULT 0 COMMENT '重试次数',
                                              MAX_RETRIES INT DEFAULT 3 COMMENT '最大重试次数',
                                              ACKED_AT TIMESTAMP NULL COMMENT '确认时间',
                                              CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              CREATED_BY BIGINT COMMENT '创建人ID',
                                              UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                              UPDATED_BY BIGINT COMMENT '更新人ID',
                                              PRIMARY KEY (ID),
                                              UNIQUE KEY UK_IDEMPOTENCY (IDEMPOTENCY_KEY),
                                              INDEX IDX_DEVICE_ID (DEVICE_ID),
                                              INDEX IDX_STATUS (STATUS)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指令下发记录表';


-- ============================================================
-- 第二部分：预置数据
-- ============================================================

-- 产品
INSERT INTO dm_product (ID, PRODUCT_KEY, NAME, DESCRIPTION, CREATED_BY) VALUES
                                                                            (1, 'TEMP_SENSOR_V1', '温湿度传感器', '工业级温湿度传感器，支持-40~125℃量程', 0),
                                                                            (2, 'SMART_LOCK_V1', '智能门锁', '支持指纹/密码/刷卡多种开锁方式', 0),
                                                                            (3, 'SMOKE_DETECTOR_V1', '烟感探测器', '光电式烟雾探测器，灵敏度可调', 0),
                                                                            (4, 'SMART_METER_V1', '智能电表', '三相智能电表，支持电压/电流/功率/电量采集', 0),
                                                                            (5, 'INDUSTRIAL_PLC_V1', '工业PLC控制器', '可编程逻辑控制器，支持多路模拟量/数字量采集', 0);

-- 物模型属性
INSERT INTO dm_thing_attribute (ID, PRODUCT_ID, IDENTIFIER, NAME, DATA_TYPE, UNIT, ACCESS_MODE, DESCRIPTION, CREATED_BY) VALUES
                                                                                                                             (101, 1, 'temperature', '温度', 'DOUBLE', '℃', 'R', '当前环境温度', 0),
                                                                                                                             (102, 1, 'humidity', '湿度', 'DOUBLE', '%', 'R', '当前环境相对湿度', 0),
                                                                                                                             (103, 2, 'lock_status', '门锁状态', 'ENUM', NULL, 'R', 'LOCKED=上锁/UNLOCKED=解锁', 0),
                                                                                                                             (104, 2, 'battery_level', '电池电量', 'INT', '%', 'R', '剩余电量百分比', 0),
                                                                                                                             (105, 2, 'last_unlock_method', '最近开锁方式', 'STRING', NULL, 'R', 'fingerprint/password/card/remote', 0),
                                                                                                                             (106, 3, 'smoke_concentration', '烟雾浓度', 'DOUBLE', 'ppm', 'R', '当前烟雾浓度值', 0),
                                                                                                                             (107, 3, 'battery_level', '电池电量', 'INT', '%', 'R', '剩余电量百分比', 0),
                                                                                                                             (108, 4, 'voltage', '电压', 'DOUBLE', 'V', 'R', '当前电压', 0),
                                                                                                                             (109, 4, 'current', '电流', 'DOUBLE', 'A', 'R', '当前电流', 0),
                                                                                                                             (110, 4, 'active_power', '有功功率', 'DOUBLE', 'kW', 'R', '当前有功功率', 0),
                                                                                                                             (111, 4, 'total_energy', '累计电量', 'DOUBLE', 'kWh', 'R', '累计用电量', 0),
                                                                                                                             (112, 5, 'motor_speed', '电机转速', 'INT', 'rpm', 'R', '当前电机转速', 0),
                                                                                                                             (113, 5, 'oil_pressure', '油压', 'DOUBLE', 'MPa', 'R', '液压系统压力', 0),
                                                                                                                             (114, 5, 'vibration', '振动值', 'DOUBLE', 'mm/s', 'R', '设备振动监测值', 0),
                                                                                                                             (115, 5, 'running_status', '运行状态', 'ENUM', NULL, 'R', 'RUNNING=运行/STOPPED=停止/ALARM=故障', 0);

-- 告警规则
INSERT INTO dm_alert_rule (ID, RULE_NAME, DEVICE_TYPE, ATTR_NAME, OPERATOR, THRESHOLD, DURATION_SECONDS, LEVEL, CREATED_BY) VALUES
                                                                                                                                (201, '温度过高', 'TEMP_SENSOR_V1', 'temperature', '>', 35.0, 60, 'WARN', 0),
                                                                                                                                (202, '温度超高', 'TEMP_SENSOR_V1', 'temperature', '>', 45.0, 30, 'CRITICAL', 0),
                                                                                                                                (203, '烟雾浓度超标', 'SMOKE_DETECTOR_V1', 'smoke_concentration', '>', 5.0, 10, 'CRITICAL', 0),
                                                                                                                                (204, '门锁电池过低', 'SMART_LOCK_V1', 'battery_level', '<', 15, 60, 'WARN', 0),
                                                                                                                                (205, '烟感电池过低', 'SMOKE_DETECTOR_V1', 'battery_level', '<', 10, 60, 'WARN', 0),
                                                                                                                                (206, '电机转速异常', 'INDUSTRIAL_PLC_V1', 'motor_speed', '>', 3000, 30, 'WARN', 0),
                                                                                                                                (207, '振动超标', 'INDUSTRIAL_PLC_V1', 'vibration', '>', 4.0, 10, 'CRITICAL', 0),
                                                                                                                                (208, '油压过高', 'INDUSTRIAL_PLC_V1', 'oil_pressure', '>', 5.0, 10, 'CRITICAL', 0);


-- ============================================================
-- 第三部分：TimescaleDB 时序表（在 devicemind_ts 数据库中执行）
-- ============================================================

-- 设备数据时序表
CREATE TABLE IF NOT EXISTS device_data (
                                           time TIMESTAMPTZ NOT NULL,
                                           device_id VARCHAR(64) NOT NULL,
                                           attr_name VARCHAR(50) NOT NULL,
                                           value DOUBLE PRECISION
);

-- 转为超表
SELECT create_hypertable('device_data', 'time', if_not_exists => TRUE);

-- 索引
CREATE INDEX IF NOT EXISTS idx_device_data_device_time ON device_data (device_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_device_data_attr_time ON device_data (attr_name, time DESC);

-- 7天后自动压缩
SELECT add_compression_policy('device_data', INTERVAL '7 days', if_not_exists => TRUE);

-- 90天后自动删除
SELECT add_retention_policy('device_data', INTERVAL '90 days', if_not_exists => TRUE);