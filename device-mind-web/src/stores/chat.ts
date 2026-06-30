import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  type?: 'text' | 'analysis' | 'query' | 'error'
  raw?: any
  loading?: boolean
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([
    {
      role: 'assistant',
      content: '你好！我是 DeviceMind AI 助手。我可以帮你：\n\n🔍 分析设备告警根因\n📊 用自然语言查询设备数据\n💡 提供运维建议\n\n请问有什么可以帮你的？',
      type: 'text',
    },
  ])

  function addMessage(msg: ChatMessage) {
    messages.value.push(msg)
  }

  function updateLastMessage(msg: ChatMessage) {
    messages.value[messages.value.length - 1] = msg
  }

  function clear() {
    messages.value = [
      { role: 'assistant', content: '对话已清空，有什么新的问题吗？', type: 'text' },
    ]
  }

  return { messages, addMessage, updateLastMessage, clear }
})
