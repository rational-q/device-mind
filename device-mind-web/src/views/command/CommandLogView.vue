<template>
  <PageContainer title="指令日志">
    <el-form :model="query" label-width="70px">
      <el-row :gutter="16">
        <el-col :span="6"><el-form-item label="设备"><el-input v-model="query.deviceId" placeholder="设备ID" clearable /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="指令"><el-input v-model="query.command" placeholder="指令标识" clearable /></el-form-item></el-col>
        <el-col :span="5"><el-form-item label="状态"><el-select v-model="query.status" clearable style="width:100%"><el-option v-for="(v,k) in COMMAND_STATUS_MAP" :key="k" :label="v" :value="k" /></el-select></el-form-item></el-col>
        <el-col :span="4"><el-form-item><el-button type="primary" @click="fetchData">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item></el-col>
      </el-row>
    </el-form>
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="deviceId" label="设备" width="150" />
      <el-table-column prop="command" label="指令" width="180" />
      <el-table-column prop="params" label="参数" />
      <el-table-column prop="status" label="状态" width="80"><template #default="{ row }"><el-tag :type="row.status==='FAILED'?'danger':row.status==='ACKED'?'success':'info'">{{ COMMAND_STATUS_MAP[row.status] }}</el-tag></template></el-table-column>
      <el-table-column prop="retryCount" label="重试" width="60" />
      <el-table-column prop="createdDate" label="时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createdDate) }}</template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="fetchData" style="margin-top: 16px; justify-content: flex-end" />
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { getCommandLogList } from '@/api/commandLog'
import { COMMAND_STATUS_MAP } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import type { CommandLogVO } from '@/types/commandLog'

const query = reactive({ deviceId: '', command: '', status: '', pageNum: 1, pageSize: 10 })
const tableData = ref<CommandLogVO[]>([])
const total = ref(0)
const loading = ref(false)

async function fetchData() { loading.value = true; try { const res = await getCommandLogList(query); tableData.value = res.records; total.value = res.total } finally { loading.value = false } }
function resetQuery() { Object.assign(query, { deviceId: '', command: '', status: '', pageNum: 1 }); fetchData() }

onMounted(fetchData)
</script>
