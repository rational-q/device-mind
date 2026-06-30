<template>
  <div class="agent-container">
    <!-- 左侧: 快捷操作区 -->
    <div class="agent-sidebar">
      <div class="sidebar-title">快捷操作</div>
      <el-button type="primary" style="width:100%;margin-bottom:8px" @click="openAlertDialog">
        <el-icon style="margin-right:4px"><Bell /></el-icon>分析告警
      </el-button>
      <el-select v-model="quickExample" style="width:100%;margin-bottom:8px" placeholder="快速查询示例" @change="sendQuickQuery">
        <el-option value="最近1小时温度最高的设备" label="🌡️ 最近1小时温度最高的设备" />
        <el-option value="A-102设备最近的温度数据" label="📊 A-102设备最近的温度数据" />
        <el-option value="今天触发了哪些告警" label="🔔 今天触发了哪些告警" />
        <el-option value="当前在线的设备有哪些" label="✅ 当前在线的设备有哪些" />
        <el-option value="最近24小时温度超过35度的次数" label="📈 最近24小时温度超过35度的次数" />
      </el-select>
      <el-button style="width:100%" @click="clearChat">清空对话</el-button>
    </div>

    <!-- 右侧: 聊天区域 -->
    <div class="agent-main">
      <div class="chat-messages" ref="messagesRef">
        <div v-for="(msg, i) in messages" :key="i" class="message-row" :class="msg.role">
          <div class="avatar">{{ msg.role === 'user' ? '🧑' : '🤖' }}</div>
          <div class="bubble">
            <div class="bubble-content" v-html="renderMessage(msg)"></div>
            <div v-if="msg.loading" class="loading-dots"><span></span><span></span><span></span></div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="输入自然语言查询，例如：查询A-102设备的温度数据..."
          @keydown.enter.prevent="sendMessage"
          :disabled="sending"
        />
        <div style="display:flex;flex-direction:column;gap:4px;margin-left:8px;align-self:flex-end">
          <el-button type="primary" @click="sendMessage" :loading="sending">
            发送
          </el-button>
        </div>
      </div>
    </div>

    <!-- 告警分析弹窗 -->
    <el-dialog v-model="alertDialogVisible" title="告警分析" width="500px">
      <el-form :model="alertForm" label-width="80px">
        <el-form-item label="设备ID" required>
          <el-input v-model="alertForm.deviceId" placeholder="如 A-102" />
        </el-form-item>
        <el-form-item label="产品类型">
          <el-input v-model="alertForm.productKey" placeholder="如 TEMP_SENSOR_V1（可选）" />
        </el-form-item>
        <el-form-item label="规则名称">
          <el-input v-model="alertForm.ruleName" placeholder="如 温度过高（可选）" />
        </el-form-item>
        <el-form-item label="告警等级">
          <el-select v-model="alertForm.level" style="width:100%">
            <el-option value="" label="请选择" />
            <el-option value="WARN" label="警告" />
            <el-option value="CRITICAL" label="严重" />
          </el-select>
        </el-form-item>
        <el-form-item label="监控属性">
          <el-input v-model="alertForm.metric" placeholder="如 temperature（可选）" />
        </el-form-item>
        <el-form-item label="当前值">
          <el-input-number v-model="alertForm.currentValue" :min="0" :precision="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="alertForm.threshold" :min="0" :precision="1" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alertDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendAlertAnalysis" :loading="sending">开始分析</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue'
import { analyzeAlert, chat } from '@/api/agent'
import type { AlertAnalysisResponse, ChatResponse } from '@/types/agent'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  type?: 'text' | 'analysis' | 'query' | 'error'
  raw?: any
  loading?: boolean
}

const messages = ref<ChatMessage[]>([
  { role: 'assistant', content: '你好！我是 DeviceMind AI 助手。我可以帮你：\n\n🔍 分析设备告警根因\n📊 用自然语言查询设备数据\n💡 提供运维建议\n\n请问有什么可以帮你的？', type: 'text' },
])
const inputText = ref('')
const sending = ref(false)
const messagesRef = ref<HTMLElement>()

// 告警分析弹窗
const alertDialogVisible = ref(false)
const alertForm = reactive({
  deviceId: '', productKey: '', ruleName: '', level: '',
  metric: '', currentValue: 0, threshold: 0,
})
const quickExample = ref('')

