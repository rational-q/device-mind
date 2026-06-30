import request from './request'
import type { AlertVO, AlertPageQueryDTO, AlertRuleVO, AlertRuleCreateDTO, AlertRuleUpdateDTO, AlertRulePageQueryDTO } from '@/types/alert'
import type { PageResult } from '@/types/api'

export function getAlertList(data: AlertPageQueryDTO) {
  return request.post<any, PageResult<AlertVO>>('/device-mind/core/alerts/list', data)
}
export function getAlertById(id: number) {
  return request.get<any, AlertVO>('/device-mind/core/alerts/detail', { params: { id } })
}
export function confirmAlert(id: number) {
  return request.put('/device-mind/core/alerts/confirm', null, { params: { id } })
}
export function resolveAlert(id: number) {
  return request.put('/device-mind/core/alerts/resolve', null, { params: { id } })
}

export function getAlertRuleList(data: AlertRulePageQueryDTO) {
  return request.post<any, PageResult<AlertRuleVO>>('/device-mind/core/alert-rules/list', data)
}
export function getAlertRuleById(id: number) {
  return request.get<any, AlertRuleVO>('/device-mind/core/alert-rules/detail', { params: { id } })
}
export function createAlertRule(data: AlertRuleCreateDTO) {
  return request.post('/device-mind/core/alert-rules', data)
}
export function updateAlertRule(id: number, data: AlertRuleUpdateDTO) {
  return request.put('/device-mind/core/alert-rules', data, { params: { id } })
}
export function deleteAlertRule(id: number) {
  return request.delete('/device-mind/core/alert-rules', { params: { id } })
}
export function toggleAlertRule(id: number) {
  return request.put('/device-mind/core/alert-rules/toggle', null, { params: { id } })
}
