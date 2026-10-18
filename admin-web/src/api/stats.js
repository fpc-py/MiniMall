import request from '@/utils/request'

/** 平台统计概览 */
export function getAdminStats() {
  return request({ url: '/api/admin/stats/overview', method: 'get' })
}

/** 店铺统计概览（商家端） */
export function getShopStats() {
  return request({ url: '/api/shop/stats/overview', method: 'get' })
}
