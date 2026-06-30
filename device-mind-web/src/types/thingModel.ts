export interface ThingAttributeVO {
  id: string
  productId: number
  identifier: string
  name: string
  dataType: string
  unit: string
  accessMode: string
  description: string
}

export interface ThingAttributeCreateDTO {
  identifier: string
  name: string
  dataType: string
  unit?: string
  accessMode: string
  description?: string
}

export interface ThingAttributeUpdateDTO {
  name?: string
  dataType?: string
  unit?: string
  accessMode?: string
  description?: string
}

export interface ThingServiceVO {
  id: string
  productId: number
  identifier: string
  name: string
  callType: string
  description: string
  params: ThingServiceParamVO[]
}

export interface ThingServiceParamVO {
  id: string
  serviceId: number
  identifier: string
  name: string
  dataType: string
  required: boolean
  unit: string
  description: string
}

export interface ThingServiceCreateDTO {
  identifier: string
  name: string
  callType: string
  description?: string
  params: ThingServiceParamCreateDTO[]
}

export interface ThingServiceParamCreateDTO {
  identifier: string
  name: string
  dataType: string
  required?: boolean
  unit?: string
  description?: string
}

export interface ThingServiceUpdateDTO {
  name?: string
  callType?: string
  description?: string
}

export interface ThingEventVO {
  id: string
  productId: number
  identifier: string
  name: string
  type: string
  description: string
}

export interface ThingEventCreateDTO {
  identifier: string
  name: string
  type: string
  description?: string
}

export interface ThingEventUpdateDTO {
  name?: string
  type?: string
  description?: string
}
