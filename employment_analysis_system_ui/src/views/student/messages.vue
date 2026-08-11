<template>
  <div class="page-container">
    <h2>消息通知</h2>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>通知列表</span>
          <el-button type="primary" link @click="handleMarkAllRead" v-if="unreadCount > 0">全部标为已读</el-button>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="records.length === 0" class="empty-state">
        <el-empty description="暂无通知消息" />
      </div>

      <div v-else>
        <div v-for="item in records" :key="item.id" class="notification-item" :class="{ unread: item.isRead === '0' }">
          <div class="notification-icon">
            <el-icon :size="20"><Bell /></el-icon>
          </div>
          <div class="notification-content" @click="handleItemClick(item)">
            <div class="notification-title">
              <span v-if="item.isRead === '0'" class="unread-dot"></span>
              {{ item.title }}
            </div>
            <div class="notification-body" v-if="item.content">{{ item.content }}</div>
            <div class="notification-meta">
              <span>{{ item.senderName || '系统通知' }}</span>
              <span v-if="item.createTime">{{ formatDateTime(item.createTime) }}</span>
            </div>
          </div>
          <div class="notification-actions">
            <el-button type="primary" link size="small" v-if="item.isRead === '0'" @click="handleMarkRead(item)">标为已读</el-button>
          </div>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="params.page"
            v-model:page-size="params.size"
            :page-sizes="[10, 20, 30]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Loading, Bell } from '@element-plus/icons-vue'
import { notificationApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const unreadCount = ref(0)
const params = reactive({ page: 1, size: 10 })

function loadData() {
  loading.value = true
  notificationApi.getList(params).then(res => {
    records.value = res?.records || res?.list || []
    total.value = res?.total || records.value.length
    unreadCount.value = res?.unreadCount || 0
  }).catch(() => {
    records.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function formatDateTime(dateStr) {
  if (!dateStr) return ''
  try { return dateStr.substring(0, 16).replace('T', ' ') } catch { return dateStr }
}

function handleMarkRead(item) {
  notificationApi.markAsRead(item.id).then(() => {
    item.isRead = '1'
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    ElMessage.success('已标为已读')
  })
}

function handleMarkAllRead() {
  notificationApi.markAllAsRead().then(() => {
    records.value.forEach(item => { item.isRead = '1' })
    unreadCount.value = 0
    ElMessage.success('全部已读')
  })
}

function handleItemClick(item) {
  if (item.isRead === '0') {
    handleMarkRead(item)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.notification-item { display: flex; align-items: flex-start; padding: 16px 0; border-bottom: 1px solid #f0f0f0; gap: 12px; }
.notification-item:last-child { border-bottom: none; }
.notification-item.unread { background: #f0f7ff; margin: 0 -16px; padding: 16px; border-radius: 4px; }
.notification-icon { color: #409eff; padding-top: 2px; flex-shrink: 0; }
.notification-content { flex: 1; min-width: 0; cursor: pointer; }
.notification-title { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 4px; display: flex; align-items: center; gap: 6px; }
.notification-body { font-size: 13px; color: #666; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notification-meta { font-size: 12px; color: #999; display: flex; gap: 12px; }
.notification-actions { flex-shrink: 0; }
.unread-dot { width: 8px; height: 8px; background: #f56c6c; border-radius: 50%; flex-shrink: 0; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
