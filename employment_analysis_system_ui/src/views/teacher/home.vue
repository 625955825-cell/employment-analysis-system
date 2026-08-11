<template>
  <div class="teacher-home">
    <!-- 第一行：欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <h2 class="welcome-title">{{ welcomeMessage }}</h2>
        <p class="welcome-sub">{{ currentDate }}</p>
      </div>
      <el-button class="refresh-btn" @click="loadAllData">
        <el-icon><Refresh /></el-icon> 刷新数据
      </el-button>
    </div>

    <!-- 第二行：统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <!-- 院级老师：全院总人数 -->
      <el-col v-if="isDeptTeacher" :xs="12" :sm="6">
        <div class="stat-card" @click="$router.push('/teacher/students')">
          <div class="stat-icon-wrap stat-icon-blue">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">全院总人数</p>
            <p class="stat-value">{{ stats.totalStudents ?? 0 }}</p>
            <p class="stat-tag" v-if="stats.classCount">{{ stats.classCount }} 个班级</p>
          </div>
        </div>
      </el-col>
      <!-- 班主任：本班总人数 -->
      <el-col v-else :xs="12" :sm="8">
        <div class="stat-card">
          <div class="stat-icon-wrap stat-icon-blue">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">本班总人数</p>
            <p class="stat-value">{{ stats.totalStudents ?? 0 }}</p>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="isDeptTeacher ? 6 : 8">
        <div class="stat-card">
          <div class="stat-icon-wrap stat-icon-green">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">{{ isDeptTeacher ? '已就业人数' : '已就业' }}</p>
            <p class="stat-value">{{ employmentStats.employed ?? 0 }}</p>
            <p class="stat-tag">{{ employmentRate }}%</p>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="isDeptTeacher ? 6 : 8">
        <div class="stat-card">
          <div class="stat-icon-wrap stat-icon-orange">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">{{ isDeptTeacher ? '全院就业率' : '班级就业率' }}</p>
            <p class="stat-value">{{ employmentRate }}%</p>
            <p class="stat-tag">{{ isDeptTeacher ? '全院已达' : '班级已达' }}</p>
          </div>
        </div>
      </el-col>

      <!-- 院级老师专属：待审核企业 -->
      <el-col v-if="isDeptTeacher" :xs="12" :sm="6">
        <div class="stat-card" @click="$router.push('/teacher/company-auth')">
          <div class="stat-icon-wrap stat-icon-red">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">待审核企业</p>
            <p class="stat-value">{{ pendingCompanyCount }}</p>
            <p class="stat-tag">{{ pendingCompanyCount === 0 ? '已清空' : '待审核' }}</p>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 系统通知 -->
    <div class="notices-section" v-if="notices.length > 0">
      <div class="notices-header">
        <span class="notices-title">
          <el-icon><Bell /></el-icon> 系统通知
        </span>
        <span class="notices-more" @click="$router.push('/notice')">查看全部 <el-icon><ArrowRight /></el-icon></span>
      </div>
      <div class="notices-list">
        <div
          v-for="notice in notices.slice(0, 4)"
          :key="notice.id"
          class="notice-item"
          @click="viewNotice(notice)"
        >
          <el-tag :type="notice.topStatus === 1 ? 'danger' : 'info'" size="small" effect="plain" class="notice-tag">
            {{ notice.topStatus === 1 ? '置顶' : (notice.noticeType || '通知') }}
          </el-tag>
          <span class="notice-item-title">{{ notice.title }}</span>
          <span class="notice-item-time">{{ formatTime2(notice.publishTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 第三行：就业地图 + 待审核事项 -->
    <el-row :gutter="20" class="third-row">
      <!-- 左侧：就业地区分布 -->
      <el-col :xs="24" :lg="15">
        <div class="section-card map-section-card">
          <div class="section-header">
            <span class="section-title">就业地区分布</span>
            <span class="section-more" @click="$router.push('/teacher/statistics')">详情 <el-icon><ArrowRight /></el-icon></span>
          </div>
          <div ref="mapChartRef" class="map-container"></div>
        </div>
      </el-col>

      <!-- 右侧：待审核事项 -->
      <el-col :xs="24" :lg="9">
        <div class="section-card">
          <div class="section-header">
            <span class="section-title">待审核事项</span>
            <span class="section-badge">{{ pendingCount }}</span>
          </div>
          <div class="pending-body">
            <!-- 待审核企业 -->
            <div v-if="pendingCompanyCount > 0" class="pending-section">
              <div class="pending-section-label">
                <el-icon color="#f59e0b"><OfficeBuilding /></el-icon> 待审核企业入驻
              </div>
              <div
                v-for="item in pendingCompanies.slice(0, 3)"
                :key="item.id"
                class="pending-item"
                @click="$router.push('/teacher/company-auth')"
              >
                <div class="pending-info">
                  <span class="pending-name">{{ item.companyName }}</span>
                  <span class="pending-meta">{{ item.industry }} · {{ formatTime(item.createTime) }}</span>
                </div>
                <el-tag type="warning" size="small">待审核</el-tag>
              </div>
            </div>

            <!-- 待审核就业登记 -->
            <div class="pending-section" v-if="pendingItems.length > 0">
              <div class="pending-section-label">
                <el-icon color="#e6a23c"><Document /></el-icon> 待审核就业登记
              </div>
              <div
                v-for="item in pendingItems.slice(0, 3)"
                :key="item.id"
                class="pending-item"
                @click="$router.push('/teacher/audit')"
              >
                <div class="pending-info">
                  <span class="pending-name">{{ item.realName }} 提交了就业登记</span>
                  <span class="pending-meta">{{ item.employmentType }} · {{ formatTime(item.createTime) }}</span>
                </div>
                <el-tag type="warning" size="small">待审核</el-tag>
              </div>
            </div>

            <!-- 帮扶提醒 -->
            <div class="pending-section" v-if="unreadReminderCount > 0">
              <div class="pending-section-label">
                <el-icon color="#f56c6c"><Warning /></el-icon> 帮扶提醒
              </div>
              <div
                v-for="item in reminders.filter(r => r.isRead === '0').slice(0, 3)"
                :key="item.id"
                class="pending-item"
                @click="viewReminderDetail(item)"
              >
                <div class="pending-info">
                  <span class="pending-name">{{ item.title }}</span>
                  <span class="pending-meta">{{ item.senderName }} · {{ formatTime(item.createTime) }}</span>
                </div>
                <el-tag type="danger" size="small">未读</el-tag>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="pendingCount === 0" class="empty-pending">
              <el-icon :size="40" class="empty-icon"><CircleCheck /></el-icon>
              <p>太棒了</p>
              <span>暂无待审核事项</span>
            </div>

            <!-- 已读提醒提示 -->
            <div v-if="reminders.length > 0 && unreadReminderCount === 0 && pendingItems.length === 0 && pendingCompanies.value.length === 0" class="empty-pending" style="padding-top: 10px;">
              <span style="color: #67c23a; font-size: 13px;">所有提醒已处理完毕</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 第四行：三方协议管理 + 快捷入口 -->
    <el-row :gutter="20" class="fourth-row">
      <!-- 左侧：三方协议管理 -->
      <el-col :xs="24" :lg="12">
        <div class="section-card">
          <div class="section-header">
            <span class="section-title">三方协议管理</span>
            <span class="section-more" @click="$router.push('/teacher/agreements')">详情 <el-icon><ArrowRight /></el-icon></span>
          </div>
          <div class="agreement-body">
            <div class="agreement-stats">
              <div class="agree-stat-item">
                <span class="agree-num">{{ agreementStats.pending ?? 0 }}</span>
                <span class="agree-label">待发起</span>
              </div>
              <div class="agree-stat-item">
                <span class="agree-num">{{ agreementStats.pending ?? 0 }}</span>
                <span class="agree-label">待审核</span>
              </div>
              <div class="agree-stat-item">
                <span class="agree-num">{{ agreementStats.companySigned ?? 0 }}</span>
                <span class="agree-label">企业已签</span>
              </div>
              <div class="agree-stat-item">
                <span class="agree-num">{{ agreementStats.completed ?? 0 }}</span>
                <span class="agree-label">已完成</span>
              </div>
            </div>
            <div class="agreement-progress">
              <div class="progress-header">
                <span class="progress-label">完成率</span>
                <span class="progress-value">{{ agreementProgress }}%</span>
              </div>
              <div class="progress-bar-wrap">
                <div class="progress-bar-fill" :style="{ width: agreementProgress + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：快捷入口 -->
      <el-col :xs="24" :lg="12">
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
              <div class="ql-icon" :style="{ background: link.bgColor }">
                <el-icon :style="{ color: link.iconColor }"><component :is="link.icon" /></el-icon>
              </div>
              <span class="ql-title">{{ link.title }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 提醒详情对话框 -->
    <el-dialog v-model="reminderDetailVisible" title="提醒详情" width="500px" destroy-on-close>
      <el-descriptions :column="1" border v-if="currentReminder">
        <el-descriptions-item label="标题">{{ currentReminder.title }}</el-descriptions-item>
        <el-descriptions-item label="发送人">{{ currentReminder.senderName }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ currentReminder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ currentReminder.className }}</el-descriptions-item>
        <el-descriptions-item label="当时就业率">
          <el-tag type="warning">{{ currentReminder.employmentRate }}%</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="内容">{{ currentReminder.content }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="reminderDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 公告详情对话框 -->
    <el-dialog v-model="noticeDetailVisible" :title="currentNotice?.title" width="600px" destroy-on-close>
      <div v-if="currentNotice" class="notice-content">
        <div class="notice-meta">
          <el-tag type="info" size="small">{{ currentNotice.noticeType || '通知' }}</el-tag>
          <span>发布于 {{ formatTime2(currentNotice.publishTime) }}</span>
          <span>浏览 {{ currentNotice.viewCount || 0 }} 次</span>
        </div>
        <el-divider />
        <div class="notice-body" v-html="currentNotice.content"></div>
        <div v-if="noticeImages.length > 0" class="notice-images">
          <el-image
            v-for="(img, idx) in noticeImages"
            :key="idx"
            :src="img"
            fit="cover"
            class="notice-img"
            :preview-src-list="noticeImages"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import {
  Bell, CircleCheck, Loading, ArrowRight, Refresh, OfficeBuilding,
  Document, TrendCharts, User,
  List, Check, DataLine, Bell as BellIcon, MessageBox, Guide, Warning
} from '@element-plus/icons-vue'
import { teacherApi, noticeApi } from '@/api'
import wsService from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import * as echarts from 'echarts'

const userStore = useUserStore()

// ========== 角色判断 ==========
const isDeptTeacher = computed(() => userStore.userInfo?.role === 'dept_teacher')

// ========== 欢迎语 ==========
const welcomeMessage = computed(() => {
  const name = userStore.userInfo?.realName
  const college = userStore.userInfo?.deptName || ''

  if (name) {
    return `欢迎回来，${name}老师！`
  }
  if (college) {
    const label = isDeptTeacher.value ? '院级老师' : '班主任'
    return `欢迎回来，${college}${label}！`
  }
  return '欢迎回来，老师！'
})

const currentDate = computed(() => {
  const now = new Date()
  const weekDay = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}年${m}月${d}日 ${weekDay[now.getDay()]}`
})

// ========== 工作台数据 ==========
const homeStats = ref({})
const employmentStats = ref({})
const agreementStats = ref({})
const stats = computed(() => homeStats.value || {})

const employmentRate = computed(() => {
  const total = employmentStats.value.total || 0
  const employed = employmentStats.value.employed || 0
  if (total === 0) return '0.0'
  return ((employed / total) * 100).toFixed(1)
})

const agreementProgress = computed(() => {
  const total = agreementStats.value.total || 0
  const completed = agreementStats.value.completed || 0
  if (total === 0) return 0
  return Math.round((completed / total) * 100)
})

// ========== 待审核 ==========
const pendingItems = ref([])
const pendingCompanies = ref([])
const pendingCount = computed(() => pendingItems.value.length + pendingCompanies.value.length + unreadReminderCount.value)
const pendingCompanyCount = ref(0)

// ========== 提醒 ==========
const reminders = ref([])
const unreadReminderCount = ref(0)
const reminderDetailVisible = ref(false)
const currentReminder = ref(null)

// ========== 公告 ==========
const notices = ref([])
const noticeDetailVisible = ref(false)
const currentNotice = ref(null)

// ========== 地图 ==========
const mapChartRef = ref(null)
let mapChart = null

// ========== 快捷入口 ==========
const quickLinks = [
  { path: '/teacher/students', title: '各班人数', icon: User, bgColor: '#eaf2ff', iconColor: '#2f6bff' },
  { path: '/teacher/statistics', title: '就业统计', icon: TrendCharts, bgColor: '#e8fdf0', iconColor: '#67c23a' },
  { path: '/teacher/company-auth', title: '企业入驻', icon: OfficeBuilding, bgColor: '#fff8e6', iconColor: '#e6a23c' },
  { path: '/teacher/agreements', title: '三方协议', icon: List, bgColor: '#f0f0ff', iconColor: '#7070e8' },
  { path: '/teacher/data-approval', title: '材料审核', icon: Check, bgColor: '#f0fff0', iconColor: '#5aad5a' },
  { path: '/teacher/conversation', title: '沟通记录', icon: MessageBox, bgColor: '#fdf0ff', iconColor: '#b060d0' },
  { path: '/teacher/audit', title: '就业审核', icon: DataLine, bgColor: '#fff0f0', iconColor: '#f56c6c' },
  { path: '/teacher/home', title: '数据总览', icon: Guide, bgColor: '#f5f5f5', iconColor: '#8c8c8c' }
]

// ========== 加载数据 ==========
async function loadAllData() {
  await Promise.all([
    loadHomeStats(),
    loadPendingEmployments(),
    loadPendingCompanies(),
    loadAgreementStats(),
    loadNotices(),
    loadReminders()
  ])
}

async function loadHomeStats() {
  try {
    const res = await teacherApi.getHomeStats()
    homeStats.value = res || {}
    employmentStats.value = res?.employmentStats || {}
  } catch {
    homeStats.value = {}
    employmentStats.value = {}
  }
}

async function loadPendingEmployments() {
  try {
    const res = await teacherApi.getPendingEmployments()
    pendingItems.value = Array.isArray(res) ? res : []
  } catch {
    pendingItems.value = []
  }
}

async function loadPendingCompanies() {
  try {
    const res = await teacherApi.getPendingCompanyAuths()
    pendingCompanies.value = Array.isArray(res) ? res : []
    pendingCompanyCount.value = pendingCompanies.value.length
  } catch {
    pendingCompanies.value = []
    pendingCompanyCount.value = 0
  }
}

async function loadAgreementStats() {
  try {
    const res = await teacherApi.getAgreementStats()
    agreementStats.value = (Array.isArray(res) && res.length > 0) ? res[0] : {}
  } catch {
    agreementStats.value = {}
  }
}

function loadNotices() {
  noticeApi.getMyNotices().then(res => {
    notices.value = Array.isArray(res) ? res.slice(0, 5) : []
  }).catch(() => {
    notices.value = []
  })
}

function loadReminders() {
  teacherApi.getEmploymentReminders().then(res => {
    reminders.value = Array.isArray(res) ? res : []
    unreadReminderCount.value = reminders.value.filter(r => r.isRead === '0').length
  }).catch(() => {
    reminders.value = []
    unreadReminderCount.value = 0
  })
}

// ========== 地图 ==========
const chinaNameMap = {
  '北京': '北京市', '天津': '天津市', '上海': '上海市', '重庆': '重庆市',
  '河北': '河北省', '山西': '山西省', '辽宁': '辽宁省', '吉林': '吉林省',
  '黑龙江': '黑龙江省', '江苏': '江苏省', '浙江': '浙江省', '安徽': '安徽省',
  '福建': '福建省', '江西': '江西省', '山东': '山东省', '河南': '河南省',
  '湖北': '湖北省', '湖南': '湖南省', '广东': '广东省', '海南': '海南省',
  '四川': '四川省', '贵州': '贵州省', '云南': '云南省', '陕西': '陕西省',
  '甘肃': '甘肃省', '青海': '青海省', '内蒙古': '内蒙古自治区',
  '广西': '广西壮族自治区', '西藏': '西藏自治区', '宁夏': '宁夏回族自治区',
  '新疆': '新疆维吾尔自治区', '香港': '香港特别行政区', '澳门': '澳门特别行政区',
  '台湾': '台湾省'
}

function normalizeProvince(name) {
  if (!name) return null
  return chinaNameMap[name] || name
}

async function initMap() {
  await nextTick()
  if (!mapChartRef.value) return
  try {
    mapChart = echarts.init(mapChartRef.value)
    const chinaGeo = await fetch('/china.json').then(r => r.json())
    echarts.registerMap('china', chinaGeo)

    const provinceDist = employmentStats.value.provinceDistribution || {}
    const regionData = Object.entries(provinceDist).map(([name, value]) => ({
      name: normalizeProvince(name),
      value: value
    }))

    const option = {
      tooltip: {
        trigger: 'item',
        formatter: (params) => {
          if (params.value > 0) {
            return `${params.name}<br/>就业人数: ${params.value} 人`
          }
          return `${params.name}<br/>暂无数据`
        }
      },
      visualMap: {
        min: 0,
        max: Math.max(...regionData.map(d => d.value), 1),
        text: ['高', '低'],
        realtime: false,
        calculable: true,
        inRange: { color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#f46d43', '#d73027', '#a50026'] },
        textStyle: { fontSize: 12 }
      },
      series: [{
        name: '就业人数',
        type: 'map',
        map: 'china',
        roam: true,
        zoom: 1.2,
        scaleLimit: { min: 0.8, max: 3 },
        label: {
          show: false,
          fontSize: 10
        },
        emphasis: {
          label: { show: true, fontSize: 11, color: '#333' },
          itemStyle: { areaColor: '#ffa500' }
        },
        itemStyle: {
          areaColor: '#f0f0f0',
          borderColor: '#b0b0b0',
          borderWidth: 0.5
        },
        data: regionData
      }]
    }

    mapChart.setOption(option)
    mapChart.resize()
  } catch (e) {
    console.warn('地图渲染失败:', e)
  }
}

function resizeMap() {
  if (mapChart) mapChart.resize()
}

// ========== 工具 ==========
function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(0, 16)
}

function formatTime2(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(0, 10)
}

function viewNotice(notice) {
  currentNotice.value = notice
  noticeDetailVisible.value = true
}

const noticeImages = computed(() => {
  if (!currentNotice.value?.images) return []
  try {
    const imgs = typeof currentNotice.value.images === 'string'
      ? JSON.parse(currentNotice.value.images)
      : currentNotice.value.images
    return Array.isArray(imgs) ? imgs : []
  } catch { return [] }
})

async function viewReminderDetail(reminder) {
  currentReminder.value = reminder
  reminderDetailVisible.value = true
  if (reminder.isRead === '0') {
    try {
      await teacherApi.markReminderAsRead(reminder.id)
      reminder.isRead = '1'
    } catch {}
  }
}

// ========== 生命周期 ==========
onMounted(async () => {
  await loadAllData()
  await nextTick()
  try { initMap() } catch (e) { console.warn('地图初始化失败:', e) }
  window.addEventListener('resize', resizeMap)

  // 监听 WebSocket 新消息，收到提醒时自动刷新
  wsService.on('message', (data) => {
    if (data?.type === 'notification' && data?.category === 'system') {
      loadReminders()
    }
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeMap)
  if (mapChart) {
    mapChart.dispose()
    mapChart = null
  }
  // 移除 WebSocket 监听
  wsService.off('message')
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.teacher-home {
  padding: 24px;
  max-width: 1400px;
}

/* ===== 欢迎横幅 ===== */
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  background: linear-gradient(135deg, #2f6bff 0%, #3d8bff 50%, #60a5fa 100%);
  border-radius: 14px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(47, 107, 255, 0.2);
}

.welcome-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.welcome-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.welcome-sub {
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0;
}

.refresh-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #fff;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.6);
}

/* ===== 统计卡片行 ===== */
.stats-row {
  margin-bottom: 20px;
}

/* ===== 系统通知 ===== */
.notices-section {
  background: #ffffff;
  border: 1px solid #e5eaf3;
  border-radius: 14px;
  padding: 14px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(47, 107, 255, 0.04);
}

.notices-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.notices-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #0f2a5f;
}

.notices-title .el-icon {
  color: #2f6bff;
}

.notices-more {
  font-size: 12px;
  color: #2f6bff;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 2px;
  transition: color 0.2s;
}

.notices-more:hover {
  color: #1d4ed8;
}

.notices-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.notice-item:hover {
  background: #f5f9ff;
}

.notice-tag {
  flex-shrink: 0;
}

.notice-item-title {
  flex: 1;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-item-time {
  font-size: 11px;
  color: #c0d0e8;
  flex-shrink: 0;
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
  margin-bottom: 12px;
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

.stat-tag {
  font-size: 11px;
  color: #9ab5d6;
  margin-top: 2px;
}

/* ===== 第三、四行 ===== */
.third-row,
.fourth-row {
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
  display: flex;
  flex-direction: column;
}

.map-section-card {
  min-height: 420px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 14px;
  border-bottom: 1px solid #f0f4ff;
  flex-shrink: 0;
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

.section-badge {
  font-size: 12px;
  font-weight: 700;
  color: #f56c6c;
  background: #fff0f0;
  padding: 2px 8px;
  border-radius: 20px;
}

/* ===== 地图 ===== */
.map-container {
  flex: 1;
  min-height: 380px;
  position: relative;
  padding: 8px;
}

/* ===== 待审核事项 ===== */
.pending-body {
  padding: 14px 16px;
  flex: 1;
  overflow-y: auto;
  max-height: 320px;
}

.pending-section {
  margin-bottom: 14px;
}

.pending-section-label {
  font-size: 12px;
  font-weight: 600;
  color: #8aa0c8;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.pending-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid #f0f4ff;
  margin-bottom: 6px;
  transition: all 0.2s;
}

.pending-item:hover {
  background: #f5f9ff;
  border-color: #c8dcff;
}

.pending-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pending-name {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pending-meta {
  font-size: 11px;
  color: #9ab5d6;
}

.empty-pending {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 6px;
}

.empty-pending span {
  font-size: 12px;
  color: #b0c4de;
}

/* ===== 三方协议 ===== */
.agreement-body {
  padding: 20px;
}

.agreement-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.agree-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 8px;
  background: #f5f9ff;
  border-radius: 12px;
  border: 1px solid #e5eaf3;
}

.agree-num {
  font-size: 22px;
  font-weight: 800;
  color: #0f2a5f;
}

.agree-label {
  font-size: 11px;
  color: #8aa0c8;
  font-weight: 500;
}

.agreement-progress {
  margin-top: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-label {
  font-size: 12px;
  color: #8aa0c8;
  font-weight: 500;
}

.progress-value {
  font-size: 14px;
  font-weight: 700;
  color: #2f6bff;
}

.progress-bar-wrap {
  height: 8px;
  background: #f0f4ff;
  border-radius: 10px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2f6bff, #60a5fa);
  border-radius: 10px;
  transition: width 0.6s ease;
}

/* ===== 快捷入口 ===== */
.quick-links-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  padding: 16px;
}

.quick-link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 10px;
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

.ql-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ql-icon .el-icon {
  font-size: 18px;
}

.ql-title {
  font-size: 12px;
  font-weight: 600;
  color: #2d4a72;
  text-align: center;
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

.notice-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.notice-img {
  width: 140px;
  height: 100px;
  border-radius: 8px;
  border: 1px solid #e5eaf3;
  cursor: pointer;
  transition: transform 0.2s;
}

.notice-img:hover {
  transform: scale(1.03);
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .stats-row .el-col {
    margin-bottom: 0;
  }

  .agreement-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .quick-links-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 640px) {
  .teacher-home {
    padding: 16px;
  }

  .welcome-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .quick-links-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    padding: 12px;
  }

  .third-row .el-col,
  .fourth-row .el-col {
    margin-bottom: 16px;
  }
}

@media (max-width: 480px) {
  .notice-item-title {
    font-size: 12px;
  }
}
</style>
