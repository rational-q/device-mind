import request from './request'
import type { AlertVO, AlertPageQueryDTO, AlertRuleVO, AlertRuleCreateDTO, AlertRuleUpdateDTO, AlertRulePageQueryDTO } from '@/types/alert'
import type { PageResult } from '@/types/api'

export function getAlertList(data: AlertPageQueryDTO) {
  return request.post<any, PageResult<AlertVO>>('/device-mind/alerts/list', data)
}
export function getAlertById(id: number) {
  return request.get<any, AlertVO>('/device-mind/alerts/detail', { params: { id } })
}
export function confirmAlert(id: number) {
  return request.put('/device-mind/alerts/confirm', null, { params: { id } })
}
export function resolveAlert(id: number) {
  return request.put('/device-mind/alerts/resolve', null, { params: { id } })
}

export function getAlertRuleList(data: AlertRulePageQueryDTO) {
  return request.post<any, PageResult<AlertRuleVO>>('/device-mind/alert-rules/list', data)
}
export function getAlertRuleById(id: number) {
  return request.get<any, AlertRuleVO>('/device-mind/alert-rules/detail', { params: { id } })
}
export function createAlertRule(data: AlertRuleCreateDTO) {
  return request.post('/device-mind/alert-rules', data)
}
export function updateAlertRule(id: number, data: AlertRuleUpdateDTO) {
  return request.put('/device-mind/alert-rules', data, { params: { id } })
}
export function deleteAlertRule(id: number) {
  return request.delete('/device-mind/alert-rules', { params: { id } })
}
export function toggleAlertRule(id: number) {
  return request.put('/device-mind/alert-rules/toggle', null, { params: { id } })
}
