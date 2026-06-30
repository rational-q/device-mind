<template>
  <PageContainer :title="`物模型配置 - ${productName}`">
    <template #actions><el-button @click="$router.back()">返回</el-button></template>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="属性定义" name="attributes">
        <el-button type="primary" size="small" @click="openAttrDialog()" style="margin-bottom:12px">新增属性</el-button>
        <el-table :data="attributes" border stripe size="small">
          <el-table-column prop="identifier" label="标识" /><el-table-column prop="name" label="名称" />
          <el-table-column prop="dataType" label="类型" /><el-table-column prop="unit" label="单位" />
          <el-table-column prop="accessMode" label="读写" /><el-table-column prop="description" label="描述" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="openAttrDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除？" @confirm="deleteAttribute(row.id)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="服务定义" name="services">
        <el-button type="primary" size="small" @click="openSvcDialog()" style="margin-bottom:12px">新增服务</el-button>
        <el-table :data="services" border stripe size="small">
          <el-table-column prop="identifier" label="标识" /><el-table-column prop="name" label="名称" />
          <el-table-column prop="callType" label="调用类型" /><el-table-column prop="description" label="描述" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="openSvcDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除？" @confirm="deleteService(row.id)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="事件定义" name="events">
        <el-button type="primary" size="small" @click="openEvtDialog()" style="margin-bottom:12px">新增事件</el-button>
        <el-table :data="events" border stripe size="small">
          <el-table-column prop="identifier" label="标识" /><el-table-column prop="name" label="名称" />
          <el-table-column prop="type" label="类型" /><el-table-column prop="description" label="描述" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="openEvtDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除？" @confirm="deleteEvent(row.id)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 属性弹窗 -->
    <el-dialog :title="attrForm.id ? '编辑属性' : '新增属性'" v-model="attrDialog" width="500px">
      <el-form :model="attrForm" ref="attrFormRef" label-width="80px">
        <el-form-item label="标识" prop="identifier"><el-input v-model="attrForm.identifier" :disabled="!!attrForm.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="attrForm.name" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="attrForm.dataType"><el-option v-for="t in ['DOUBLE','INT','STRING','ENUM']" :key="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="单位"><el-input v-model="attrForm.unit" /></el-form-item>
        <el-form-item label="读写"><el-select v-model="attrForm.accessMode"><el-option value="R" label="只读" /><el-option value="RW" label="读写" /></el-select></el-form-item>
        <el-form-item label="描述"><el-input v-model="attrForm.description" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="attrDialog = false">取消</el-button><el-button type="primary" @click="saveAttr">保存</el-button></template>
    </el-dialog>

    <!-- 服务弹窗 -->
    <el-dialog :title="svcForm.id ? '编辑服务' : '新增服务'" v-model="svcDialog" width="500px">
      <el-form :model="svcForm" ref="svcFormRef" label-width="80px">
        <el-form-item label="标识"><el-input v-model="svcForm.identifier" :disabled="!!svcForm.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="svcForm.name" /></el-form-item>
        <el-form-item label="调用类型"><el-select v-model="svcForm.callType"><el-option value="ASYNC" /><el-option value="SYNC" /></el-select></el-form-item>
        <el-form-item label="描述"><el-input v-model="svcForm.description" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="svcDialog = false">取消</el-button><el-button type="primary" @click="saveSvc">保存</el-button></template>
    </el-dialog>

    <!-- 事件弹窗 -->
    <el-dialog :title="evtForm.id ? '编辑事件' : '新增事件'" v-model="evtDialog" width="500px">
      <el-form :model="evtForm" ref="evtFormRef" label-width="80px">
        <el-form-item label="标识"><el-input v-model="evtForm.identifier" :disabled="!!evtForm.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="evtForm.name" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="evtForm.type"><el-option v-for="t in ['INFO','ALERT','ERROR']" :key="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="描述"><el-input v-model="evtForm.description" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="evtDialog = false">取消</el-button><el-button type="primary" @click="saveEvt">保存</el-button></template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import * as api from '@/api/thingModel'
import { getProductById } from '@/api/product'
import type * as T from '@/types/thingModel'

const route = useRoute()
const productId = Number(route.query.productId)
const productName = ref('')
const activeTab = ref('attributes')
const attributes = ref<T.ThingAttributeVO[]>([])
const services = ref<T.ThingServiceVO[]>([])
const events = ref<T.ThingEventVO[]>([])

const attrDialog = ref(false), attrForm = reactive<any>({})
const svcDialog = ref(false), svcForm = reactive<any>({})
const evtDialog = ref(false), evtForm = reactive<any>({})

async function fetchAll() {
  const [p] = await Promise.all([getProductById(productId).catch(() => null)])
  if (p) productName.value = p.name
  await refreshTab()
}
async function refreshTab() {
  if (activeTab.value === 'attributes') attributes.value = await api.getAttributes(productId)
  else if (activeTab.value === 'services') services.value = await api.getServices(productId)
  else events.value = await api.getEvents(productId)
}

function openAttrDialog(row?: T.ThingAttributeVO) { Object.assign(attrForm, row ? { ...row } : { dataType: 'DOUBLE', accessMode: 'R' }); attrDialog.value = true }
async function saveAttr() { if (attrForm.id) await api.updateAttribute(productId, attrForm.id, attrForm); else await api.createAttribute(productId, attrForm); attrDialog.value = false; refreshTab() }
async function deleteAttribute(id: string) { await api.deleteAttribute(productId, id); refreshTab() }

function openSvcDialog(row?: T.ThingServiceVO) { Object.assign(svcForm, row ? { ...row } : { callType: 'ASYNC' }); svcDialog.value = true }
async function saveSvc() { if (svcForm.id) await api.updateService(productId, svcForm.id, svcForm); else await api.createService(productId, { ...svcForm, params: [] }); svcDialog.value = false; refreshTab() }
async function deleteService(id: string) { await api.deleteService(productId, id); refreshTab() }

function openEvtDialog(row?: T.ThingEventVO) { Object.assign(evtForm, row ? { ...row } : { type: 'INFO' }); evtDialog.value = true }
async function saveEvt() { if (evtForm.id) await api.updateEvent(productId, evtForm.id, evtForm); else await api.createEvent(productId, evtForm); evtDialog.value = false; refreshTab() }
async function deleteEvent(id: string) { await api.deleteEvent(productId, id); refreshTab() }

onMounted(fetchAll)
</script>
