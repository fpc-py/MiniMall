import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 认证/授权相关业务码（后端 ErrorCode.Auth）
const AUTH_CODES = [20000, 20001, 20002, 20008, 20009]

request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code === 0 || data.code === 200) return data
    // 业务层无权限（如商家访问平台接口）：仅提示，不强制跳登录
    if (AUTH_CODES.includes(data.code)) {
      ElMessage.error('无权访问该功能')
      return Promise.reject(new Error('无权访问该功能'))
    }
    ElMessage.error(data.msg || '请求失败')
    return Promise.reject(new Error(data.msg))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('admin_token')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error('无权访问该功能')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
