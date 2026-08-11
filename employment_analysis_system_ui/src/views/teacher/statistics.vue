<template>
  <div class="page-container">
    <div class="page-header">
      <h2>就业统计</h2>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner blue">
            <el-icon class="stat-icon"><User /></el-icon>
            <div class="stat-text">
              <p class="stat-label">班级总人数</p>
              <p class="stat-value">{{ stats.total || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner green">
            <el-icon class="stat-icon"><CircleCheck /></el-icon>
            <div class="stat-text">
              <p class="stat-label">已就业人数</p>
              <p class="stat-value">{{ stats.employed || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner orange">
            <el-icon class="stat-icon"><Warning /></el-icon>
            <div class="stat-text">
              <p class="stat-label">未就业人数</p>
              <p class="stat-value">{{ stats.unemployed || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner purple">
            <el-icon class="stat-icon"><TrendCharts /></el-icon>
            <div class="stat-text">
              <p class="stat-label">就业率</p>
              <p class="stat-value">{{ stats.employmentRate || '0.0' }}%</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner teal">
            <el-icon class="stat-icon"><Reading /></el-icon>
            <div class="stat-text">
              <p class="stat-label">升学深造</p>
              <p class="stat-value">{{ (stats.specialTypes || {})['继续深造'] || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner red">
            <el-icon class="stat-icon"><Medal /></el-icon>
            <div class="stat-text">
              <p class="stat-label">应征入伍</p>
              <p class="stat-value">{{ (stats.specialTypes || {})['应征入伍'] || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner yellow">
            <el-icon class="stat-icon"><Shop /></el-icon>
            <div class="stat-text">
              <p class="stat-label">自主创业</p>
              <p class="stat-value">{{ (stats.specialTypes || {})['自主创业'] || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner cyan">
            <el-icon class="stat-icon"><Promotion /></el-icon>
            <div class="stat-text">
              <p class="stat-label">出国出境</p>
              <p class="stat-value">{{ (stats.specialTypes || {})['出国出境'] || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>就业类型分布</span>
            </div>
          </template>
          <div ref="employmentTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>行业分布 TOP10</span>
            </div>
          </template>
          <div ref="industryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>薪资分布</span>
            </div>
          </template>
          <div ref="salaryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>城市分布 TOP10</span>
            </div>
          </template>
          <div ref="cityChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>就业地区分布</span>
            </div>
          </template>
          <div ref="mapChartRef" class="map-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>学生就业明细表</span>
              <el-button type="primary" size="small" @click="exportTable">
                <el-icon><Download /></el-icon> 导出 Excel
              </el-button>
            </div>
          </template>
          <el-table :data="detailList" stripe border v-loading="detailLoading" max-height="400">
            <el-table-column prop="studentNo" label="学号" width="130" fixed />
            <el-table-column prop="realName" label="姓名" width="100" fixed />
            <el-table-column prop="gender" label="性别" width="60" />
            <el-table-column prop="employmentType" label="就业类型" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.employmentType" type="success" size="small">{{ row.employmentType }}</el-tag>
                <el-tag v-else type="info" size="small">未就业</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="companyName" label="就业单位" min-width="180" show-overflow-tooltip />
            <el-table-column prop="workProvince" label="省份" width="100" />
            <el-table-column prop="workCity" label="城市" width="100" />
            <el-table-column prop="positionName" label="岗位" width="120" show-overflow-tooltip />
            <el-table-column prop="industry" label="行业" width="130" show-overflow-tooltip />
            <el-table-column prop="salary" label="薪资" width="100">
              <template #default="{ row }">
                <span v-if="row.salary" style="color:#67c23a;font-weight:600;">{{ row.salary }}</span>
                <span v-else style="color:#999;">--</span>
              </template>
            </el-table-column>
            <el-table-column prop="auditStatus" label="状态" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.auditStatus === 'approved'" type="success" size="small">已审核</el-tag>
                <el-tag v-else-if="row.auditStatus === 'pending'" type="warning" size="small">待审核</el-tag>
                <el-tag v-else type="info" size="small">未就业</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="detailTotal > 0"
            v-model:current-page="detailPage"
            :page-size="detailSize"
            :total="detailTotal"
            layout="total, prev, pager, next"
            style="margin-top:12px;justify-content:center;"
            @current-change="loadDetail"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { User, CircleCheck, Warning, TrendCharts, Download, Reading, Medal, Shop, Promotion } from '@element-plus/icons-vue'
import { teacherApi } from '@/api'

const loading = ref(false)
const detailLoading = ref(false)
const stats = ref({})
const detailList = ref([])
const detailTotal = ref(0)
const detailPage = ref(1)
const detailSize = ref(20)

const employmentTypeChartRef = ref(null)
const industryChartRef = ref(null)
const salaryChartRef = ref(null)
const cityChartRef = ref(null)
const mapChartRef = ref(null)

let employmentTypeChart = null
let industryChart = null
let salaryChart = null
let cityChart = null
let mapChart = null
let resizeObserver = null

const COLOR_PALETTE = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#009688'
]

const PROVINCE_NAME_MAP = {
  '北京': '北京市', '天津': '天津市', '上海': '上海市', '重庆': '重庆市',
  '河北': '河北省', '山西': '山西省', '辽宁': '辽宁省', '吉林': '吉林省',
  '黑龙江': '黑龙江省', '江苏': '江苏省', '浙江': '浙江省', '安徽': '安徽省',
  '福建': '福建省', '江西': '江西省', '山东': '山东省', '河南': '河南省',
  '湖北': '湖北省', '湖南': '湖南省', '广东': '广东省', '海南': '海南省',
  '四川': '四川省', '贵州': '贵州省', '云南': '云南省', '陕西': '陕西省',
  '甘肃': '甘肃省', '青海': '青海省', '内蒙古': '内蒙古自治区',
  '广西': '广西壮族自治区', '西藏': '西藏自治区', '宁夏': '宁夏回族自治区',
  '新疆': '新疆维吾尔自治区', '台湾': '台湾省', '香港': '香港特别行政区', '澳门': '澳门特别行政区'
}

function normalizeProvince(name) {
  if (!name) return null
  const trimmed = name.trim()
  if (PROVINCE_NAME_MAP[trimmed]) return PROVINCE_NAME_MAP[trimmed]
  if (trimmed.endsWith('省') || trimmed.endsWith('市') || trimmed.endsWith('自治区') ||
      trimmed.endsWith('特别行政区')) return trimmed
  return PROVINCE_NAME_MAP[trimmed + '省'] || trimmed
}

function initCharts() {
  employmentTypeChart = echarts.init(employmentTypeChartRef.value)
  industryChart = echarts.init(industryChartRef.value)
  salaryChart = echarts.init(salaryChartRef.value)
  cityChart = echarts.init(cityChartRef.value)
  mapChart = echarts.init(mapChartRef.value)
}

function destroyCharts() {
  employmentTypeChart?.dispose()
  industryChart?.dispose()
  salaryChart?.dispose()
  cityChart?.dispose()
  mapChart?.dispose()
}

function renderEmploymentTypeChart(data) {
  const typeData = data.employmentTypeDistribution || {}
  const items = Object.entries(typeData).map(([name, value]) => ({ name, value }))
  employmentTypeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 12 } },
    color: COLOR_PALETTE,
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: items.length <= 6, fontSize: 12 },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: items
    }]
  }, true)
}

function renderIndustryChart(data) {
  const raw = data.industryDistribution || {}
  const entries = Object.entries(raw).sort((a, b) => b[1] - a[1]).slice(0, 10)
  industryChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value', name: '人数', axisLabel: { fontSize: 11 } },
    yAxis: { type: 'category', data: entries.map(e => e[0]).reverse(), axisLabel: { fontSize: 11 } },
    color: ['#5470c6'],
    series: [{
      type: 'bar',
      data: entries.map(e => e[1]).reverse(),
      barMaxWidth: 30,
      itemStyle: { borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', fontSize: 11 }
    }]
  }, true)
}

function renderSalaryChart(data) {
  const salaryData = data.salaryDistribution || {}
  const labels = ['5k以下', '5k-8k', '8k-12k', '12k-20k', '20k以上']
  const values = labels.map(l => salaryData[l] || 0)
  salaryChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 }, name: '薪资范围' },
    yAxis: { type: 'value', name: '人数', axisLabel: { fontSize: 11 } },
    color: ['#73c0de'],
    series: [{
      type: 'bar',
      data: values,
      barMaxWidth: 40,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', fontSize: 11 },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(115, 192, 222, 0.5)' } }
    }]
  }, true)
}

