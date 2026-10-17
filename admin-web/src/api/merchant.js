import request from '@/utils/request'

export function getPendingShops() {
  return request({ url: '/api/admin/shops/pending', method: 'get' })
}

export function getApplyList(params) {
  return request({ url: '/api/admin/shops/applies', method: 'get', params })
}

export function getShopList(params) {
  return request({ url: '/api/admin/shops', method: 'get', params })
}

export function auditShop(data) {
  return request({ url: '/api/admin/shops/audit', method: 'post', data })
}
