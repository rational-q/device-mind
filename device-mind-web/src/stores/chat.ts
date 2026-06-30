import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const STORAGE_KEY = 'dm_chat_messages'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  type?: 'text' | 'analysis' | 'query' | 'error'
  raw?: any
  loading?: boolean
}

function loadFromStorage(): ChatMessage[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const msgs = JSON.parse(raw) as ChatMessage[]
      if (msgs.length > 0) return msgs.map(m => ({ ...m, loading: false }))
    }
  } catch { /* ignore */ }
  return [{ role: 'assistant', content: '你好！我是 DeviceMind AI 助手。我可以帮你：\n\n🔍 分析设备告警根因\n📊 用自然语言查询设备数据\n💡 提供运维建议\n\n请问有什么可以帮你的？', type: 'text' }]
}

function saveToStorage(msgs: ChatMessage[]) {
  // 只存已完成的（不含 loading 状态），最多存 100 条
  const toSave = msgs.filter(m => !m.loading).slice(-100)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(toSave))
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>(loadFromStorage())

  // 自动保存
  watch(messages, (val) => saveToStorage(val), { deep: true })

  function addMessage(msg: ChatMessage) {
    messages.value.push(msg)
  }

  function updateLastMessage(msg: ChatMessage) {
    messages.value[messages.value.length - 1] = msg
  }

  function clear() {
    messages.value = [{ role: 'assistant', content: '对话已清空，有什么新的问题吗？', type: 'text' }]
  }

  return { messages, addMessage, updateLastMessage, clear }
})