function renderCityChart(data) {
  const cityData = data.cityDistribution || {}
  const entries = Object.entries(cityData).sort((a, b) => b[1] - a[1]).slice(0, 10)
  cityChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value', name: '人数', axisLabel: { fontSize: 11 } },
    yAxis: { type: 'category', data: entries.map(e => e[0]).reverse(), axisLabel: { fontSize: 11 } },
    color: ['#fac858'],
    series: [{
      type: 'bar',
      data: entries.map(e => e[1]).reverse(),
      barMaxWidth: 30,
      itemStyle: { borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', fontSize: 11 }
    }]
  }, true)
}

async function renderMapChart(data) {
  const provinceData = data.provinceDistribution || {}
  const mapData = Object.entries(provinceData).map(([name, value]) => ({
    name: normalizeProvince(name),
    value
  })).filter(d => d.name !== null)

  try {
    const response = await fetch('/china.json')
    const chinaJson = await response.json()
    echarts.registerMap('china', chinaJson)

    const maxVal = mapData.length > 0 ? Math.max(...mapData.map(d => d.value)) : 1

    mapChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (params) => {
          const value = params.data?.value ?? 0
          return `${params.name}<br/>就业人数: <b>${value}</b> 人`
        }
      },
      visualMap: {
        min: 0,
        max: maxVal,
        text: ['高', '低'],
        realtime: false,
        calculable: true,
        inRange: { color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#f46d43', '#d73027', '#a50026'] },
        textStyle: { fontSize: 12 }
      },
      series: [{
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
        data: mapData
      }]
    }, true)
  } catch (e) {
    console.warn('地图加载失败:', e)
  }
}

