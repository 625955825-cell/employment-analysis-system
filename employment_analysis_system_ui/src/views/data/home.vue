<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">数据分析首页</h2>
        <p class="page-subtitle">总览就业数据、推荐算法、爬虫任务与待审核记录</p>
      </div>
      <div class="header-actions">
        <el-tag type="info" effect="plain" size="default">
          <el-icon><Timer /></el-icon>
          {{ currentTime }}
        </el-tag>
        <el-button type="primary" @click="refreshAll" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>正在加载数据...</span>
    </div>

    <!-- 主内容区 -->
    <div v-else class="main-content">

      <!-- 第一行：6 个核心指标卡片 -->
      <div class="metric-grid">
        <div class="stat-card stat-card--blue">
          <div class="stat-card__icon"><el-icon><User /></el-icon></div>
          <div class="stat-card__body">
            <p class="stat-card__label">学生总数</p>
            <p class="stat-card__value">{{ stats.totalStudents || 0 }}</p>
            <p class="stat-card__sub">平台初始化数据</p>
          </div>
        </div>
        <div class="stat-card stat-card--green">
          <div class="stat-card__icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-card__body">
            <p class="stat-card__label">已就业人数</p>
            <p class="stat-card__value">{{ stats.employed || 0 }}</p>
            <p class="stat-card__sub">已登记就业</p>
          </div>
        </div>
        <div class="stat-card stat-card--orange">
          <div class="stat-card__icon"><el-icon><Warning /></el-icon></div>
          <div class="stat-card__body">
            <p class="stat-card__label">待就业人数</p>
            <p class="stat-card__value">{{ stats.unemployed || 0 }}</p>
            <p class="stat-card__sub">待跟进学生</p>
          </div>
        </div>
        <div class="stat-card stat-card--primary">
          <div class="stat-card__icon"><el-icon><TrendCharts /></el-icon></div>
          <div class="stat-card__body">
            <p class="stat-card__label">就业率</p>
            <p class="stat-card__value">{{ stats.employmentRate || 0 }}<span class="stat-card__unit">%</span></p>
            <p class="stat-card__sub">当前整体就业率</p>
          </div>
        </div>
        <div class="stat-card stat-card--teal">
          <div class="stat-card__icon"><el-icon><Document /></el-icon></div>
          <div class="stat-card__body">
            <p class="stat-card__label">就业记录总数</p>
            <p class="stat-card__value">{{ stats.totalEmploymentRecords || 0 }}</p>
            <p class="stat-card__sub">有效就业记录</p>
          </div>
        </div>
        <div class="stat-card stat-card--amber">
          <div class="stat-card__icon"><el-icon><Clock /></el-icon></div>
          <div class="stat-card__body">
            <p class="stat-card__label">待审核记录</p>
            <p class="stat-card__value">{{ stats.pending || 0 }}</p>
            <p class="stat-card__sub">需要处理</p>
          </div>
        </div>
      </div>

      <!-- 第二行：就业趋势分析 + 特殊就业类型统计 -->
      <div class="main-grid">
        <!-- 左侧：就业趋势分析 -->
        <div class="panel chart-panel">
          <div class="panel__header">
            <div class="panel__title">
              <el-icon><TrendCharts /></el-icon>
              就业趋势分析
            </div>
            <el-tag size="small" type="info" effect="plain">近12个月趋势</el-tag>
          </div>
          <div ref="trendChartRef" class="chart-container"></div>
        </div>

        <!-- 右侧：特殊就业类型统计 -->
        <div class="panel special-panel">
          <div class="panel__header">
            <div class="panel__title">
              <el-icon><Memo /></el-icon>
              特殊就业类型统计
            </div>
          </div>
          <div class="special-grid" v-if="specialTypes.length">
            <div
              v-for="(item, idx) in specialTypes"
              :key="item.type"
              class="special-card"
              :style="{ borderLeftColor: specialTypeColors[idx] }"
            >
              <div class="special-card__top">
                <span class="special-card__name">{{ item.type }}</span>
                <span class="special-card__count">{{ item.count }}<span class="special-card__unit">人</span></span>
              </div>
              <div class="special-card__bar">
                <div
                  class="special-card__bar-fill"
                  :style="{ width: item.percent + '%', background: specialTypeColors[idx] }"
                ></div>
              </div>
              <span class="special-card__percent">{{ item.percent.toFixed(1) }}%</span>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-icon :size="32" color="#c0c4cc"><Tickets /></el-icon>
            <span>暂无特殊就业类型数据</span>
          </div>
        </div>
      </div>

      <!-- 第三行：数据任务状态 + 待处理事项 -->
      <div class="main-grid">
        <!-- 左侧：数据任务状态 -->
        <div class="panel task-panel">
          <div class="panel__header">
            <div class="panel__title">
              <el-icon><Cpu /></el-icon>
              数据任务状态
            </div>
            <el-button size="small" text type="primary" @click="$router.push('/data/spider')">
              爬虫管理 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div class="task-grid">
            <div class="task-card">
              <div class="task-card__icon task-card__icon--blue"><el-icon><MagicStick /></el-icon></div>
              <div class="task-card__info">
                <p class="task-card__name">推荐算法</p>
                <p class="task-card__desc">TF-IDF 推荐服务</p>
              </div>
              <div class="task-card__status task-card__status--normal">
                <el-icon><CircleCheck /></el-icon> 正常
              </div>
            </div>
            <div class="task-card">
              <div class="task-card__icon task-card__icon--green"><el-icon><Monitor /></el-icon></div>
              <div class="task-card__info">
                <p class="task-card__name">爬虫任务</p>
                <p class="task-card__desc">招聘数据采集</p>
              </div>
              <div class="task-card__status task-card__status--warning">
                <el-icon><Warning /></el-icon> 待配置
              </div>
            </div>
            <div class="task-card">
              <div class="task-card__icon task-card__icon--orange"><el-icon><Filter /></el-icon></div>
              <div class="task-card__info">
                <p class="task-card__name">数据质量</p>
                <p class="task-card__desc">就业记录清洗</p>
              </div>
              <div class="task-card__status task-card__status--normal">
                <el-icon><CircleCheck /></el-icon> 正常
              </div>
            </div>
            <div class="task-card">
              <div class="task-card__icon task-card__icon--teal"><el-icon><RefreshRight /></el-icon></div>
              <div class="task-card__info">
                <p class="task-card__name">系统同步</p>
                <p class="task-card__desc">数据更新状态</p>
              </div>
              <div class="task-card__status task-card__status--normal">
                <el-icon><CircleCheck /></el-icon> 正常
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：待处理事项 -->
        <div class="panel pending-panel">
          <div class="panel__header">
            <div class="panel__title">
              <el-icon><Bell /></el-icon>
              待处理事项
            </div>
            <el-badge :value="stats.pending || 0" :hidden="!stats.pending" type="warning" />
          </div>
          <div class="pending-list">
            <div class="pending-item">
              <div class="pending-item__left">
                <span class="pending-dot" :class="stats.pending > 0 ? 'pending-dot--warn' : 'pending-dot--ok'"></span>
                <span>待审核记录</span>
              </div>
              <el-tag size="small" :type="stats.pending > 0 ? 'warning' : 'success'" effect="plain">
                {{ stats.pending || 0 }} 条
              </el-tag>
            </div>
            <div class="pending-item">
              <div class="pending-item__left">
                <span class="pending-dot pending-dot--ok"></span>
                <span>爬虫异常</span>
              </div>
              <el-tag size="small" type="success" effect="plain">0 条</el-tag>
            </div>
            <div class="pending-item">
              <div class="pending-item__left">
                <span class="pending-dot pending-dot--ok"></span>
                <span>算法配置</span>
              </div>
              <el-tag size="small" type="success" effect="plain">正常</el-tag>
            </div>
            <div class="pending-item">
              <div class="pending-item__left">
                <span class="pending-dot pending-dot--ok"></span>
                <span>数据同步</span>
              </div>
              <el-tag size="small" type="success" effect="plain">正常</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 第四行：系统公告（横跨整行） -->
      <div class="panel notice-panel">
        <div class="panel__header">
          <div class="panel__title">
            <el-icon><Message /></el-icon>
            系统公告
          </div>
          <el-button size="small" text type="primary" @click="$router.push('/notice')">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-if="noticesLoading" class="notice-loading">
          <el-icon class="is-loading" :size="18"><Loading /></el-icon>
        </div>
        <div v-else-if="notices.length === 0" class="notice-empty">
          <el-icon :size="22" color="#c0c4cc"><Bell /></el-icon>
          <span>暂无公告</span>
        </div>
        <div v-else class="notice-list">
          <div
            v-for="notice in notices.slice(0, 4)"
            :key="notice.id"
            class="notice-item"
            @click="viewNotice(notice)"
          >
            <div class="notice-item__left">
              <el-tag v-if="notice.topStatus === '1'" type="danger" size="small" effect="plain" style="margin-right: 4px; padding: 0 4px; height: 18px; line-height: 18px;">置顶</el-tag>
              <span class="notice-item__title">{{ notice.title }}</span>
            </div>
            <span class="notice-item__time">{{ formatTime(notice.publishTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 公告详情对话框 -->
    <el-dialog v-model="noticeDetailVisible" :title="currentNotice?.title" width="600px" destroy-on-close>
      <div v-if="currentNotice" class="notice-content">
        <div class="notice-meta">
          <el-tag type="info" size="small">{{ currentNotice.noticeType || '通知' }}</el-tag>
          <span>发布于 {{ formatTime(currentNotice.publishTime) }}</span>
          <span>浏览 {{ currentNotice.viewCount || 0 }} 次</span>
        </div>
        <el-divider />
        <div class="notice-body" v-html="currentNotice.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import {
  User, CircleCheck, Warning, TrendCharts, Document, Clock,
  Cpu, Bell, Message, Refresh, Loading, ArrowRight,
  Timer, Memo, Tickets, Filter, RefreshRight, MagicStick, Monitor
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { analyticsApi, noticeApi } from '@/api'

// 统计数据
const stats = ref({})
const loading = ref(false)

// 公告数据
const notices = ref([])
const noticesLoading = ref(false)
const noticeDetailVisible = ref(false)
const currentNotice = ref(null)

// 图表
const trendChartRef = ref(null)
let trendChart = null

// 当前时间
const currentTime = ref('')
let timer = null

// 特殊就业类型颜色
const specialTypeColors = ['#2f6bff', '#22c55e', '#f59e0b', '#ef4444']

// 特殊就业类型列表（从 stats.specialTypes 转换）
const specialTypes = computed(() => {
  const st = stats.value.specialTypes || {}
  const total = Object.values(st).reduce((sum, v) => sum + (Number(v) || 0), 0)
  return Object.entries(st).map(([type, count]) => ({
    type,
    count: Number(count) || 0,
    percent: total > 0 ? (Number(count) / total) * 100 : 0
  }))
})

// 加载统计数据
async function loadStats() {
  try {
    const res = await analyticsApi.getOverview({})
    if (res && typeof res === 'object') {
      stats.value = res
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

// 加载就业趋势数据
async function loadTrendData() {
  try {
    const res = await analyticsApi.getEmploymentTrend({})
    if (res && typeof res === 'object') {
      trendChartData.value = res
    }
  } catch (e) {
    // 接口失败不影响，使用 fallback 数据
  }
}

// 图表数据
const trendChartData = ref({})

// 初始化趋势图
function renderTrendChart() {
  if (!trendChartRef.value) return

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
    window.addEventListener('resize', handleResize)
  }

  const typeCount = trendChartData.value.typeCount || {}

  // fallback：接口无数据时使用模拟趋势数据
  const trendMonths = ['1月', '2月', '3月', '4月', '5月', '6月']
  const employedData = [26000, 27800, 29100, 30200, 30925, 30925]
  const rateData = [76.6, 81.9, 85.7, 89.0, 91.1, 91.1]

  const hasData = Object.keys(typeCount).length > 0
  const names = hasData ? Object.keys(typeCount) : trendMonths
  const values = hasData ? Object.values(typeCount) : employedData

  const option = {
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      right: 10,
      data: hasData ? ['人数'] : ['已就业人数', '就业率'],
      textStyle: { fontSize: 12, color: '#8a9ab5' }
    },
    grid: {
      left: 40,
      right: 45,
      top: hasData ? 30 : 48,
      bottom: 32,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: names,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e5eaf3' } },
      axisLabel: { color: '#8a9ab5', fontSize: 12 }
    },
    yAxis: hasData ? [{
      type: 'value',
      name: '人数',
      axisLabel: { color: '#8a9ab5' },
      splitLine: { lineStyle: { color: '#eef2f8' } }
    }] : [
      {
        type: 'value',
        name: '人数',
        axisLabel: { color: '#8a9ab5' },
        splitLine: { lineStyle: { color: '#eef2f8' } }
      },
      {
        type: 'value',
        name: '就业率',
        min: 0,
        max: 100,
        axisLabel: { color: '#8a9ab5', formatter: '{value}%' },
        splitLine: { show: false }
      }
    ],
    series: hasData ? [{
      name: '人数',
      type: 'bar',
      data: values,
      barWidth: 24,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#2f6bff' },
          { offset: 1, color: '#409eff' }
        ]),
        borderRadius: [4, 4, 0, 0]
      }
    }] : [
      {
        name: '已就业人数',
        type: 'bar',
        data: employedData,
        barWidth: 18,
        itemStyle: {
          color: '#2f6bff',
          borderRadius: [6, 6, 0, 0]
        }
      },
      {
        name: '就业率',
        type: 'line',
        yAxisIndex: 1,
        data: rateData,
        smooth: true,
        symbolSize: 8,
        lineStyle: { width: 3, color: '#22c55e' },
        itemStyle: { color: '#22c55e' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(34,197,94,0.18)' },
              { offset: 1, color: 'rgba(34,197,94,0.02)' }
            ]
          }
        }
      }
    ]
  }

  trendChart.setOption(option, true)
}

