import request from '@/utils/request'

export function confirmOrder(data) {
  return request({ url: '/api/orders/confirm', method: 'post', data })
}

export function submitOrder(data) {
  return request({ url: '/api/orders/submit', method: 'post', data })
}

export function getMyOrders(params) {
  return request({ url: '/api/orders', method: 'get', params })
}

export function getOrderDetail(id) {
  return request({ url: `/api/orders/${id}`, method: 'get' })
}

export function cancelOrder(id, reason) {
  return request({ url: `/api/orders/${id}/cancel`, method: 'post', data: { cancelReason: reason } })
}

export function confirmReceipt(id) {
  return request({ url: `/api/orders/${id}/receive`, method: 'post' })
}