function renderAllCharts(data) {
  renderEmploymentTypeChart(data)
  renderIndustryChart(data)
  renderSalaryChart(data)
  renderCityChart(data)
  renderMapChart(data)
}

async function loadData() {
  loading.value = true
  try {
    const res = await teacherApi.getEmploymentStats()
    if (res && typeof res === 'object') {
      stats.value = res
      await nextTick()
      renderAllCharts(res)
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  } finally {
    loading.value = false
  }
}

async function loadDetail(page = 1) {
  detailLoading.value = true
  detailPage.value = page
  try {
    const res = await teacherApi.getEmploymentDetail(null, page, detailSize.value)
    if (res && typeof res === 'object') {
      detailList.value = res.records || []
      detailTotal.value = res.total || 0
    }
  } catch (e) {
    console.error('加载明细数据失败', e)
  } finally {
    detailLoading.value = false
  }
}

async function exportTable() {
  const headers = ['学号', '姓名', '性别', '就业类型', '就业单位', '省份', '城市', '岗位', '行业', '薪资', '状态']
  const keys = ['studentNo', 'realName', 'gender', 'employmentType', 'companyName', 'workProvince', 'workCity', 'positionName', 'industry', 'salary', 'auditStatus']
  const statusMap = { 'approved': '已审核', 'pending': '待审核', 'unemployed': '未就业' }

  const res = await teacherApi.getEmploymentDetailAll(null)
  const allRecords = res?.records || []

  const rows = allRecords.map(row => {
    const r = {}
    keys.forEach((key, i) => {
      let val = row[key] ?? ''
      if (key === 'auditStatus') val = statusMap[val] || val
      r[headers[i]] = val
    })
    return r
  })

  const csvContent = [headers, ...rows.map(r => headers.map(h => `"${(r[h] || '').toString().replace(/"/g, '""')}"`))]
    .map(line => line.join(',')).join('\n')

  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `就业统计明细_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  initCharts()
  loadData()
  loadDetail()
  resizeObserver = new ResizeObserver(() => {
    employmentTypeChart?.resize()
    industryChart?.resize()
    salaryChart?.resize()
    cityChart?.resize()
    mapChart?.resize()
  })
  resizeObserver.observe(mapChartRef.value)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  destroyCharts()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.stats-row { margin-bottom: 12px; }
.stat-inner { display: flex; align-items: center; gap: 14px; }
.stat-icon { font-size: 32px; color: #fff; }
.stat-text { flex: 1; }
.stat-label { font-size: 13px; color: #606266; margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: 700; line-height: 1; }
.blue .stat-icon { color: #409eff; }
.green .stat-icon { color: #67c23a; }
.orange .stat-icon { color: #e6a23c; }
.purple .stat-icon { color: #9c27b0; }
.teal .stat-icon { color: #009688; }
.red .stat-icon { color: #f56c6c; }
.yellow .stat-icon { color: #e6a23c; }
.cyan .stat-icon { color: #0db8ac; }
.chart-row { margin-bottom: 16px; }
.chart-container { height: 280px; width: 100%; }
.map-container { height: 480px; width: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-size: 14px; font-weight: 600; color: #303133; }
</style>