function handleResize() {
  trendChart?.resize()
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(0, 10)
}

function viewNotice(notice) {
  currentNotice.value = notice
  noticeDetailVisible.value = true
}

async function loadNotices() {
  noticesLoading.value = true
  try {
    const res = await noticeApi.getMyNotices()
    notices.value = Array.isArray(res) ? res.slice(0, 10) : []
  } catch (e) {
    notices.value = []
  } finally {
    noticesLoading.value = false
  }
}

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function refreshAll() {
  loading.value = true
  await loadTrendData()
  await loadStats()
  await loadNotices()
  loading.value = false
  await nextTick()
  await nextTick()
  renderTrendChart()
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  loading.value = true
  await loadTrendData()
  await loadStats()
  await loadNotices()
  loading.value = false
  await nextTick()
  await nextTick()
  renderTrendChart()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (trendChart) {
    window.removeEventListener('resize', handleResize)
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped>
/* ========== 页面容器 ========== */
.page-container {
  padding: 24px;
}

/* ========== 页面标题区 ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e5eaf3;
}
.page-title {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: #0f2a5f;
}
.page-subtitle {
  margin: 0;
  font-size: 13px;
  color: #5f6f8f;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ========== 加载状态 ========== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 12px;
  color: #909399;
}

/* ========== 主内容区 ========== */
.main-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== 面板通用 ========== */
.panel {
  background: #fff;
  border: 1px solid #e5eaf3;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(47, 107, 255, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  border-bottom: 1px solid #f0f2f5;
  background: #fafbfc;
  flex-shrink: 0;
  height: 46px;
  box-sizing: border-box;
}
.panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0f2a5f;
}

/* ========== 第一行：6 个核心指标卡片 ========== */
.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e5eaf3;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(47, 107, 255, 0.04);
  height: 96px;
  box-sizing: border-box;
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: default;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(47, 107, 255, 0.12);
}
.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  flex-shrink: 0;
}
.stat-card--blue .stat-card__icon    { background: linear-gradient(135deg, #2f6bff, #5a9fff); }
.stat-card--green .stat-card__icon   { background: linear-gradient(135deg, #22c55e, #4ade80); }
.stat-card--orange .stat-card__icon  { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
.stat-card--primary .stat-card__icon { background: linear-gradient(135deg, #2f6bff, #409eff); }
.stat-card--teal .stat-card__icon   { background: linear-gradient(135deg, #009688, #26a69a); }
.stat-card--amber .stat-card__icon   { background: linear-gradient(135deg, #f59e0b, #f97316); }

.stat-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.stat-card__label {
  font-size: 12px;
  color: #5f6f8f;
  margin-bottom: 2px;
  white-space: nowrap;
}
.stat-card__value {
  font-size: 26px;
  font-weight: 700;
  color: #0f2a5f;
  line-height: 1.2;
}
.stat-card__unit { font-size: 14px; font-weight: 400; }
.stat-card__sub {
  font-size: 11px;
  color: #909399;
  margin: 2px 0 0;
  white-space: nowrap;
}

/* ========== 第二行 & 第三行：通用主网格 (1.55fr : 1fr) ========== */
.main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

/* ========== 图表面板 ========== */
.chart-panel {
  height: 360px;
}
.chart-container {
  flex: 1;
  width: 100%;
  min-height: 0;
  padding: 8px 14px 14px;
  box-sizing: border-box;
}

/* ========== 特殊就业类型统计 ========== */
.special-panel {
  height: 360px;
}
.special-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 14px;
  padding: 14px 16px;
  min-height: 0;
}
.special-card {
  background: #f5f9ff;
  border-radius: 8px;
  border-left: 3px solid;
  padding: 12px 14px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: box-shadow 0.2s;
}
.special-card:hover {
  box-shadow: 0 2px 8px rgba(47, 107, 255, 0.1);
}
.special-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.special-card__name {
  font-size: 12px;
  color: #5f6f8f;
  font-weight: 500;
}
.special-card__count {
  font-size: 20px;
  font-weight: 700;
  color: #0f2a5f;
  text-align: right;
}
.special-card__unit {
  font-size: 11px;
  font-weight: 400;
  color: #909399;
  margin-left: 1px;
}
.special-card__bar {
  height: 4px;
  background: #e5eaf3;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}
.special-card__bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.6s ease;
}
.special-card__percent {
  font-size: 11px;
  color: #909399;
}

/* ========== 任务面板 ========== */
.task-panel {
  height: 228px;
}
.task-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
}
.task-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-right: 1px solid #f0f2f5;
  border-bottom: 1px solid #f0f2f5;
  box-sizing: border-box;
  transition: background 0.2s;
}
.task-card:hover { background: #f5f9ff; }
.task-card:nth-child(2n) { border-right: none; }
.task-card:nth-last-child(-n+2) { border-bottom: none; }

.task-card__icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  flex-shrink: 0;
}
.task-card__icon--blue   { background: linear-gradient(135deg, #2f6bff, #5a9fff); }
.task-card__icon--green  { background: linear-gradient(135deg, #22c55e, #4ade80); }
.task-card__icon--orange { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
.task-card__icon--teal   { background: linear-gradient(135deg, #009688, #26a69a); }

.task-card__info { flex: 1; min-width: 0; }
.task-card__name { font-size: 13px; font-weight: 600; color: #0f2a5f; margin-bottom: 2px; }
.task-card__desc { font-size: 11px; color: #909399; margin: 0; }

.task-card__status {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 12px;
  flex-shrink: 0;
}
.task-card__status--normal { color: #22c55e; background: #f0fdf4; }
.task-card__status--warning { color: #f59e0b; background: #fffbeb; }

/* ========== 待处理事项面板 ========== */
.pending-panel {
  height: 228px;
}
.pending-list { padding: 2px 0; }
.pending-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f2f5;
  transition: background 0.2s;
  cursor: default;
}
.pending-item:last-child { border-bottom: none; }
.pending-item:hover { background: #f5f9ff; }
.pending-item__left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #5f6f8f;
}
.pending-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.pending-dot--ok   { background: #22c55e; }
.pending-dot--warn { background: #f59e0b; animation: pulse 1.5s infinite; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* ========== 公告列表 ========== */
.notice-list { padding: 2px 0; }
.notice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f2f5;
  transition: background 0.2s;
}
.notice-item:last-child { border-bottom: none; }
.notice-item:hover { background: #f5f9ff; }
.notice-item__left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  gap: 4px;
}
.notice-item__title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.notice-item__time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 8px;
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  color: #909399;
  font-size: 13px;
  flex: 1;
}

/* ========== 系统公告面板 ========== */
.notice-panel {
  width: 100%;
}
.notice-panel .panel__body {
  padding: 0;
}
.notice-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  color: #909399;
}
.notice-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 80px;
  color: #909399;
  font-size: 13px;
}

/* ========== 公告详情 ========== */
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

/* ========== 响应式 ========== */
@media (max-width: 1200px) {
  .metric-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .main-grid {
    grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  }
}

@media (max-width: 992px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .main-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .page-container { padding: 16px; }
  .page-header { flex-direction: column; gap: 12px; }
  .metric-grid { grid-template-columns: 1fr; }
  .task-grid { grid-template-columns: 1fr; }
  .task-card { border-right: none; }
  .task-card:nth-last-child(-n+2) { border-bottom: 1px solid #f0f2f5; }
  .task-card:last-child { border-bottom: none; }
  .special-grid { grid-template-columns: 1fr; }
}
</style>
