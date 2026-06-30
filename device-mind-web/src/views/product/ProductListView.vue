<template>
  <PageContainer title="产品管理">
    <template #actions>
      <el-button type="primary" @click="openDialog()">新增产品</el-button>
    </template>
    <el-form :model="query" label-width="80px">
      <el-row :gutter="16">
        <el-col :span="6"><el-form-item label="产品标识"><el-input v-model="query.productKey" placeholder="模糊搜索" clearable /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="名称"><el-input v-model="query.name" placeholder="模糊搜索" clearable /></el-form-item></el-col>
        <el-col :span="4"><el-form-item><el-button type="primary" @click="fetchData">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item></el-col>
      </el-row>
    </el-form>
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="productKey" label="产品标识" width="150" />
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="protocolType" label="协议" width="80" />
      <el-table-column prop="dataFormat" label="数据格式" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdDate" label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdDate) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" @click="$router.push(`/things?productId=${row.id}`)">物模型</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
      @change="fetchData" style="margin-top: 16px; justify-content: flex-end"
    />
    <el-dialog :title="formTitle" v-model="dialogVisible" width="500px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="产品标识" prop="productKey"><el-input v-model="form.productKey" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="协议类型" prop="protocolType"><el-input v-model="form.protocolType" /></el-form-item>
        <el-form-item label="数据格式" prop="dataFormat"><el-input v-model="form.dataFormat" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item v-if="form.id" label="状态"><el-select v-model="form.status"><el-option label="ACTIVE" value="ACTIVE" /><el-option label="INACTIVE" value="INACTIVE" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { getProductList, createProduct, updateProduct, deleteProduct } from '@/api/product'
import { formatDateTime } from '@/utils/date'
import type { ProductVO } from '@/types/product'
import type { FormInstance } from 'element-plus'

const query = reactive({ productKey: '', name: '', pageNum: 1, pageSize: 10 })
const tableData = ref<ProductVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive<any>({ id: null, productKey: '', name: '', protocolType: 'MQTT', dataFormat: 'JSON', description: '', status: '' })
const formRef = ref<FormInstance>()
const rules = { productKey: [{ required: true, message: '必填' }], name: [{ required: true, message: '必填' }], protocolType: [{ required: true, message: '必填' }], dataFormat: [{ required: true, message: '必填' }] }

const formTitle = computed(() => form.id ? '编辑产品' : '新增产品')

async function fetchData() { loading.value = true; try { const res = await getProductList({ ...query }); tableData.value = res.records; total.value = res.total } finally { loading.value = false } }
function resetQuery() { Object.assign(query, { productKey: '', name: '', pageNum: 1 }); fetchData() }
function openDialog(row?: ProductVO) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, productKey: '', name: '', protocolType: 'MQTT', dataFormat: 'JSON', description: '', status: '' })
  dialogVisible.value = true
}
function resetForm() { formRef.value?.resetFields() }
async function handleSave() {
  await formRef.value?.validate()
  if (form.id) await updateProduct(form.id, { name: form.name, description: form.description, protocolType: form.protocolType, dataFormat: form.dataFormat, status: form.status })
  else await createProduct({ productKey: form.productKey, name: form.name, description: form.description, protocolType: form.protocolType, dataFormat: form.dataFormat })
  dialogVisible.value = false; fetchData()
}
async function handleDelete(id: string) { await deleteProduct(id); fetchData() }

onMounted(fetchData)
</script>
