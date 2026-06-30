import request from './request'
import type * as T from '@/types/thingModel'

const BASE = '/device-mind/core/things'

export function getAttributes(productId: string) {
  return request.get<any, T.ThingAttributeVO[]>(`${BASE}/attributes`, { params: { productId } })
}
export function createAttribute(productId: string, data: T.ThingAttributeCreateDTO) {
  return request.post(`${BASE}/attributes`, data, { params: { productId } })
}
export function updateAttribute(productId: string, id: number, data: T.ThingAttributeUpdateDTO) {
  return request.put(`${BASE}/attributes`, data, { params: { productId, id } })
}
export function deleteAttribute(productId: string, id: number) {
  return request.delete(`${BASE}/attributes`, { params: { productId, id } })
}

export function getServices(productId: string) {
  return request.get<any, T.ThingServiceVO[]>(`${BASE}/services`, { params: { productId } })
}
export function createService(productId: string, data: T.ThingServiceCreateDTO) {
  return request.post(`${BASE}/services`, data, { params: { productId } })
}
export function updateService(productId: string, id: number, data: T.ThingServiceUpdateDTO) {
  return request.put(`${BASE}/services`, data, { params: { productId, id } })
}
export function deleteService(productId: string, id: number) {
  return request.delete(`${BASE}/services`, { params: { productId, id } })
}

export function getEvents(productId: string) {
  return request.get<any, T.ThingEventVO[]>(`${BASE}/events`, { params: { productId } })
}
export function createEvent(productId: string, data: T.ThingEventCreateDTO) {
  return request.post(`${BASE}/events`, data, { params: { productId } })
}
export function updateEvent(productId: string, id: number, data: T.ThingEventUpdateDTO) {
  return request.put(`${BASE}/events`, data, { params: { productId, id } })
}
export function deleteEvent(productId: string, id: number) {
  return request.delete(`${BASE}/events`, { params: { productId, id } })
}
