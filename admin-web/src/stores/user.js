import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('admin_user') || '{}'))

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value.username || '管理员')
  const realName = computed(() => userInfo.value.realName || username.value)
  // 后端 LoginVO.userType：平台用户(type 0/1) -> 'admin'；商家用户(type 2/3) -> 'shop'
  const userType = computed(() => userInfo.value.userType || 'admin')
  const shopId = computed(() => userInfo.value.shopId || null)
  const isMerchant = computed(() => userType.value === 'shop')
  const isPlatform = computed(() => userType.value !== 'shop')

  function login(loginToken, info) {
    token.value = loginToken
    userInfo.value = info
    localStorage.setItem('admin_token', loginToken)
    localStorage.setItem('admin_user', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    realName,
    userType,
    shopId,
    isMerchant,
    isPlatform,
    login,
    logout,
  }
})
