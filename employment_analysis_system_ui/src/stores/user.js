import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import wsService from '@/utils/websocket'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const username = computed(() => userInfo.value?.username || '')
  const realName = computed(() => userInfo.value?.realName || '')
  const role = computed(() => userInfo.value?.role || '')
  const userId = computed(() => userInfo.value?.userId || '')

  async function login(loginForm) {
    const res = await authApi.login(loginForm)
    // res is the LoginVO directly from the Result wrapper
    token.value = res.token
    userInfo.value = res
    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(res))
    // 连接 WebSocket 实时通知
    wsService.connect(res.token)
    return res
  }

  async function register(registerForm) {
    const res = await authApi.register(registerForm)
    return res
  }

  async function getUserInfo() {
    try {
      const res = await authApi.getUserInfo()
      // res is the LoginVO directly
      userInfo.value = res
      localStorage.setItem('userInfo', JSON.stringify(res))
    } catch (e) {
      logout()
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    wsService.disconnect()
  }

  function hasRole(roleKey) {
    return userInfo.value?.role === roleKey
  }

  return {
    token,
    userInfo,
    username,
    realName,
    role,
    userId,
    login,
    register,
    getUserInfo,
    logout,
    hasRole,
    wsService
  }
})
