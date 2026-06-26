export interface ProductVO {
  id: number
  productKey: string
  name: string
  description: string
  protocolType: string
  dataFormat: string
  status: string
  createdDate: string
}

export interface ProductCreateDTO {
  productKey: string
  name: string
  description?: string
  protocolType: string
  dataFormat: string
}

export interface ProductUpdateDTO {
  name?: string
  description?: string
  protocolType?: string
  dataFormat?: string
  status?: string
}

export interface ProductPageQueryDTO {
  productKey?: string
  name?: string
  status?: string
  pageNum?: number
  pageSize?: number
}
