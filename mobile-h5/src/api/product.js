import request from '@/utils/request'

export function getProductList(params) {
  return request({ url: '/api/product/spus', method: 'get', params })
}

export function getProductDetail(id) {
  return request({ url: `/api/product/spus/${id}`, method: 'get' })
}

export function getCategoryTree() {
  return request({ url: '/api/product/categories/tree', method: 'get' })
}
