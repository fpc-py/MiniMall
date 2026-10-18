import request from '@/utils/request'

/** 会员列表（platform） */
export function getMemberList(params) {
  return request({ url: '/api/admin/members', method: 'get', params })
}

/** 启用/禁用会员，status 1启用 0禁用 */
export function updateMemberStatus(id, status) {
  return request({
    url: `/api/admin/members/${id}/status`,
    method: 'post',
    params: { status },
  })
}
