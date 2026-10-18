import request from '@/utils/request'

/** 公开分类树（启用状态，最多三级），供类目选择器使用 */
export function getCategoryTree() {
  return request({ url: '/api/product/categories/tree', method: 'get' })
}

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
