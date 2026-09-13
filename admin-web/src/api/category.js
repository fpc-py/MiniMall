import request from '@/utils/request'

export function getCategoryList() {
  return request({ url: '/api/admin/categories', method: 'get' })
}

export function createCategory(data) {
  return request({ url: '/api/admin/categories', method: 'post', data })
}

export function updateCategory(data) {
  return request({ url: '/api/admin/categories', method: 'put', data })
}

export function deleteCategory(id) {
  return request({ url: `/api/admin/categories/${id}`, method: 'delete' })
}
