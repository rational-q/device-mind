<template>
  <PageContainer title="仪表盘">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-statistic title="产品总数" :value="stats.products" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="设备总数" :value="stats.devices" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="在线设备" :value="stats.onlineDevices" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="活跃告警" :value="stats.activeAlerts" />
      </el-col>
    </el-row>
    <el-divider />
    <h3 style="margin-bottom: 16px">最近告警</h3>
    <el-table :data="recentAlerts" border stripe size="small">
      <el-table-column prop="deviceId" label="设备" width="120" />
      <el-table-column prop="ruleName" label="规则" />
      <el-table-column prop="level" label="等级" width="80" />
      <el-table-column prop="currentValue" label="当前值" width="100" />
      <el-table-column prop="threshold" label="阈值" width="80" />
      <el-table-column prop="triggeredAt" label="触发时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.triggeredAt) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'TRIGGERED' ? 'danger' : row.status === 'CONFIRMED' ? 'warning' : 'success'">
            {{ ALERT_STATUS_MAP[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { getAlertList } from '@/api/alert'
import { getProductList } from '@/api/product'
import { getDeviceList } from '@/api/device'
import { ALERT_STATUS_MAP } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import type { AlertVO } from '@/types/alert'

const stats = reactive({ products: 0, devices: 0, onlineDevices: 0, activeAlerts: 0 })
const recentAlerts = ref<AlertVO[]>([])

onMounted(async () => {
  const [products, devices, alerts] = await Promise.all([
    getProductList({ pageSize: 1 }),
    getDeviceList({ pageSize: 1 }),
    getAlertList({ pageSize: 10 }),
  ])
  stats.products = products.total
  stats.devices = devices.total
  stats.activeAlerts = alerts.total
  recentAlerts.value = alerts.records

  const onlineDevices = await getDeviceList({ status: 'ONLINE', pageSize: 1 })
  stats.onlineDevices = onlineDevices.total
})
</script>
