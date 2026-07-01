<template>
  <div class="agent-container">
    <div class="agent-sidebar">
      <div class="sidebar-title">快捷操作</div>
      <el-button type="primary" style="width:100%;margin-bottom:8px" @click="openAlertDialog">
        <el-icon style="margin-right:4px"><Bell /></el-icon>分析告警
      </el-button>
      <el-select v-model="quickExample" style="width:100%;margin-bottom:8px" placeholder="快速查询示例" @change="sendQuickQuery">
        <el-option value="最近1小时温度最高的设备" label="🌡️ 最近1小时温度最高的设备" />
        <el-option value="最近有哪些告警" label="🔔 最近有哪些告警" />
        <el-option value="当前在线的设备有哪些" label="✅ 当前在线的设备有哪些" />
        <el-option value="指令下发成功率是多少" label="📊 指令下发成功率是多少" />
      </el-select>
      <el-button style="width:100%" @click="clearChat">清空对话</el-button>
    </div>

    <div class="agent-main">
      <div class="chat-messages" ref="messagesRef">
        <div v-for="(msg, i) in store.messages" :key="i" class="message-row" :class="msg.role">
          <div class="avatar">{{ msg.role === 'user' ? '🧑' : '🤖' }}</div>
          <div class="bubble">
            <div class="bubble-content" v-html="renderMessage(msg)"></div>
            <div v-if="msg.loading" class="loading-dots"><span></span><span></span><span></span></div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input v-model="inputText" type="textarea" :rows="2" placeholder="输入问题，例如：现在有多少设备在线？" @keydown.enter.prevent="sendMessage" :disabled="sending" />
        <el-button type="primary" @click="sendMessage" :loading="sending" style="margin-left:8px;align-self:flex-end">发送</el-button>
      </div>
    </div>

    <!-- 告警分析弹窗 -->
    <el-dialog v-model="alertDialogVisible" title="告警分析" width="500px">
      <el-form :model="alertForm" label-width="80px">
        <el-form-item label="设备ID" required><el-input v-model="alertForm.deviceId" placeholder="如 A-102" /></el-form-item>
        <el-form-item label="告警等级">
          <el-select v-model="alertForm.level" style="width:100%"><el-option value="WARN" label="警告" /><el-option value="CRITICAL" label="严重" /></el-select>
        </el-form-item>
        <el-form-item label="监控属性"><el-input v-model="alertForm.metric" placeholder="如 temperature" /></el-form-item>
        <el-form-item label="当前值"><el-input-number v-model="alertForm.currentValue" :min="0" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="阈值"><el-input-number v-model="alertForm.threshold" :min="0" :precision="1" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alertDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendAlertAnalysis" :loading="sending">开始分析</el-button>
      </template>
    </el-dialog>

    <!-- 指令确认弹窗 -->
    <el-dialog v-model="confirmVisible" title="⚠️ 指令下发确认" width="450px">
      <div style="margin-bottom:16px">
        <p><strong>设备：</strong>{{ pendingCmd.deviceId }}</p>
        <p><strong>指令：</strong>{{ pendingCmd.command }}</p>
        <p v-if="pendingCmd.params"><strong>参数：</strong>{{ JSON.stringify(pendingCmd.params) }}</p>
        <p style="color:#e64242;font-size:13px">{{ pendingCmd.message }}</p>
      </div>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="primary" @click="doSendCommand" :loading="cmdSending">确认下发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { analyzeAlert, chat } from '@/api/agent'
import { sendCommand } from '@/api/command'
import { useChatStore } from '@/stores/chat'
import type { AlertAnalysisResponse, ChatResponse } from '@/types/agent'

const store = useChatStore()

const inputText = ref('')
const sending = ref(false)
const messagesRef = ref<HTMLElement>()
const quickExample = ref('')
const SESSION_KEY = 'dm_chat_session'
const sessionId = ref<string | null>(localStorage.getItem(SESSION_KEY) || null)  // 持久化，刷新不丢

// 告警分析
const alertDialogVisible = ref(false)
const alertForm = reactive({ deviceId: '', level: '', metric: '', currentValue: 0, threshold: 0 })

// 指令确认
const confirmVisible = ref(false)
const cmdSending = ref(false)
const pendingCmd = reactive({ deviceId: '', command: '', params: null as any, message: '' })

function clearChat() { store.clear(); localStorage.removeItem(SESSION_KEY); sessionId.value = null }

function openAlertDialog() {
  alertForm.deviceId = ''; alertForm.level = ''; alertForm.metric = ''; alertForm.currentValue = 0; alertForm.threshold = 0
  alertDialogVisible.value = true
}

function scrollToBottom() {
  nextTick(() => { const el = messagesRef.value; if (el) el.scrollTop = el.scrollHeight })
}

