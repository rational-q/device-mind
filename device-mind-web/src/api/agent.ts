import request from './request'
import type { AlertAnalysisRequest, AlertAnalysisResponse, Nl2SqlRequest, Nl2SqlResponse } from '@/types/agent'

const AGENT_BASE = ''  // 通过 vite proxy 转发

export function analyzeAlert(data: AlertAnalysisRequest) {
  return request.post<any, AlertAnalysisResponse>(AGENT_BASE + '/api/v1/analysis/alert', data)
}

export function queryByNL(data: Nl2SqlRequest) {
  return request.post<any, Nl2SqlResponse>(AGENT_BASE + '/api/v1/analysis/query', data)
}
