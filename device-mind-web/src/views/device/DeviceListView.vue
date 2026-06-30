<template>
  <PageContainer title="设备管理">
    <template #actions><el-button type="primary" @click="openDialog()">注册设备</el-button></template>
    <el-form :model="query" label-width="80px">
      <el-row :gutter="16">
        <el-col :span="6"><el-form-item label="设备ID"><el-input v-model="query.deviceId" placeholder="模糊搜索" clearable /></el-form-item></el-col>
        <el-col :span="5"><el-form-item label="状态"><el-select v-model="query.status" clearable style="width:100%"><el-option label="在线" value="ONLINE" /><el-option label="离线" value="OFFLINE" /></el-select></el-form-item></el-col>
        <el-col :span="4"><el-form-item><el-button type="primary" @click="fetchData">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item></el-col>
      </el-row>
    </el-form>
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="deviceId" label="设备ID" width="150" />
      <el-table-column prop="productName" label="产品" width="150" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="location" label="位置" width="120" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 'ONLINE' ? 'success' : 'info'">{{ DEVICE_STATUS_MAP[row.status] || row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="lastOnlineTime" label="最后上线" width="170"><template #default="{ row }">{{ formatDateTime(row.lastOnlineTime) }}</template></el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/device-detail?id=${row.id}`)">详情</el-button>
          <el-button size="small" @click="openDialog(row as DeviceVO)">编辑</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="fetchData" style="margin-top: 16px; justify-content: flex-end" />
    <el-dialog :title="form.id ? '编辑设备' : '注册设备'" v-model="dialogVisible" width="500px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="设备ID" prop="deviceId"><el-input v-model="form.deviceId" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="产品" prop="productId"><el-select v-model="form.productId" :disabled="!!form.id"><el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" /></el-select></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="固件版本"><el-input v-model="form.firmwareVersion" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tags" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { getDeviceList, createDevice, updateDevice, deleteDevice } from '@/api/device'
import { getProductList } from '@/api/product'
import { DEVICE_STATUS_MAP } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import type { DeviceVO } from '@/types/device'
import type { ProductVO } from '@/types/product'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
const query = reactive({ deviceId: '', status: '', pageNum: 1, pageSize: 10 })
const tableData = ref<DeviceVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const products = ref<ProductVO[]>([])
const form = reactive<any>({ id: null, deviceId: '', productId: null, name: '', location: '', firmwareVersion: '', tags: '' })
const formRef = ref<FormInstance>()
const rules = { deviceId: [{ required: true }], productId: [{ required: true }] }

async function fetchData() { loading.value = true; try { const res = await getDeviceList(query); tableData.value = res.records; total.value = res.total } finally { loading.value = false } }
function resetQuery() { Object.assign(query, { deviceId: '', status: '', pageNum: 1 }); fetchData() }
async function openDialog(row?: DeviceVO) {
  if (!products.value.length) products.value = (await getProductList({ pageSize: 100 })).records
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, deviceId: '', productId: null, name: '', location: '', firmwareVersion: '', tags: '' })
  dialogVisible.value = true
}
function resetForm() { formRef.value?.resetFields() }
async function handleSave() {
  
  if (form.id) { await updateDevice(form.id, { name: form.name, location: form.location, firmwareVersion: form.firmwareVersion, tags: form.tags }); ElMessage.success('更新成功') }
  else { await createDevice({ deviceId: form.deviceId, productId: form.productId, name: form.name, location: form.location, firmwareVersion: form.firmwareVersion, tags: form.tags }); ElMessage.success('创建成功') }
  dialogVisible.value = false; fetchData()
}
async function handleDelete(id: string) { await deleteDevice(id); ElMessage.success("删除成功"); fetchData() }

onMounted(fetchData)
</script>
