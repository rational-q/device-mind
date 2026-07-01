<template>
  <div class="dashboard">
    <!-- 顶部: 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ stats.onlineDevices }}</div>
          <div class="stat-label">在线设备</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ stats.totalDevices }}</div>
          <div class="stat-label">设备总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card warn">
          <div class="stat-value">{{ stats.activeAlerts }}</div>
          <div class="stat-label">未处理告警</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ stats.dataPoints }}/s</div>
          <div class="stat-label">数据吞吐</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 中间: 实时曲线 + 告警列表 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="18">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>实时数据曲线</span>
              <div style="display:flex;gap:8px;align-items:center">
                <el-select v-model="selectedDevice" size="small" style="width:180px" placeholder="选择设备">
                  <el-option v-for="d in deviceOptions" :key="d.deviceId" :label="`${d.name}(${d.deviceId})`" :value="d.deviceId" />
                </el-select>
                <el-select v-model="selectedAttr" size="small" style="width:130px" placeholder="选择属性">
                  <el-option v-for="a in attrOptions" :key="a" :label="a" :value="a" />
                </el-select>
                <el-tag type="success" v-if="wsConnected">已连接</el-tag>
                <el-tag type="danger" v-else>未连接</el-tag>
              </div>
            </div>
          </template>
          <v-chart :option="chartOption" style="height:360px" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="alert-card">
          <template #header><span>实时告警</span></template>
          <div class="alert-list">
            <div v-for="(a, i) in alertList" :key="i" class="alert-item" :class="a.level.toLowerCase()">
              <div class="alert-time">{{ formatTime(a.timestamp) }}</div>
              <div class="alert-device">{{ a.deviceId }}</div>
              <div class="alert-rule">{{ a.ruleName }}</div>
              <el-tag :type="a.level === 'CRITICAL' ? 'danger' : 'warning'" size="small">{{ a.level }}</el-tag>
            </div>
            <div v-if="alertList.length === 0" style="color:#909399;text-align:center;padding:20px">暂无告警</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ElNotification } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, DataZoomComponent } from 'echarts/components'
import { getDeviceList } from '@/api/device'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, DataZoomComponent])

// 设备列表
const deviceOptions = ref<{ deviceId: string; name: string }[]>([])
const selectedDevice = ref('')
const selectedAttr = ref('temperature')
const attrOptions = ref(['temperature', 'humidity', 'voltage', 'current', 'active_power', 'smoke_concentration', 'motor_speed', 'vibration'])

// 统计数据
const stats = reactive({ onlineDevices: 0, totalDevices: 0, activeAlerts: 0, dataPoints: 0 })

// 告警列表
interface AlertItem { deviceId: string; ruleName: string; level: string; timestamp: number }
const alertList = ref<AlertItem[]>([])

// ECharts 曲线数据
const chartData = reactive({ time: [] as string[], value: [] as number[] })
const chartOption = computed(() => ({
  grid: { left: 50, right: 20, top: 20, bottom: 40 },
  tooltip: { trigger: 'axis' as const },
  xAxis: { type: 'category' as const, data: chartData.time, boundaryGap: false },
  yAxis: { type: 'value' as const },
  dataZoom: [{ type: 'inside' as const, start: 0, end: 100 }],
  series: [{
    type: 'line' as const,
    data: chartData.value,
    smooth: true,
    showSymbol: false,
    lineStyle: { width: 2 },
    areaStyle: { opacity: 0.1 },
  }]
}))

// WebSocket 连接
const wsConnected = ref(false)
let ws: WebSocket | null = null
let pingTimer: ReturnType<typeof setInterval> | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let reconnectAttempts = 0
let manualClose = false
let dataPointCount = 0
let lastStatReset = Date.now()

