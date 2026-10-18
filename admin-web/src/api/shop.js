import request from '@/utils/request'

// ==================== 店铺信息 ====================

export function getShopInfo() {
  return request({ url: '/api/shop/info', method: 'get' })
}

export function updateShopInfo(data) {
  return request({ url: '/api/shop/info', method: 'put', data })
}

// ==================== 店铺商品 ====================

export function getShopProducts(params) {
  return request({ url: '/api/shop/products', method: 'get', params })
}

export function createShopProduct(data) {
  return request({ url: '/api/shop/products', method: 'post', data })
}

/** 上下架：{ spuId, status } status 0下架 1上架 */
export function updateShopProductStatus(data) {
  return request({ url: '/api/shop/products/status', method: 'post', data })
}

// ==================== 店铺订单 ====================

export function getShopOrders(params) {
  return request({ url: '/api/shop/orders', method: 'get', params })
}

export function getShopOrderDetail(id) {
  return request({ url: `/api/shop/orders/${id}`, method: 'get' })
}

/** 发货：{ orderId, deliveryCompany, deliveryNo } */
export function shipOrder(data) {
  return request({ url: '/api/shop/orders/ship', method: 'post', data })
}
