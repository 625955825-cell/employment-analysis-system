<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import wsService from '@/utils/websocket'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

onMounted(() => {
  // 如果已登录（刷新页面时），恢复 WebSocket 连接
  if (userStore.token) {
    wsService.connect(userStore.token)

    // 监听实时通知，给出 UI 提示
    wsService.on('message', (data) => {
      if (data.category) {
        const categoryLabel = {
          interview: '面试邀请',
          offer: 'Offer通知',
          agreement: '三方协议',
          application: '投递动态',
          system: '系统通知'
        }[data.category] || '通知'

        ElMessage({
          type: 'success',
          message: `【${categoryLabel}】${data.title || ''}`,
          duration: 5000
        })
      }
    })
  }
})
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>
