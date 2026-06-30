import request from './request'
import type { AlertAnalysisRequest, AlertAnalysisResponse, ChatRequest, ChatResponse } from '@/types/agent'

const AGENT_BASE = ''  // 通过 vite proxy 转发

/** 告警根因分析 */
export function analyzeAlert(data: AlertAnalysisRequest) {
  return request.post<any, AlertAnalysisResponse>(AGENT_BASE + '/device-mind/agent/analysis/alert', data)
}

/** 通用智能问答（含自然语言查数据、设备状态、告警统计、指令下发确认等） */
export function chat(data: ChatRequest) {
  return request.post<any, ChatResponse>(AGENT_BASE + '/device-mind/agent/analysis/chat', data)
}
