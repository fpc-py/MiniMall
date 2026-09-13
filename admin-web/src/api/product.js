import request from '@/utils/request'

export function getSpuList(params) {
  return request({ url: '/api/admin/spus', method: 'get', params })
}

export function getSpuDetail(id) {
  return request({ url: `/api/admin/spus/${id}`, method: 'get' })
}

export function createSpu(data) {
  return request({ url: '/api/admin/spus', method: 'post', data })
}

export function updateSpu(data) {
  return request({ url: '/api/admin/spus', method: 'put', data })
}

export function deleteSpu(id) {
  return request({ url: `/api/admin/spus/${id}`, method: 'delete' })
}
