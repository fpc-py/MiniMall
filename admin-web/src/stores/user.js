import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('admin_user') || '{}'))

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value.username || '管理员')

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

  return { token, userInfo, isLoggedIn, username, login, logout }
})
