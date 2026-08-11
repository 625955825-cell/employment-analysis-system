<template>
  <div class="page-container">
    <h2>校级管理首页</h2>
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#409eff;"><el-icon><User /></el-icon></div>
            <div class="stat-info">
              <p class="stat-label">用户总数</p>
              <p class="stat-value">{{ stats.totalUsers }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#67c23a;"><el-icon><School /></el-icon></div>
            <div class="stat-info">
              <p class="stat-label">入驻企业</p>
              <p class="stat-value">{{ stats.totalCompanies }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#e6a23c;"><el-icon><Tickets /></el-icon></div>
            <div class="stat-info">
              <p class="stat-label">在招职位</p>
              <p class="stat-value">{{ stats.totalJobs }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#f56c6c;"><el-icon><DataLine /></el-icon></div>
            <div class="stat-info">
              <p class="stat-label">就业率</p>
              <p class="stat-value">{{ stats.employmentRate }}%</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#909399;"><el-icon><OfficeBuilding /></el-icon></div>
            <div class="stat-info">
              <p class="stat-label">学生总数</p>
              <p class="stat-value">{{ stats.totalStudents }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#009688;"><el-icon><TrendCharts /></el-icon></div>
            <div class="stat-info">
              <p class="stat-label">已就业人数</p>
              <p class="stat-value">{{ stats.totalEmployment }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#8e44ad;"><el-icon><List /></el-icon></div>
            <div class="stat-info">
              <p class="stat-label">投递记录</p>
              <p class="stat-value">{{ stats.applicationCount }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header><span>就业类型分布</span></template>
          <div ref="employmentTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>企业行业分布</span></template>
          <div ref="industryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header><span>各院系学生分布</span></template>
          <div ref="deptChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>就业地区分布（Top15）</span></template>
          <div ref="provinceChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统公告 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>系统公告</span>
              <el-button type="primary" size="small" @click="$router.push('/admin/notices')">管理公告</el-button>
            </div>
          </template>
          <div v-if="noticesLoading" style="text-align: center; padding: 20px;">
            <el-icon class="is-loading" :size="20"><Loading /></el-icon>
          </div>
          <div v-else-if="notices.length === 0" style="text-align: center; padding: 20px; color: #999;">
            暂无公告
          </div>
          <div v-else class="notice-list">
            <div
              v-for="notice in notices.slice(0, 5)"
              :key="notice.id"
              class="notice-item"
              @click="viewNotice(notice)"
            >
              <el-tag v-if="notice.topStatus === '1'" type="danger" size="small" effect="plain" style="margin-right: 6px;">置顶</el-tag>
              <span class="notice-title">{{ notice.title }}</span>
              <span class="notice-time">{{ formatTime(notice.publishTime) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { User, School, Tickets, DataLine, OfficeBuilding, TrendCharts, List, Loading } from '@element-plus/icons-vue'
import { adminApi, noticeApi } from '@/api'

const stats = reactive({
  totalUsers: 0,
  totalCompanies: 0,
  totalJobs: 0,
  employmentRate: 0,
  totalStudents: 0,
  totalEmployment: 0,
  applicationCount: 0
})

const notices = ref([])
const noticesLoading = ref(false)
const noticeDetailVisible = ref(false)
const currentNotice = ref(null)

// 图表
const employmentTypeChartRef = ref(null)
const industryChartRef = ref(null)
const deptChartRef = ref(null)
const provinceChartRef = ref(null)

let employmentTypeChart = null
let industryChart = null
let deptChart = null
let provinceChart = null

const COLOR_PALETTE = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#009688']

function initCharts() {
  employmentTypeChart = echarts.init(employmentTypeChartRef.value)
  industryChart = echarts.init(industryChartRef.value)
  deptChart = echarts.init(deptChartRef.value)
  provinceChart = echarts.init(provinceChartRef.value)
}

function destroyCharts() {
  employmentTypeChart?.dispose()
  industryChart?.dispose()
  deptChart?.dispose()
  provinceChart?.dispose()
}

function renderCharts(data) {
  // 就业类型饼图
  const typeItems = Object.entries(data.employmentTypeDistribution || {})
    .map(([name, value]) => ({ name, value }))
  employmentTypeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    color: COLOR_PALETTE,
    series: [{
      type: 'pie', radius: ['40%', '70%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: typeItems.length <= 6 }, data: typeItems
    }]
  })

  // 企业行业分布饼图
  const industryData = data.companyIndustryDistribution || []
  industryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    color: COLOR_PALETTE,
    series: [{
      type: 'pie', radius: ['40%', '70%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: industryData.length <= 6 }, data: industryData
    }]
  })

  // 各院系学生分布柱状图
  const deptData = data.studentDeptDistribution || []
  deptChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: deptData.map(d => d.name), axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', name: '人数' },
    color: ['#5470c6'],
    series: [{ type: 'bar', data: deptData.map(d => d.value), barMaxWidth: 40,
      itemStyle: { borderRadius: [4, 4, 0, 0] } }]
  })

  // 就业地区分布柱状图
  const provinceData = (data.employmentProvinceDistribution || []).slice(0, 15)
  provinceChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value', name: '人数' },
    yAxis: { type: 'category', data: provinceData.map(d => d.name).reverse(), axisLabel: { fontSize: 11 } },
    color: ['#67c23a'],
    series: [{ type: 'bar', data: provinceData.map(d => d.value).reverse(), barMaxWidth: 30,
      itemStyle: { borderRadius: [0, 4, 4, 0] } }]
  })
}

function loadStats() {
  adminApi.stats.overview().then(res => {
    if (res && typeof res === 'object') Object.assign(stats, res)
  }).catch(() => {})
  adminApi.stats.summary().then(res => {
    if (res && typeof res === 'object') {
      stats.applicationCount = res.applicationCount || 0
    }
  }).catch(() => {})
}

function loadDashboard() {
  adminApi.stats.dashboard().then(res => {
    if (res && typeof res === 'object') renderCharts(res)
  }).catch(() => {})
}

function loadNotices() {
  noticesLoading.value = true
  noticeApi.getMyNotices().then(res => {
    notices.value = Array.isArray(res) ? res.slice(0, 10) : []
  }).catch(() => { notices.value = [] })
    .finally(() => { noticesLoading.value = false })
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(0, 10)
}

function viewNotice(notice) {
  currentNotice.value = notice
  noticeDetailVisible.value = true
}

let resizeObserver = null

onMounted(() => {
  initCharts()
  loadStats()
  loadDashboard()
  loadNotices()
  resizeObserver = new ResizeObserver(() => {
    employmentTypeChart?.resize()
    industryChart?.resize()
    deptChart?.resize()
    provinceChart?.resize()
  })
  resizeObserver.observe(employmentTypeChartRef.value)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  destroyCharts()
})
</script>

<style scoped>
.stats-row { margin-top: 20px; }
.chart-row { margin-top: 20px; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 28px; color: #fff; }
.stat-info { flex: 1; }
.stat-label { font-size: 14px; color: #666; margin-bottom: 8px; }
.stat-value { font-size: 24px; font-weight: 600; color: #333; }
.chart-container { height: 280px; width: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.notice-list { display: flex; flex-direction: column; gap: 8px; }
.notice-item {
  display: flex; align-items: center; padding: 8px 4px; cursor: pointer;
  border-bottom: 1px solid #f0f0f0; transition: background 0.2s; gap: 6px;
}
.notice-item:last-child { border-bottom: none; }
.notice-item:hover { background: #f5f7fa; border-radius: 4px; }
.notice-title { flex: 1; font-size: 13px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notice-time { font-size: 12px; color: #999; flex-shrink: 0; }
.notice-meta { display: flex; align-items: center; gap: 12px; color: #909399; font-size: 13px; }
.notice-body { font-size: 14px; line-height: 1.8; color: #606266; white-space: pre-wrap; }
</style>
