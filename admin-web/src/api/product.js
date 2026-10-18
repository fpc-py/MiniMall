import request from '@/utils/request'

/** 图片上传（字段名 file），返回可访问 URL */
export function uploadImage(file, dir = 'product') {
  const formData = new FormData()
  formData.append('file', file)
  if (dir) formData.append('dir', dir)
  return request({
    url: '/api/files/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

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
