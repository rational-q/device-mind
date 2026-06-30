<template>
  <PageContainer title="告警规则">
    <template #actions><el-button type="primary" @click="openDialog()">新增规则</el-button></template>
    <el-form :model="query" label-width="80px">
      <el-row :gutter="16">
        <el-col :span="6"><el-form-item label="产品类型"><el-input v-model="query.deviceType" placeholder="如 TEMP_SENSOR_V1" clearable /></el-form-item></el-col>
        <el-col :span="5"><el-form-item label="等级"><el-select v-model="query.level" clearable style="width:100%"><el-option value="WARN" label="警告" /><el-option value="CRITICAL" label="严重" /></el-select></el-form-item></el-col>
        <el-col :span="4"><el-form-item><el-button type="primary" @click="fetchData">查询</el-button></el-form-item></el-col>
      </el-row>
    </el-form>
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="ruleName" label="规则名称" width="180" />
      <el-table-column prop="deviceType" label="产品类型" width="130" />
      <el-table-column prop="attrName" label="属性" width="120" />
      <el-table-column prop="operator" label="运算符" width="80" />
      <el-table-column prop="threshold" label="阈值" width="80" />
      <el-table-column prop="durationSeconds" label="持续(秒)" width="80" />
      <el-table-column prop="level" label="等级" width="80"><template #default="{ row }"><el-tag :type="row.level === 'CRITICAL' ? 'danger' : 'warning'">{{ ALERT_LEVEL_MAP[row.level] }}</el-tag></template></el-table-column>
      <el-table-column prop="enabled" label="启用" width="70"><template #default="{ row }"><el-switch :model-value="row.enabled" @change="toggleRule(row.id)" /></template></el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row as AlertRuleVO)">编辑</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="fetchData" style="margin-top: 16px; justify-content: flex-end" />
    <el-dialog :title="form.id ? '编辑规则' : '新增规则'" v-model="dialogVisible" width="500px">
      <el-form :model="form" ref="formRef" label-width="100px" :rules="rules">
        <el-form-item label="规则名称" prop="ruleName" required><el-input v-model="form.ruleName" /></el-form-item>
        <el-form-item label="产品类型" prop="deviceType" required><el-input v-model="form.deviceType" /></el-form-item>
        <el-form-item label="属性" prop="attrName" required><el-input v-model="form.attrName" /></el-form-item>
        <el-form-item label="运算符" prop="operator" required><el-select v-model="form.operator"><el-option v-for="o in ['>','<','>=','<=','==']" :key="o" :value="o" /></el-select></el-form-item>
        <el-form-item label="阈值" prop="threshold" required><el-input-number v-model="form.threshold" /></el-form-item>
        <el-form-item label="持续(秒)"><el-input-number v-model="form.durationSeconds" :min="0" /></el-form-item>
        <el-form-item label="等级" prop="level" required><el-select v-model="form.level"><el-option value="WARN" /><el-option value="CRITICAL" /></el-select></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { getAlertRuleList, createAlertRule, updateAlertRule, deleteAlertRule, toggleAlertRule } from '@/api/alert'
import { ALERT_LEVEL_MAP } from '@/utils/constants'
import type { AlertRuleVO } from '@/types/alert'
import { ElMessage } from 'element-plus'
const query = reactive({ deviceType: '', level: '', pageNum: 1, pageSize: 10 })
const tableData = ref<AlertRuleVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive<any>({ id: null, ruleName: '', deviceType: '', attrName: '', operator: '>', threshold: 0, durationSeconds: 60, level: 'WARN', enabled: true })
const rules = { ruleName: [{ required: true, message: "请输入规则名称" }], deviceType: [{ required: true, message: "请输入产品类型" }], attrName: [{ required: true, message: "请输入属性名" }], operator: [{ required: true, message: "请选择运算符" }], threshold: [{ required: true, message: "请输入阈值" }], level: [{ required: true, message: "请选择等级" }] }
async function fetchData() { loading.value = true; try { const res = await getAlertRuleList(query); tableData.value = res.records; total.value = res.total } finally { loading.value = false } }
function openDialog(row?: AlertRuleVO) { if (row) Object.assign(form, row); else Object.assign(form, { id: null, ruleName: '', deviceType: '', attrName: '', operator: '>', threshold: 0, durationSeconds: 60, level: 'WARN', enabled: true }); dialogVisible.value = true }
async function handleSave() { if (form.id) { await updateAlertRule(form.id, form); ElMessage.success("更新成功") } else { await createAlertRule(form); ElMessage.success("创建成功") }; dialogVisible.value = false; fetchData() }
async function handleDelete(id: string) { await deleteAlertRule(id); ElMessage.success("删除成功"); fetchData() }
async function toggleRule(id: string) { await toggleAlertRule(id); ElMessage.success("操作成功"); fetchData() }

onMounted(fetchData)
</script>
