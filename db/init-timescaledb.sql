-- ============================================================
-- DeviceMind TimescaleDB 初始化脚本
-- 挂载到 docker-entrypoint-initdb.d 自动执行
-- ============================================================

-- 设备数据时序表
CREATE TABLE IF NOT EXISTS device_data (
                                           time TIMESTAMPTZ NOT NULL,
                                           device_id VARCHAR(64) NOT NULL,
                                           attr_name VARCHAR(50) NOT NULL,
                                           value DOUBLE PRECISION,
                                           value_text TEXT
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
