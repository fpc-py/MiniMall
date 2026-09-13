import request from '@/utils/request'

export function getCart() {
  return request({ url: '/api/cart', method: 'get' })
}

export function addToCart(skuId, quantity) {
  return request({ url: '/api/cart/add', method: 'post', data: { skuId, quantity } })
}

export function updateCartItem(skuId, data) {
  return request({ url: `/api/cart/items/${skuId}`, method: 'put', data })
}

export function removeCartItem(skuId) {
  return request({ url: `/api/cart/items/${skuId}`, method: 'delete' })
}

export function clearCart() {
  return request({ url: '/api/cart', method: 'delete' })
}

export function selectAll(selected) {
  return request({ url: '/api/cart/select-all', method: 'put', params: { selected } })
}

export function getCartSummary() {
  return request({ url: '/api/cart/summary', method: 'get' })
}