function openAlertDialog() {
  alertForm.deviceId = ''; alertForm.productKey = ''; alertForm.ruleName = ''
  alertForm.level = ''; alertForm.metric = ''; alertForm.currentValue = 0; alertForm.threshold = 0
  alertDialogVisible.value = true
}

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function renderMessage(msg: ChatMessage): string {
  if (msg.type === 'analysis' && msg.raw) {
    const r = msg.raw as AlertAnalysisResponse
    let html = `<div class="analysis-result"><div class="analysis-summary"><strong>📋 分析结论：</strong>${escHtml(r.summary)}</div>`
    if (r.severity) html += `<div><strong>⚠️ 严重程度：</strong>${escHtml(r.severity)}</div>`
    if (r.possibleCauses?.length) {
      html += `<div><strong>🔍 可能原因：</strong></div><ul>${r.possibleCauses.map((c: string) => `<li>${escHtml(c)}</li>`).join('')}</ul>`
    }
    if (r.recommendations?.length) {
      html += `<div><strong>💡 处理建议：</strong></div><ul>${r.recommendations.map((c: string) => `<li>${escHtml(c)}</li>`).join('')}</ul>`
    }
    if (r.errorMsg) html += `<div style="color:#e64242">❌ ${escHtml(r.errorMsg)}</div>`
    return html + '</div>'
  }
  if (msg.type === 'query' && msg.raw) {
    const r = msg.raw as ChatResponse
    let html = escHtml(r.answer || msg.content)
    if (r.toolsCalled?.length) {
      html += `<div style="margin-top:6px;font-size:11px;color:#909399">🔧 调用了: ${r.toolsCalled.join(', ')}</div>`
    }
    if (r.pendingAction) {
      html += `<div style="margin-top:8px;padding:8px;background:#fef0f0;border-left:3px solid #e64242;border-radius:4px">`
      html += `<strong>⚠️ 待确认操作</strong><br>`
      html += escHtml(r.pendingAction.message || '请确认是否执行此操作')
      html += `</div>`
    }
    if (r.errorMsg) html += `<div style="color:#e64242">❌ ${escHtml(r.errorMsg)}</div>`
    return html + '</div>'
  }
  return escHtml(msg.content).replace(/\n/g, '<br>')
}

function escHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

// 发送消息
async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || sending.value) return
  inputText.value = ''
  quickExample.value = ''

  messages.value.push({ role: 'user', content: text })
  scrollToBottom()

  await doQuery(text)
}

function sendQuickQuery(val: string) {
  if (!val) return
  quickExample.value = ''
  inputText.value = val
  sendMessage()
}

async function doQuery(text: string) {
  const msgIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', loading: true, type: 'query' })
  sending.value = true
  scrollToBottom()

  try {
    const res = await chat({ question: text })
    const answer = res.answer || (res.success ? '查询完成' : res.errorMsg || 'AI 未返回结果')
    messages.value[msgIdx] = {
      role: 'assistant',
      content: answer,
      type: 'query',
      raw: res,
    }
  } catch (e: any) {
    messages.value[msgIdx] = { role: 'assistant', content: '请求失败: ' + (e.message || '网络错误'), type: 'error' }
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

// 告警分析
async function sendAlertAnalysis() {
  if (!alertForm.deviceId) { ElMessage.warning('请输入设备ID'); return }
  alertDialogVisible.value = false

  messages.value.push({ role: 'user', content: `分析告警: 设备 ${alertForm.deviceId}${alertForm.ruleName ? ' - ' + alertForm.ruleName : ''}` })
  scrollToBottom()

  const msgIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', loading: true, type: 'analysis' })
  sending.value = true
  scrollToBottom()

  try {
    const res = await analyzeAlert({
      deviceId: alertForm.deviceId,
      productKey: alertForm.productKey || undefined,
      ruleName: alertForm.ruleName || undefined,
      level: alertForm.level || undefined,
      metric: alertForm.metric || undefined,
      currentValue: alertForm.currentValue != null ? alertForm.currentValue : undefined,
      threshold: alertForm.threshold || undefined,
    })
    messages.value[msgIdx] = {
      role: 'assistant',
      content: res.summary || '分析完成',
      type: 'analysis',
      raw: res,
    }
  } catch (e: any) {
    messages.value[msgIdx] = { role: 'assistant', content: '分析请求失败: ' + (e.message || '网络错误'), type: 'error' }
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = [{ role: 'assistant', content: '对话已清空，有什么新的问题吗？', type: 'text' }]
}
</script>

<style scoped>
.agent-container { display: flex; height: calc(100vh - 100px); gap: 16px; }
.agent-sidebar { width: 260px; flex-shrink: 0; }
.sidebar-title { font-size: 16px; font-weight: bold; margin-bottom: 12px; }
.agent-main { flex: 1; display: flex; flex-direction: column; background: #fff; border-radius: 8px; border: 1px solid #dcdfe6; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; }
.message-row { display: flex; margin-bottom: 16px; gap: 10px; }
.message-row.user { flex-direction: row-reverse; }
.avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.bubble { max-width: 70%; padding: 12px 16px; border-radius: 12px; line-height: 1.6; font-size: 14px; }
.user .bubble { background: #409EFF; color: #fff; border-bottom-right-radius: 4px; }
.assistant .bubble { background: #f0f2f5; color: #333; border-bottom-left-radius: 4px; }
.chat-input { display: flex; padding: 16px; border-top: 1px solid #dcdfe6; background: #fafafa; border-radius: 0 0 8px 8px; }
.loading-dots { display: flex; gap: 4px; padding: 4px 0; }
.loading-dots span { width: 8px; height: 8px; background: #909399; border-radius: 50%; animation: bounce 1s infinite; }
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
</style>
