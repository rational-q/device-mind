<template>
  <PageContainer title="告警列表">
    <el-form :model="query" label-width="70px">
      <el-row :gutter="16">
        <el-col :span="6"><el-form-item label="设备"><el-input v-model="query.deviceId" placeholder="设备ID" clearable /></el-form-item></el-col>
        <el-col :span="5"><el-form-item label="等级"><el-select v-model="query.level" clearable style="width:100%"><el-option value="WARN" label="警告" /><el-option value="CRITICAL" label="严重" /></el-select></el-form-item></el-col>
        <el-col :span="5"><el-form-item label="状态"><el-select v-model="query.status" clearable style="width:100%"><el-option value="TRIGGERED" label="已触发" /><el-option value="CONFIRMED" label="已确认" /><el-option value="RESOLVED" label="已恢复" /></el-select></el-form-item></el-col>
        <el-col :span="4"><el-form-item><el-button type="primary" @click="fetchData">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item></el-col>
      </el-row>
    </el-form>
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="deviceId" label="设备" width="120" />
      <el-table-column prop="ruleName" label="规则名称" width="180" />
      <el-table-column prop="metric" label="属性" width="100" />
      <el-table-column prop="level" label="等级" width="80"><template #default="{ row }"><el-tag :type="row.level === 'CRITICAL' ? 'danger' : 'warning'">{{ ALERT_LEVEL_MAP[row.level] }}</el-tag></template></el-table-column>
      <el-table-column prop="currentValue" label="当前值" width="100" />
      <el-table-column prop="threshold" label="阈值" width="80" />
      <el-table-column prop="triggeredAt" label="触发时间" width="170"><template #default="{ row }">{{ formatDateTime(row.triggeredAt) }}</template></el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 'TRIGGERED' ? 'danger' : row.status === 'CONFIRMED' ? 'warning' : 'success'">{{ ALERT_STATUS_MAP[row.status] }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="row.status === 'TRIGGERED'" size="small" @click="confirmAlert(row.id)">确认</el-button>
          <el-button v-if="row.status !== 'RESOLVED'" size="small" type="success" @click="resolveAlert(row.id)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="fetchData" style="margin-top: 16px; justify-content: flex-end" />
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { getAlertList, confirmAlert as confirmApi, resolveAlert as resolveApi } from '@/api/alert'
import { ALERT_LEVEL_MAP, ALERT_STATUS_MAP } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import type { AlertVO } from '@/types/alert'

const query = reactive({ deviceId: '', level: '', status: '', pageNum: 1, pageSize: 10 })
const tableData = ref<AlertVO[]>([])
const total = ref(0)
const loading = ref(false)

async function fetchData() { loading.value = true; try { const res = await getAlertList(query); tableData.value = res.records; total.value = res.total } finally { loading.value = false } }
function resetQuery() { Object.assign(query, { deviceId: '', level: '', status: '', pageNum: 1 }); fetchData() }
async function confirmAlert(id: string) { await confirmApi(id); fetchData() }
async function resolveAlert(id: string) { await resolveApi(id); fetchData() }

onMounted(fetchData)
</script>