function renderMessage(msg: any): string {
  if (msg.type === 'analysis' && msg.raw) {
    const r = msg.raw as AlertAnalysisResponse
    let html = `<div class="analysis-result"><div class="analysis-summary"><strong>📋 分析结论：</strong>${escHtml(r.summary)}</div>`
    if (r.severity) html += `<div><strong>⚠️ 严重程度：</strong>${escHtml(r.severity)}</div>`
    if (r.possibleCauses?.length) html += `<div><strong>🔍 可能原因：</strong></div><ul>${r.possibleCauses.map((c: string) => `<li>${escHtml(c)}</li>`).join('')}</ul>`
    if (r.recommendations?.length) html += `<div><strong>💡 处理建议：</strong></div><ul>${r.recommendations.map((c: string) => `<li>${escHtml(c)}</li>`).join('')}</ul>`
    if (r.errorMsg) html += `<div style="color:#e64242">❌ ${escHtml(r.errorMsg)}</div>`
    return html + '</div>'
  }
  if (msg.type === 'query' && msg.raw) {
    const r = msg.raw as ChatResponse
    let html = escHtml(r.answer || msg.content)
    if (r.toolsCalled?.length) html += `<div style="margin-top:6px;font-size:11px;color:#909399">🔧 ${r.toolsCalled.join(', ')}</div>`
    if (r.pendingAction) {
      html += `<div style="margin-top:8px;padding:8px;background:#fef0f0;border-left:3px solid #e64242;border-radius:4px"><strong>⚠️ 待确认操作</strong><br>${escHtml(r.pendingAction.message || '')}</div>`
    }
    if (r.errorMsg) html += `<div style="color:#e64242">❌ ${escHtml(r.errorMsg)}</div>`
    return html + '</div>'
  }
  return escHtml(msg.content).replace(/\n/g, '<br>')
}

function escHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || sending.value) return
  inputText.value = ''
  quickExample.value = ''
  store.addMessage({ role: 'user', content: text })
  scrollToBottom()
  await doQuery(text)
}

function sendQuickQuery(val: string) { if (val) { quickExample.value = ''; inputText.value = val; sendMessage() } }

async function doQuery(text: string) {
  store.addMessage({ role: 'assistant', content: '', loading: true, type: 'query' })
  sending.value = true; scrollToBottom()

  try {
    const res = await chat({ question: text, sessionId: sessionId.value ?? undefined })
    if (res.sessionId) { sessionId.value = res.sessionId; localStorage.setItem(SESSION_KEY, res.sessionId) }
    store.updateLastMessage({ role: 'assistant', content: res.answer || '查询完成', type: 'query', raw: res })
    // 检测待确认操作 → 弹窗
    if (res.pendingAction?.status === 'pending_confirmation') {
      pendingCmd.deviceId = res.pendingAction.deviceId || ''
      pendingCmd.command = res.pendingAction.command || ''
      pendingCmd.params = res.pendingAction.params || null
      pendingCmd.message = res.pendingAction.message || ''
      confirmVisible.value = true
    }
  } catch (e: any) {
    store.updateLastMessage({ role: 'assistant', content: '请求失败: ' + (e.message || '网络错误'), type: 'error' })
  } finally { sending.value = false; scrollToBottom() }
}

async function doSendCommand() {
  cmdSending.value = true
  try {
    await sendCommand({ deviceId: pendingCmd.deviceId, command: pendingCmd.command, params: pendingCmd.params })
    ElMessage.success('指令已下发')
    confirmVisible.value = false
    store.addMessage({ role: 'assistant', content: `✅ 指令已下发: ${pendingCmd.deviceId} → ${pendingCmd.command}`, type: 'text' })
  } catch (e: any) {
    ElMessage.error('下发失败: ' + (e.message || '网络错误'))
  } finally { cmdSending.value = false }
}

async function sendAlertAnalysis() {
  if (!alertForm.deviceId) { ElMessage.warning('请输入设备ID'); return }
  alertDialogVisible.value = false
  store.addMessage({ role: 'user', content: `分析告警: 设备 ${alertForm.deviceId}` })
  scrollToBottom()
  store.addMessage({ role: 'assistant', content: '', loading: true, type: 'analysis' })
  sending.value = true; scrollToBottom()
  try {
    const res = await analyzeAlert({ deviceId: alertForm.deviceId, level: alertForm.level || undefined, metric: alertForm.metric || undefined, currentValue: alertForm.currentValue || undefined, threshold: alertForm.threshold || undefined })
    store.updateLastMessage({ role: 'assistant', content: res.summary || '分析完成', type: 'analysis', raw: res })
  } catch (e: any) {
    store.updateLastMessage({ role: 'assistant', content: '分析请求失败: ' + (e.message || '网络错误'), type: 'error' })
  } finally { sending.value = false; scrollToBottom() }
}
</script>

<style scoped>
.agent-container { display: flex; height: calc(100vh - 100px); gap: 16px; }
.agent-sidebar { width: 220px; flex-shrink: 0; }
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
