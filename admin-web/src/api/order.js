import request from '@/utils/request'

export function getOrderList(params) {
  return request({ url: '/api/admin/orders', method: 'get', params })
}

export function getOrderDetail(id) {
  return request({ url: `/api/admin/orders/${id}`, method: 'get' })
}
