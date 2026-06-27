#!/usr/bin/env python3
"""向所有设备模拟多轮真实数据上报"""
import paho.mqtt.client as mqtt
import json
import time
import random

BROKER = "localhost"
PORT = 1883

# 设备模拟数据生成器
def gen_temp_sensor(device_id, ts_offset=0):
    return {
        "deviceId": device_id,
        "ts": int(time.time()) + ts_offset,
        "attrs": {
            "temperature": round(random.uniform(20.0, 35.0), 1),
            "humidity": round(random.uniform(40.0, 85.0), 1)
        }
    }

def gen_smart_lock(device_id, ts_offset=0):
    return {
        "deviceId": device_id,
        "ts": int(time.time()) + ts_offset,
        "attrs": {
            "lock_status": random.choice(["LOCKED", "UNLOCKED"]),
            "battery_level": random.randint(75, 98),
            "last_unlock_method": random.choice(["fingerprint", "password", "card", "remote"])
        }
    }

def gen_smoke_detector(device_id, ts_offset=0):
    return {
        "deviceId": device_id,
        "ts": int(time.time()) + ts_offset,
        "attrs": {
            "smoke_concentration": round(random.uniform(0.3, 3.5), 2),
            "battery_level": random.randint(65, 95)
        }
    }

def gen_smart_meter(device_id, base_load, ts_offset=0):
    voltage = round(random.uniform(225.0, 238.0), 1)
    current = round(base_load + random.uniform(-3, 5), 1)
    return {
        "deviceId": device_id,
        "ts": int(time.time()) + ts_offset,
        "attrs": {
            "voltage": voltage,
            "current": current,
            "active_power": round(voltage * current / 1000, 2),
            "total_energy": round(base_load * 1000 + random.uniform(100, 500), 1)
        }
    }

def gen_plc(device_id, running, ts_offset=0):
    if running:
        return {
            "deviceId": device_id,
            "ts": int(time.time()) + ts_offset,
            "attrs": {
                "motor_speed": random.randint(800, 2800),
                "oil_pressure": round(random.uniform(1.5, 4.0), 2),
                "vibration": round(random.uniform(0.5, 2.8), 2)
            }
        }
    else:
        return {
            "deviceId": device_id,
            "ts": int(time.time()) + ts_offset,
            "attrs": {
                "motor_speed": 0,
                "oil_pressure": 0.05,
                "vibration": round(random.uniform(0.01, 0.08), 3)
            }
        }

# 设备配置：(device_id, generator_func, extra_args)
devices = [
    ("TH-S01", gen_temp_sensor, {}),
    ("TH-S02", gen_temp_sensor, {}),
    ("TH-S03", gen_temp_sensor, {}),
    ("LOCK-M01", gen_smart_lock, {}),
    ("LOCK-M02", gen_smart_lock, {}),
    ("SMOKE-W01", gen_smoke_detector, {}),
    ("SMOKE-W02", gen_smoke_detector, {}),
    ("METER-M01", gen_smart_meter, {"base_load": 42}),
    ("METER-M02", gen_smart_meter, {"base_load": 18}),
    ("PLC-P01", gen_plc, {"running": True}),
    ("PLC-P02", gen_plc, {"running": False}),
]

client = mqtt.Client(client_id="seed-script")
client.connect(BROKER, PORT, keepalive=60)
client.loop_start()
time.sleep(0.5)

# 每台设备发送 5 轮数据（间隔 2 秒，模拟历史趋势）
total = 0
for round_num in range(5):
    for device_id, gen_func, kwargs in devices:
        payload = gen_func(device_id, ts_offset=round_num * 2)
        topic = f"device/data/{device_id}"
        client.publish(topic, json.dumps(payload), qos=0)
        total += 1
        time.sleep(0.05)  # 避免消息堆积
    print(f"第 {round_num + 1} 轮发送完成")
    time.sleep(1)

time.sleep(2)
client.loop_stop()
client.disconnect()
print(f"共计发送 {total} 条设备数据")
