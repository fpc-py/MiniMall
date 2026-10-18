import request from '@/utils/request'

export function applyRefund(data) {
  return request({ url: '/api/refunds', method: 'post', data })
}

export function cancelRefund(id) {
  return request({ url: `/api/refunds/${id}/cancel`, method: 'post' })
}

export function getMyRefunds(params) {
  return request({ url: '/api/refunds/my', method: 'get', params })
}
