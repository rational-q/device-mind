import request from './request'
import type { DeviceVO, DeviceCreateDTO, DeviceUpdateDTO, DevicePageQueryDTO, DeviceStatusUpdateDTO } from '@/types/device'
import type { PageResult } from '@/types/api'

export function getDeviceList(data: DevicePageQueryDTO) {
  return request.post<any, PageResult<DeviceVO>>('/device-mind/core/devices/list', data)
}

export function getDeviceById(id: string) {
  return request.get<any, DeviceVO>('/device-mind/core/devices/detail', { params: { id } })
}

export function createDevice(data: DeviceCreateDTO) {
  return request.post('/device-mind/core/devices', data)
}

export function updateDevice(id: string, data: DeviceUpdateDTO) {
  return request.put('/device-mind/core/devices', data, { params: { id } })
}

export function deleteDevice(id: string) {
  return request.delete('/device-mind/core/devices', { params: { id } })
}

export function updateDeviceStatus(id: string, data: DeviceStatusUpdateDTO) {
  return request.put('/device-mind/core/devices/status', data, { params: { id } })
}
