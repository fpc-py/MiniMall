import axios from 'axios'
import { getToken, clearAuth } from './auth'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
})

request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

function redirectToLogin() {
  clearAuth()
  import('@/router').then(({ default: router }) => {
    if (router.currentRoute.value.path === '/login') return
    const redirect = router.currentRoute.value.fullPath
    router.replace({ path: '/login', query: { redirect } })
  })
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 0 || res.code === 200) {
      return res
    }
    if (res.code === 401) {
      redirectToLogin()
    }
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      redirectToLogin()
    }
    return Promise.reject(error)
  }
)

export default request
