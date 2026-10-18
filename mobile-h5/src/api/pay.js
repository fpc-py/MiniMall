import request from '@/utils/request'

export function createPay(data) {
  return request({ url: '/api/pay/create', method: 'post', data })
}

export function payCallback(data) {
  return request({ url: '/api/pay/callback', method: 'post', data })
}
