<template>
  <PageContainer :title="`设备详情 - ${device?.name || ''}`">
    <template #actions><el-button @click="$router.back()">返回</el-button></template>
    <el-descriptions v-if="device" :column="2" border>
      <el-descriptions-item label="设备ID">{{ device.deviceId }}</el-descriptions-item>
      <el-descriptions-item label="产品">{{ device.productName }}</el-descriptions-item>
      <el-descriptions-item label="状态"><el-tag :type="device.status === 'ONLINE' ? 'success' : 'info'">{{ DEVICE_STATUS_MAP[device.status] }}</el-tag></el-descriptions-item>
      <el-descriptions-item label="位置">{{ device.location }}</el-descriptions-item>
      <el-descriptions-item label="固件版本">{{ device.firmwareVersion }}</el-descriptions-item>
      <el-descriptions-item label="标签">{{ device.tags }}</el-descriptions-item>
      <el-descriptions-item label="最后上线">{{ formatDateTime(device.lastOnlineTime) }}</el-descriptions-item>
      <el-descriptions-item label="注册时间">{{ formatDateTime(device.createdDate) }}</el-descriptions-item>
    </el-descriptions>

    <el-divider />
    <h3 style="margin-bottom: 16px">设备影子</h3>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="上报状态 (Reported)">
          <pre v-if="shadow" style="max-height:300px;overflow:auto">{{ JSON.stringify(shadow.reported, null, 2) || '无' }}</pre>
          <el-empty v-else description="无影子数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="期望状态 (Desired)">
          <pre v-if="shadow" style="max-height:300px;overflow:auto">{{ JSON.stringify(shadow.desired, null, 2) || '无' }}</pre>
          <el-empty v-else description="无影子数据" />
          <template #footer v-if="shadow">
            <el-button type="primary" @click="openShadowDialog">编辑期望状态</el-button>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog title="编辑期望状态" v-model="shadowDialog" width="500px">
      <el-input v-model="desiredJson" type="textarea" :rows="10" placeholder='{"key":"value"}' />
      <template #footer><el-button @click="shadowDialog = false">取消</el-button><el-button type="primary" @click="saveShadow">保存</el-button></template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import { getDeviceById } from '@/api/device'
import { getDeviceShadow, updateDeviceShadow } from '@/api/deviceData'
import { DEVICE_STATUS_MAP } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import type { DeviceVO } from '@/types/device'
import type { ShadowVO } from '@/types/deviceData'

const route = useRoute()
const device = ref<DeviceVO | null>(null)
const shadow = ref<ShadowVO | null>(null)
const shadowDialog = ref(false)
const desiredJson = ref('{}')

onMounted(async () => {
  const id = route.query.id as string
  device.value = await getDeviceById(id)
  shadow.value = await getDeviceShadow(device.value!.deviceId)
})

function openShadowDialog() {
  desiredJson.value = JSON.stringify(shadow.value?.desired || {}, null, 2)
  shadowDialog.value = true
}

async function saveShadow() {
  try {
    const desired = JSON.parse(desiredJson.value)
    await updateDeviceShadow(device.value!.deviceId, { desired })
    shadow.value = await getDeviceShadow(device.value!.deviceId)
    shadowDialog.value = false
  } catch {
    console.error('JSON 格式错误')
  }
}
</script>