function connectWebSocket() {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  ws = new WebSocket(protocol + '//' + location.host + '/device-mind/core/ws/monitor')

  ws.onopen = () => {
    wsConnected.value = true
    reconnectAttempts = 0
    console.log('WebSocket 已连接')
    // 每30秒发送心跳，防止代理/负载均衡断开空闲连接
    pingTimer = setInterval(() => {
      if (ws?.readyState === WebSocket.OPEN) {
        ws.send('ping')
      }
    }, 30000)
  }

  ws.onclose = () => {
    wsConnected.value = false
    if (pingTimer) { clearInterval(pingTimer); pingTimer = null }
    // 组件已卸载/主动关闭时不再重连，避免僵尸连接与定时器泄漏
    if (manualClose) return
    // 指数退避重连：3s、6s、12s… 上限 30s
    reconnectAttempts++
    const delay = Math.min(3000 * 2 ** (reconnectAttempts - 1), 30000)
    console.log(`WebSocket 断开，${delay / 1000}秒后重连（第 ${reconnectAttempts} 次）`)
    reconnectTimer = setTimeout(connectWebSocket, delay)
  }

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)

      if (msg.type === 'device_data') {
        // 更新曲线
        if (msg.deviceId === selectedDevice.value && msg.attrName === selectedAttr.value) {
          const time = new Date(msg.timestamp * 1000).toLocaleTimeString()
          chartData.time.push(time)
          chartData.value.push(msg.value)
          if (chartData.time.length > 60) {
            chartData.time.shift()
            chartData.value.shift()
          }
        }
        // 统计吞吐
        dataPointCount++
        const now = Date.now()
        if (now - lastStatReset > 1000) {
          stats.dataPoints = dataPointCount
          dataPointCount = 0
          lastStatReset = now
        }
      } else if (msg.type === 'alert') {
        // 告警通知
        alertList.value.unshift({
          deviceId: msg.deviceId,
          ruleName: msg.ruleName,
          level: msg.level,
          timestamp: Date.now(),
        })
        if (alertList.value.length > 50) alertList.value.length = 50

        ElNotification({
          title: '告警: ' + msg.deviceId,
          message: msg.ruleName + (msg.currentValue ? ' (当前值: ' + msg.currentValue + ')' : ''),
          type: msg.level === 'CRITICAL' ? 'error' : 'warning',
          duration: 5000,
        })

        stats.activeAlerts++
      }
    } catch (e) {
      // ignore
    }
  }
}

onMounted(async () => {
  // 加载设备列表
  try {
    const res = await getDeviceList({ pageSize: 100 })
    deviceOptions.value = res.records.map((d: any) => ({ deviceId: d.deviceId, name: d.name || d.deviceId }))
    stats.totalDevices = res.total
    stats.onlineDevices = res.records.filter((d: any) => d.status === 'ONLINE').length
    if (deviceOptions.value.length) selectedDevice.value = deviceOptions.value[0].deviceId
  } catch (e) { /* ignore */ }

  connectWebSocket()
})

onUnmounted(() => {
  manualClose = true
  if (pingTimer) { clearInterval(pingTimer); pingTimer = null }
  if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
  ws?.close()
})

function formatTime(ts: number) {
  return new Date(ts).toLocaleTimeString()
}
</script>

<style scoped>
.dashboard { padding: 0; }
.stats-row { margin-bottom: 0 !important; }
.stat-card { text-align: center; border-radius: 8px; }
.stat-card .stat-value { font-size: 36px; font-weight: bold; color: #303133; }
.stat-card .stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.stat-card.warn .stat-value { color: #e64242; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-card { border-radius: 8px; }
.alert-card { border-radius: 8px; }
.alert-card :deep(.el-card__body) { padding: 8px; }
.alert-list { max-height: 360px; overflow-y: auto; }
.alert-item { padding: 8px; border-bottom: 1px solid #f0f0f0; font-size: 13px; line-height: 1.8; }
.alert-item:last-child { border-bottom: none; }
.alert-item .alert-time { color: #909399; font-size: 12px; }
.alert-item .alert-device { font-weight: bold; }
.alert-item .alert-rule { font-size: 12px; }
</style>
