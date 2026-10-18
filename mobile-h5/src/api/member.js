import request from '@/utils/request'

export function getMemberMe() {
  return request({ url: '/api/member/me', method: 'get' })
}
