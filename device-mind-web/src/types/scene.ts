export interface SceneVO {
  id: number
  name: string
  description: string
  productId: number
  conditions: string
  actions: string
  enabled: boolean
  createdDate: string
  updatedDate: string
}

export interface SceneCreateDTO {
  name: string
  description: string
  productId: number
  conditions: string
  actions: string
}

export interface SceneUpdateDTO {
  name?: string
  description?: string
  conditions?: string
  actions?: string
}

export interface SceneLogVO {
  id: number
  sceneId: number
  sceneName: string
  deviceId: string
  triggeredAt: string
  actionsResult: string
  status: string
  createdDate: string
}
