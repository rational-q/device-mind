<template>
  <PageContainer title="场景联动">
    <el-row style="margin-bottom: 16px">
      <el-button type="primary" @click="openCreate">新建场景</el-button>
    </el-row>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="name" label="场景名称" width="160" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="conditions" label="触发条件" width="200" show-overflow-tooltip />
      <el-table-column prop="actions" label="执行动作" width="200" show-overflow-tooltip />
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdDate" label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdDate) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.enabled ? 'warning' : 'success'" @click="handleToggle(row.id)">{{ row.enabled ? '禁用' : '启用' }}</el-button>
          <el-button size="small" @click="viewLogs(row)">日志</el-button>
          <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="fetchData" style="margin-top: 16px; justify-content: flex-end" />

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑场景' : '新建场景'" width="700px">
      <el-form :model="form" label-width="80px" ref="formRef" :rules="rules">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="场景名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="场景描述" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="产品" prop="productId">
          <el-select v-model="form.productId" placeholder="选择产品" style="width:100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>        <el-form-item label="触发条件" prop="conditions">
          <el-input v-model="form.conditions" type="textarea" :rows="4"
            placeholder='[{"attr":"temperature","operator":">","value":40,"duration":30}]' />
          <div style="color:#909399;font-size:12px;margin-top:4px">条件JSON: attr=属性名, operator=运算符(&gt; &lt; &gt;= &lt;= ==), value=阈值, duration=持续秒数</div>
        </el-form-item>
        <el-form-item label="执行动作" prop="actions">
          <el-input v-model="form.actions" type="textarea" :rows="5"
            placeholder='[{"type":"COMMAND","targetDeviceId":"FAN-01","command":"turnOn","params":{}},{"type":"DELAY","seconds":10},{"type":"SMS","phoneNumbers":["13800138000"],"content":"告警通知"}]' />
          <div style="color:#909399;font-size:12px;margin-top:4px">
            动作类型: COMMAND(指令下发), DELAY(延迟), SMS(短信通知)
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 执行日志弹窗 -->
    <el-dialog v-model="logVisible" :title="'执行日志 - ' + currentSceneName" width="800px">
      <el-table :data="logData" border stripe v-loading="logLoading">
        <el-table-column prop="deviceId" label="设备" width="120" />
        <el-table-column prop="triggeredAt" label="触发时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.triggeredAt) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'PARTIAL' ? 'warning' : 'danger'">{{ SCENE_STATUS_MAP[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="actionsResult" label="执行结果" min-width="300" show-overflow-tooltip />
      </el-table>
      <el-pagination v-model:current-page="logQuery.pageNum" v-model:page-size="logQuery.pageSize" :total="logTotal" layout="total, prev, pager, next" @change="fetchLogs" style="margin-top: 12px" />
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/common/PageContainer.vue'
import { getSceneList, createScene, updateScene, deleteScene, toggleScene, getSceneLogList } from '@/api/scene'
import { getProductList } from '@/api/product'
import { formatDateTime } from '@/utils/date'
import { SCENE_STATUS_MAP } from '@/utils/constants'
import type { SceneVO, SceneLogVO } from '@/types/scene'

const query = reactive({ pageNum: 1, pageSize: 10 })
const tableData = ref<SceneVO[]>([])
const total = ref(0)
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try { const res = await getSceneList(query.pageNum, query.pageSize); tableData.value = res.records; total.value = res.total }
  finally { loading.value = false }
}

// 新增/编辑
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(0)
const saving = ref(false)
const formRef = ref()
const form = reactive({ name: '', description: '', productId: null as number | null, conditions: '', actions: '' })
const rules = { name: [{ required: true, message: '请输入场景名称' }], conditions: [{ required: true, message: '请填写触发条件' }], actions: [{ required: true, message: '请填写执行动作' }] }

function openCreate() {
  isEdit.value = false; editId.value = 0
  form.name = ''; form.description = ''; form.conditions = ''; form.actions = ''
  dialogVisible.value = true
}

function openEdit(row: SceneVO) {
  isEdit.value = true; editId.value = row.id
  form.name = row.name; form.description = row.description || ''
  form.conditions = row.conditions; form.actions = row.actions
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) { await updateScene(editId.value, form) } else { await createScene(form) }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false; fetchData()
  } finally { saving.value = false }
}

async function handleToggle(id: number) { await toggleScene(id); fetchData() }
async function handleDelete(id: string) { await deleteScene(id); fetchData() }

// 日志弹窗
const logVisible = ref(false)
const currentSceneName = ref('')
const logData = ref<SceneLogVO[]>([])
const logTotal = ref(0)
const logLoading = ref(false)
const logQuery = reactive({ sceneId: null as number | null, pageNum: 1, pageSize: 10 })
const productOptions = ref<{ id: number; name: string }[]>([])
onMounted(async () => { const res = await getProductList({ pageSize: 100 }); productOptions.value = res.records })
async function viewLogs(row: SceneVO) {
  currentSceneName.value = row.name
  logQuery.sceneId = row.id; logQuery.pageNum = 1
  logVisible.value = true; await fetchLogs()
}

async function fetchLogs() {
  logLoading.value = true
  try { const res = await getSceneLogList(logQuery.sceneId, logQuery.pageNum, logQuery.pageSize); logData.value = res.records; logTotal.value = res.total }
  finally { logLoading.value = false }
}

onMounted(fetchData)
</script>
