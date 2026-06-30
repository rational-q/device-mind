<template>
  <PageContainer title="指令下发">
    <el-card shadow="never">
      <el-form :model="form" label-width="100px" style="max-width: 600px">
        <el-form-item label="目标设备" required>
          <el-input v-model="form.deviceId" placeholder="设备ID，例如 temp-001" clearable />
        </el-form-item>

        <el-form-item label="指令类型" required>
          <el-select v-model="form.command" placeholder="选择指令" style="width: 100%" filterable allow-create>
            <el-option-group label="常用指令">
              <el-option label="设置阈值 (set_threshold)" value="set_threshold" />
              <el-option label="设置上报间隔 (set_interval)" value="set_interval" />
              <el-option label="切换模式 (set_mode)" value="set_mode" />
              <el-option label="重启设备 (reboot)" value="reboot" />
              <el-option label="恢复出厂 (reset)" value="reset" />
            </el-option-group>
            <el-option-group label="执行类">
              <el-option label="启动 (start)" value="start" />
              <el-option label="停止 (stop)" value="stop" />
              <el-option label="打开 (open)" value="open" />
              <el-option label="关闭 (close)" value="close" />
              <el-option label="锁定 (lock)" value="lock" />
              <el-option label="解锁 (unlock)" value="unlock" />
            </el-option-group>
            <el-option-group label="调节类">
              <el-option label="设置功率 (set_power)" value="set_power" />
              <el-option label="设置速度 (set_speed)" value="set_speed" />
              <el-option label="设置亮度 (set_brightness)" value="set_brightness" />
              <el-option label="设置温度 (set_temperature)" value="set_temperature" />
              <el-option label="校准 (calibrate)" value="calibrate" />
            </el-option-group>
          </el-select>
        </el-form-item>

        <el-form-item label="指令参数">
          <el-input
            v-model="form.paramsText"
            type="textarea"
            :rows="3"
            placeholder='JSON 格式，例如 {"temperature": 30}'
          />
          <div v-if="paramsError" style="color: #e64242; font-size: 12px; margin-top: 4px">{{ paramsError }}</div>
        </el-form-item>

        <el-form-item label="幂等键">
          <el-input v-model="form.idempotencyKey" placeholder="不填则自动生成" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="sending" @click="doSend">
            <el-icon style="margin-right: 4px"><Promotion /></el-icon>下发指令
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 发送结果 -->
    <el-card v-if="lastResult" shadow="never" style="margin-top: 16px">
      <template #header>
        <span>
          <el-tag :type="lastResult.idempotencyKey ? 'success' : 'danger'" size="small">
            {{ lastResult.idempotencyKey ? '已发送' : '发送失败' }}
          </el-tag>
          <span style="margin-left: 8px">下发结果</span>
        </span>
      </template>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="幂等键">{{ lastResult.idempotencyKey || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目标设备">{{ lastResult.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="指令">{{ lastResult.command }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ lastResult.message }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 快捷模板 -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>快捷模板</template>
      <div style="display: flex; gap: 8px; flex-wrap: wrap">
        <el-button
          v-for="tpl in templates"
          :key="tpl.label"
          size="small"
          @click="applyTemplate(tpl)"
        >{{ tpl.label }}</el-button>
      </div>
    </el-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { Promotion } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { sendCommand } from '@/api/command'
import type { CommandSendResponse } from '@/types/command'
import { ElMessage } from 'element-plus'
interface Template {
  label: string
  deviceId: string
  command: string
  paramsText: string
}

const templates: Template[] = [
  { label: '温度阈值 30°C', deviceId: 'temp-001', command: 'set_threshold', paramsText: '{"temperature": 30}' },
  { label: '上报间隔 60s', deviceId: '', command: 'set_interval', paramsText: '{"seconds": 60}' },
  { label: '重启设备', deviceId: '', command: 'reboot', paramsText: '{}' },
  { label: '切换节能模式', deviceId: '', command: 'set_mode', paramsText: '{"mode": "eco"}' },
  { label: '校准传感器', deviceId: '', command: 'calibrate', paramsText: '{"sensor": "temperature", "offset": 0.5}' },
]

const form = reactive({
  deviceId: '',
  command: '',
  paramsText: '',
  idempotencyKey: '',
})

const sending = ref(false)
const paramsError = ref('')
const lastResult = ref<CommandSendResponse | null>(null)

function applyTemplate(tpl: Template) {
  form.deviceId = tpl.deviceId || form.deviceId
  form.command = tpl.command
  form.paramsText = tpl.paramsText
  paramsError.value = ''
}

function resetForm() {
  form.deviceId = ''
  form.command = ''
  form.paramsText = ''
  form.idempotencyKey = ''
  paramsError.value = ''
}

async function doSend() {
  if (!form.deviceId.trim()) { ElMessage.warning('请输入设备ID'); return }
  if (!form.command.trim()) { ElMessage.warning('请选择指令类型'); return }

  let params: Record<string, any> | undefined
  if (form.paramsText.trim()) {
    try {
      params = JSON.parse(form.paramsText)
      paramsError.value = ''
    } catch {
      paramsError.value = '参数 JSON 格式错误'
      return
    }
  }

  sending.value = true
  try {
    const res = await sendCommand({
      deviceId: form.deviceId.trim(),
      command: form.command.trim(),
      params,
      idempotencyKey: form.idempotencyKey.trim() || undefined,
    })
    lastResult.value = res
    ElMessage.success(res.message || '指令已发送')
  } catch (e: any) {
    lastResult.value = { idempotencyKey: '', deviceId: form.deviceId, command: form.command, message: e.message || '发送失败' }
  } finally {
    sending.value = false
  }
}
</script>
