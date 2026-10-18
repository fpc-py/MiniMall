import request from '@/utils/request'

/** 平台退款单列表（只读） */
export function getAdminRefundList(params) {
  return request({ url: '/api/admin/refunds', method: 'get', params })
}

/** 店铺退款单列表（商家端） */
export function getShopRefundList(params) {
  return request({ url: '/api/shop/refunds', method: 'get', params })
}

/** 商家审核退款：status 1同意 3拒绝 */
export function auditRefund(data) {
  return request({ url: '/api/shop/refunds/audit', method: 'post', data })
}
