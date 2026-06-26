#!/usr/bin/env python3
"""模拟 MQTT 设备上报数据"""
import paho.mqtt.client as mqtt
import json
import time

# MQTT Broker 地址
BROKER = "localhost"
PORT = 1883

# 模拟智能门锁 B-201
DEVICE_ID = "B-201"
TOPIC = f"device/data/{DEVICE_ID}"

payload = {
    "deviceId": DEVICE_ID,
    "ts": int(time.time()),
    "attrs": {
        "lock_status": "UNLOCKED",
        "battery_level": 85,
        "last_unlock_method": "fingerprint"
    }
}

client = mqtt.Client(client_id=DEVICE_ID)
client.connect(BROKER, PORT, keepalive=60)
client.loop_start()
time.sleep(0.5)

client.publish(TOPIC, json.dumps(payload), qos=0)
print(f"已发送: topic={TOPIC}, payload={json.dumps(payload, ensure_ascii=False)}")

time.sleep(1)
client.loop_stop()
client.disconnect()
print("发送完成")
