import request from '@/utils/request'

export function getAddresses() {
  return request({ url: '/api/addresses', method: 'get' })
}

export function createAddress(data) {
  return request({ url: '/api/addresses', method: 'post', data })
}

export function updateAddress(id, data) {
  return request({ url: `/api/addresses/${id}`, method: 'put', data })
}

export function deleteAddress(id) {
  return request({ url: `/api/addresses/${id}`, method: 'delete' })
}

export function setDefaultAddress(id) {
  return request({ url: `/api/addresses/${id}/default`, method: 'put' })
}
