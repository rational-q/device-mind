import request from './request'
import type { SceneVO, SceneCreateDTO, SceneUpdateDTO, SceneLogVO } from '@/types/scene'
import type { PageResult } from '@/types/api'

export function getSceneList(pageNum: number, pageSize: number) {
  return request.post<any, PageResult<SceneVO>>('/device-mind/core/scenes/list', { pageNum, pageSize })
}

export function getSceneById(id: number) {
  return request.get<any, SceneVO>('/device-mind/core/scenes/detail', { params: { id } })
}

export function createScene(data: SceneCreateDTO) {
  return request.post<any, number>('/device-mind/core/scenes', data)
}

export function updateScene(id: number, data: SceneUpdateDTO) {
  return request.put('/device-mind/core/scenes', data, { params: { id } })
}

export function deleteScene(id: number) {
  return request.delete('/device-mind/core/scenes', { params: { id } })
}

export function toggleScene(id: number) {
  return request.put('/device-mind/core/scenes/toggle', null, { params: { id } })
}

export function getSceneLogList(sceneId: number | null, pageNum: number, pageSize: number) {
  return request.post<any, PageResult<SceneLogVO>>('/device-mind/core/scenes/log/list', { sceneId, pageNum, pageSize })
}
