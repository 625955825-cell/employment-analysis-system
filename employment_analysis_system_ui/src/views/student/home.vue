<template>
  <div class="student-home">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <h2 class="page-title">学生首页</h2>
        <p class="page-welcome">欢迎回来，查看你的求职进展与就业动态</p>
      </div>
    </div>

    <!-- 第一行：统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card" @click="$router.push('/student/applications')">
          <div class="stat-icon-wrap stat-icon-blue">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">简历投递</p>
            <p class="stat-value">{{ stats.applicationCount ?? 0 }}</p>
          </div>
          <div class="stat-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" @click="$router.push('/student/interviews')">
          <div class="stat-icon-wrap stat-icon-green">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">面试邀约</p>
            <p class="stat-value">{{ stats.interviewCount ?? 0 }}</p>
          </div>
          <div class="stat-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" @click="$router.push('/student/favorites')">
          <div class="stat-icon-wrap stat-icon-orange">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">职位收藏</p>
            <p class="stat-value">{{ stats.favoriteCount ?? 0 }}</p>
          </div>
          <div class="stat-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-card-employee" @click="$router.push('/student/employment')">
          <div class="stat-icon-wrap stat-icon-red">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">就业状态</p>
            <p class="stat-value stat-value-text">{{ stats.employmentStatusText || '待登记' }}</p>
          </div>
          <div class="stat-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 第二行：快捷入口 + 公告 -->
    <el-row :gutter="20" class="second-row">
      <!-- 左侧：快捷入口 -->
      <el-col :xs="24" :lg="15">
        <div class="section-card">
          <div class="section-header">
            <span class="section-title">快捷入口</span>
          </div>
          <div class="quick-links-grid">
            <div
              v-for="link in quickLinks"
              :key="link.path"
              class="quick-link-item"
              @click="$router.push(link.path)"
            >
              <div class="quick-link-icon" :style="{ background: link.bgColor }">
                <el-icon :style="{ color: link.iconColor }"><component :is="link.icon" /></el-icon>
              </div>
              <div class="quick-link-text">
                <span class="quick-link-title">{{ link.title }}</span>
                <span class="quick-link-desc">{{ link.desc }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：系统公告 -->
      <el-col :xs="24" :lg="9">
        <div class="section-card notice-card">
          <div class="section-header">
            <span class="section-title">系统公告</span>
          </div>
          <div class="notice-body-wrap">
            <div v-if="noticesLoading" class="loading-state">
              <el-icon class="is-loading" :size="20"><Loading /></el-icon>
            </div>
            <div v-else-if="notices.length === 0" class="empty-notices">
              <el-icon :size="32" class="empty-icon"><Bell /></el-icon>
              <p>暂无公告</p>
            </div>
            <div v-else class="notice-list">
              <div
                v-for="notice in notices.slice(0, 5)"
                :key="notice.id"
                class="notice-item"
                @click="viewNotice(notice)"
              >
                <div class="notice-dot"></div>
                <div class="notice-content-wrap">
                  <div class="notice-top">
                    <el-tag v-if="notice.topStatus === '1'" type="danger" size="small" effect="plain" class="top-tag">置顶</el-tag>
                    <span class="notice-title">{{ notice.title }}</span>
                  </div>
                  <span class="notice-time">{{ formatTime(notice.publishTime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 第三行：最近动态 + 推荐职位 -->
    <el-row :gutter="20" class="third-row">
      <el-col :xs="24" :lg="12">
        <div class="section-card">
          <div class="section-header">
            <span class="section-title">最近动态</span>
          </div>
          <div class="activity-list">
            <div class="activity-empty">
              <el-icon :size="28" class="empty-icon"><Clock /></el-icon>
              <p>暂无最近动态</p>
              <span>投递简历后将显示投递记录</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="section-card">
          <div class="section-header">
            <span class="section-title">推荐职位</span>
            <span class="section-more" @click="$router.push('/student/recommendation')">
              查看更多 <el-icon><ArrowRight /></el-icon>
            </span>
          </div>
          <div class="activity-list">
            <div class="activity-empty">
              <el-icon :size="28" class="empty-icon"><Briefcase /></el-icon>
              <p>暂无推荐职位</p>
              <span>完善简历后可获得智能推荐</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 公告弹窗 -->
    <el-dialog v-model="noticeVisible" :title="currentNotice?.title" width="600px" destroy-on-close>
      <div v-if="currentNotice" class="notice-content">
        <div class="notice-meta">
          <el-tag type="info" size="small">{{ currentNotice.noticeType || '通知' }}</el-tag>
          <span class="notice-date">发布于 {{ formatTime(currentNotice.publishTime) }}</span>
          <span class="notice-views">浏览 {{ currentNotice.viewCount || 0 }} 次</span>
        </div>
        <el-divider />
        <div class="notice-body" v-html="currentNotice.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  Document, Tickets, Collection, DataLine, Search, Message, MagicStick,
  User, Loading, ArrowRight, Bell, Clock, Briefcase
} from '@element-plus/icons-vue'
import { studentApi, noticeApi } from '@/api'

const stats = ref({
  applicationCount: 0,
  interviewCount: 0,
  favoriteCount: 0,
  employmentStatus: '',
  employmentStatusText: '待登记'
})

const notices = ref([])
const noticesLoading = ref(false)
const noticeVisible = ref(false)
const currentNotice = ref(null)

const quickLinks = [
  { path: '/student/resume', title: '我的简历', desc: '维护个人简历信息', icon: Document, bgColor: '#eaf2ff', iconColor: '#2f6bff' },
  { path: '/student/job-search', title: '搜索职位', desc: '查找适合你的岗位', icon: Search, bgColor: '#e8fdf0', iconColor: '#67c23a' },
  { path: '/student/applications', title: '投递记录', desc: '查看岗位投递进度', icon: Tickets, bgColor: '#fff8e6', iconColor: '#e6a23c' },
  { path: '/student/interviews', title: '面试邀约', desc: '管理企业面试邀请', icon: Message, bgColor: '#f0f0ff', iconColor: '#7070e8' },
  { path: '/student/favorites', title: '我的收藏', desc: '收藏感兴趣职位', icon: Collection, bgColor: '#fff0f0', iconColor: '#f56c6c' },
  { path: '/student/employment', title: '就业登记', desc: '登记就业去向信息', icon: DataLine, bgColor: '#f0fff0', iconColor: '#5aad5a' },
  { path: '/student/recommendation', title: '职位推荐', desc: '智能匹配推荐岗位', icon: MagicStick, bgColor: '#fdf0ff', iconColor: '#b060d0' },
  { path: '/student/profile', title: '个人信息', desc: '查看与修改个人信息', icon: User, bgColor: '#f5f5f5', iconColor: '#8c8c8c' }
]

function loadStats() {
  studentApi.getHomeStats().then(res => {
    stats.value = res || {}
  }).catch(() => {})
}

function loadNotices() {
  noticesLoading.value = true
  noticeApi.getMyNotices().then(res => {
    notices.value = Array.isArray(res) ? res.slice(0, 10) : []
  }).catch(() => {
    notices.value = []
  }).finally(() => {
    noticesLoading.value = false
  })
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(0, 10)
}

function viewNotice(notice) {
  currentNotice.value = notice
  noticeVisible.value = true
}

onMounted(() => {
  loadStats()
  loadNotices()
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.student-home {
  padding: 24px;
  max-width: 1400px;
}

/* ===== 页面头部 ===== */
.page-header {
  margin-bottom: 24px;
}

.page-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: #0c2660;
  margin: 0;
  letter-spacing: -0.3px;
}

.page-welcome {
  font-size: 13.5px;
  color: #8aa0c8;
  margin: 0;
}

/* ===== 统计卡片行 ===== */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 18px;
  background: #ffffff;
  border: 1px solid #e5eaf3;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(47, 107, 255, 0.04);
}

.stat-card:hover {
  border-color: #c0d8ff;
  box-shadow: 0 6px 24px rgba(47, 107, 255, 0.1);
  transform: translateY(-3px);
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-wrap .el-icon {
  font-size: 22px;
  color: #fff;
}

.stat-icon-blue { background: linear-gradient(135deg, #2f6bff, #60a5fa); }
.stat-icon-green { background: linear-gradient(135deg, #4caf50, #8bc34a); }
.stat-icon-orange { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
.stat-icon-red { background: linear-gradient(135deg, #f56c6c, #fb923c); }

.stat-body {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #8aa0c8;
  margin-bottom: 4px;
  font-weight: 500;
}

.stat-value {
  font-size: 26px;
  font-weight: 800;
  color: #0f2a5f;
  line-height: 1.1;
  letter-spacing: -0.5px;
}

.stat-value-text {
  font-size: 16px;
  font-weight: 700;
  color: #2f6bff;
}

.stat-arrow {
  flex-shrink: 0;
  color: #c0d0e8;
  font-size: 14px;
  transition: color 0.2s, transform 0.2s;
}

.stat-card:hover .stat-arrow {
  color: #2f6bff;
  transform: translateX(3px);
}

/* ===== 第二行 ===== */
.second-row {
  margin-bottom: 20px;
}

/* ===== 通用区块卡片 ===== */
.section-card {
  background: #ffffff;
  border: 1px solid #e5eaf3;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(47, 107, 255, 0.04);
  height: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 14px;
  border-bottom: 1px solid #f0f4ff;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f2a5f;
}

.section-more {
  font-size: 12px;
  color: #2f6bff;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 2px;
  transition: color 0.2s;
}

.section-more:hover {
  color: #1d4ed8;
}

/* ===== 快捷入口网格 ===== */
.quick-links-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  padding: 16px;
}

.quick-link-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.quick-link-item:hover {
  background: #f5f9ff;
  border-color: #c8dcff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(47, 107, 255, 0.08);
}

.quick-link-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.quick-link-item:hover .quick-link-icon {
  transform: scale(1.05);
}

.quick-link-icon .el-icon {
  font-size: 18px;
}

.quick-link-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.quick-link-title {
  font-size: 13px;
  font-weight: 600;
  color: #2d4a72;
  white-space: nowrap;
}

.quick-link-desc {
  font-size: 11px;
  color: #9ab5d6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== 公告区域 ===== */
.notice-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.notice-body-wrap {
  flex: 1;
  padding: 12px 16px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 160px;
  color: #8aa0c8;
}

.empty-notices {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 160px;
  gap: 8px;
  color: #9ab5d6;
}

.empty-notices p {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.empty-icon {
  color: #c0d4ee;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 8px;
  cursor: pointer;
  border-radius: 10px;
  transition: background 0.2s;
  border-bottom: 1px solid #f0f4ff;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-item:hover {
  background: #f5f9ff;
}

.notice-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2f6bff;
  flex-shrink: 0;
  margin-top: 6px;
}

.notice-item:hover .notice-dot {
  background: #1d4ed8;
}

.notice-content-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notice-top {
  display: flex;
  align-items: center;
  gap: 6px;
}

.top-tag {
  flex-shrink: 0;
  font-size: 10px;
  padding: 0 4px;
  height: 18px;
  line-height: 18px;
}

.notice-title {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.notice-item:hover .notice-title {
  color: #2f6bff;
}

.notice-time {
  font-size: 11px;
  color: #9ab5d6;
}

/* ===== 第三行 ===== */
.third-row {
  margin-bottom: 20px;
}

.activity-list {
  padding: 16px;
}

.activity-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 120px;
  gap: 6px;
  color: #9ab5d6;
  background: #f8faff;
  border-radius: 12px;
  border: 1px dashed #d8e5f5;
}

.activity-empty p {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #7090c0;
}

.activity-empty span {
  font-size: 12px;
  color: #b0c4de;
}

/* ===== 公告弹窗 ===== */
.notice-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #909399;
  font-size: 13px;
}

.notice-body {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

/* ===== 响应式适配 ===== */
@media (max-width: 1200px) {
  .quick-links-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 900px) {
  .quick-links-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stats-row .el-col {
    margin-bottom: 12px;
  }

  .stat-card {
    padding: 16px 14px;
  }

  .stat-value {
    font-size: 22px;
  }
}

@media (max-width: 640px) {
  .student-home {
    padding: 16px;
  }

  .quick-links-grid {
    grid-template-columns: repeat(2, 1fr);
    padding: 12px;
    gap: 8px;
  }

  .quick-link-item {
    padding: 10px 10px;
  }

  .quick-link-desc {
    display: none;
  }

  .third-row .el-col {
    margin-bottom: 16px;
  }
}
</style>
