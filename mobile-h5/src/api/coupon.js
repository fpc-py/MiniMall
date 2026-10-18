import request from '@/utils/request'

export function getCoupons() {
  return request({ url: '/api/coupons/available', method: 'get' })
}

export function receiveCoupon(couponId) {
  return request({ url: `/api/coupons/${couponId}/receive`, method: 'post' })
}

export function getMyCoupons(params) {
  return request({ url: '/api/coupons/my', method: 'get', params })
}
