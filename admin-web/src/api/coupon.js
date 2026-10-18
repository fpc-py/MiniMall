import request from '@/utils/request'

// ==================== 平台优惠券 ====================

export function getAdminCouponList(params) {
  return request({ url: '/api/admin/coupons', method: 'get', params })
}

export function createAdminCoupon(data) {
  return request({ url: '/api/admin/coupons', method: 'post', data })
}

export function updateAdminCouponStatus(id, status) {
  return request({
    url: `/api/admin/coupons/${id}/status`,
    method: 'post',
    params: { status },
  })
}

// ==================== 店铺优惠券（商家端） ====================

export function getShopCouponList(params) {
  return request({ url: '/api/shop/coupons', method: 'get', params })
}

export function createShopCoupon(data) {
  return request({ url: '/api/shop/coupons', method: 'post', data })
}

export function updateShopCouponStatus(id, status) {
  return request({
    url: `/api/shop/coupons/${id}/status`,
    method: 'post',
    params: { status },
  })
}
