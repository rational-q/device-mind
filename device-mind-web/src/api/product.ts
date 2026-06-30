import request from './request'
import type { ProductVO, ProductCreateDTO, ProductUpdateDTO, ProductPageQueryDTO } from '@/types/product'
import type { PageResult } from '@/types/api'

export function getProductList(data: ProductPageQueryDTO) {
  return request.post<any, PageResult<ProductVO>>('/device-mind/core/products/list', data)
}

export function getProductById(id: number) {
  return request.get<any, ProductVO>('/device-mind/core/products/detail', { params: { id } })
}

export function createProduct(data: ProductCreateDTO) {
  return request.post('/device-mind/core/products', data)
}

export function updateProduct(id: number, data: ProductUpdateDTO) {
  return request.put('/device-mind/core/products', data, { params: { id } })
}

export function deleteProduct(id: number) {
  return request.delete('/device-mind/core/products', { params: { id } })
}
