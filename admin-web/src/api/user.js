import request from '@/utils/request'

export function adminLogin(data) {
  return request({
    url: '/api/admin/login',
    method: 'post',
    data,
  })
}

export function getMerchantList(params) {
  return request({
    url: '/api/admin/merchants',
    method: 'get',
    params,
  })
}
