<template>
  <PageContainer title="设备数据">
    <el-form :model="query" label-width="70px">
      <el-row :gutter="16">
        <el-col :span="5">
          <el-form-item label="设备">
            <el-input v-model="query.deviceId" placeholder="设备ID" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="5">
          <el-form-item label="属性">
            <el-input v-model="query.attrName" placeholder="如 temperature" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="开始时间">
            <el-date-picker v-model="query.startTime" type="datetime" placeholder="开始时间" format="YYYY-MM-DD HH:mm:ss" value-format="X" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="结束时间">
            <el-date-picker v-model="query.endTime" type="datetime" placeholder="结束时间" format="YYYY-MM-DD HH:mm:ss" value-format="X" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="3">
          <el-form-item>
            <el-button type="primary" @click="fetchData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 快捷时间 -->
    <div style="margin-bottom: 12px">
      <el-button size="small" @click="quickTime(1)">最近1小时</el-button>
      <el-button size="small" @click="quickTime(6)">6小时</el-button>
      <el-button size="small" @click="quickTime(24)">24小时</el-button>
      <el-button size="small" @click="quickTime(168)">7天</el-button>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading" max-height="500">
      <el-table-column prop="time" label="时间" width="180">
        <template #default="{ row }">{{ formatTime(row.time) }}</template>
      </el-table-column>
      <el-table-column prop="deviceId" label="设备" width="150" />
      <el-table-column prop="attrName" label="属性" width="150" />
      <el-table-column prop="value" label="数值" width="150">
        <template #default="{ row }">
          {{ row.value != null ? row.value.toFixed(2) : row.valueText || '-' }}
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[50, 100, 200]"
      layout="total, sizes, prev, pager, next"
      @change="fetchData"
      style="margin-top: 16px; justify-content: flex-end"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { getDeviceDataList } from '@/api/deviceData'
import type { DeviceDataVO } from '@/types/deviceData'
import { ElMessage } from 'element-plus'

const query = reactive({
  deviceId: '',
  attrName: '',
  startTime: null as number | null,
  endTime: null as number | null,
  pageNum: 1,
  pageSize: 100,
})

const tableData = ref<DeviceDataVO[]>([])
const total = ref(0)
const loading = ref(false)

function quickTime(hours: number) {
  const now = Math.floor(Date.now() / 1000)
  query.startTime = now - hours * 3600
  query.endTime = now
  fetchData()
}

async function fetchData() {
  if (!query.deviceId.trim()) {
    ElMessage.warning('请输入设备ID')
    return
  }
  loading.value = true
  try {
    const res = await getDeviceDataList({
      deviceId: query.deviceId.trim(),
      attrName: query.attrName.trim() || undefined,
      start: query.startTime ?? undefined,
      end: query.endTime ?? undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    tableData.value = res.records; if (res.records.length > 0) ElMessage.success("查询到 " + res.total + " 条数据"); else ElMessage.info("未查到数据")
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.deviceId = ''
  query.attrName = ''
  query.startTime = null
  query.endTime = null
  query.pageNum = 1
  tableData.value = []
  total.value = 0
}

function formatTime(epoch: number) {
  if (!epoch) return '-'
  return new Date(epoch * 1000).toLocaleString('zh-CN')
}
</script>
