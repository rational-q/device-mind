<template>
  <PageContainer title="实时监控">
    <el-form label-width="80px">
      <el-row :gutter="16">
        <el-col :span="8"><el-form-item label="设备">
          <el-select v-model="selectedDeviceId" placeholder="选择设备" @change="fetchData" style="width:100%">
            <el-option v-for="d in devices" :key="d.deviceId" :label="`${d.name} (${d.deviceId})`" :value="d.deviceId" />
          </el-select>
        </el-form-item></el-col>
        <el-col :span="5"><el-form-item label="属性"><el-input v-model="attrName" placeholder="如 temperature" clearable @change="fetchData" /></el-form-item></el-col>
        <el-col :span="7"><el-form-item label="时间范围">
          <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至" @change="fetchData" style="width:100%" />
        </el-form-item></el-col>
        <el-col :span="3"><el-form-item><el-button type="primary" @click="fetchData">查询</el-button></el-form-item></el-col>
      </el-row>
    </el-form>
    <el-table :data="tableData" border stripe size="small" v-loading="loading">
      <el-table-column label="时间" width="180"><template #default="{ row }">{{ formatDateTime(new Date(row.time * 1000)) }}</template></el-table-column>
      <el-table-column prop="deviceId" label="设备" width="150" />
      <el-table-column prop="attrName" label="属性" width="150" />
      <el-table-column prop="value" label="值" />
    </el-table>
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { getDeviceList } from '@/api/device'
import { getDeviceDataList } from '@/api/deviceData'
import { formatDateTime } from '@/utils/date'
import type { DeviceVO } from '@/types/device'
import type { DeviceDataVO } from '@/types/deviceData'

const devices = ref<DeviceVO[]>([])
const selectedDeviceId = ref('')
const attrName = ref('')
const timeRange = ref<[Date, Date] | null>(null)
const tableData = ref<DeviceDataVO[]>([])
const loading = ref(false)

onMounted(async () => {
  const res = await getDeviceList({ pageSize: 100 })
  devices.value = res.records
  selectedDeviceId.value = devices.value[0]?.deviceId || ''
  if (selectedDeviceId.value) fetchData()
})

async function fetchData() {
  if (!selectedDeviceId.value) return
  loading.value = true
  try {
    const params: any = { deviceId: selectedDeviceId.value, pageSize: 200 }
    if (attrName.value) params.attrName = attrName.value
    if (timeRange.value) {
      params.start = Math.floor(timeRange.value[0].getTime() / 1000)
      params.end = Math.floor(timeRange.value[1].getTime() / 1000)
    }
    const res = await getDeviceDataList(params)
    tableData.value = res.records
  } finally { loading.value = false }
}
</script>
