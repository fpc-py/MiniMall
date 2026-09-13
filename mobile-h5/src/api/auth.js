import request from '@/utils/request'

export function sendSmsCode(phone) {
  return request({
    url: '/api/auth/sms/send',
    method: 'post',
    data: { phone },
  })
}

export function smsLogin(phone, code) {
  return request({
    url: '/api/auth/sms/login',
    method: 'post',
    data: { phone, code },
  })
}
