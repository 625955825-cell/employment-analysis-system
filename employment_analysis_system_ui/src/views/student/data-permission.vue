<template>
  <div class="page-container">
    <h2>就业数据查看申请</h2>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card style="margin-top: 16px;">
          <template #header>
            <div class="card-header">
              <span>提交申请</span>
            </div>
          </template>
          <el-alert title="说明" type="info" :closable="false" style="margin-bottom: 16px;">
            <template #default>
              就业数据包含各学院、各专业的就业率、薪资分布、行业分布等信息。请填写申请理由，经审批通过后即可查看详细数据。
            </template>
          </el-alert>
          <el-form label-width="110px" :model="form" :rules="rules" ref="formRef">
            <el-form-item label="选择学院" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择学院" style="width: 100%;" clearable>
                <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="申请类型" prop="requestType">
              <el-select v-model="form.requestType" placeholder="请选择申请类型" style="width: 100%;">
                <el-option label="就业率统计" value="就业率统计" />
                <el-option label="薪资分布" value="薪资分布" />
                <el-option label="行业分布" value="行业分布" />
                <el-option label="全部数据" value="全部数据" />
              </el-select>
            </el-form-item>
            <el-form-item label="年份范围">
              <el-select v-model="form.yearFrom" placeholder="起始届" style="width: 45%;" clearable>
                <el-option v-for="y in availableYears" :key="y" :label="y + '届'" :value="y" />
              </el-select>
              <span style="margin: 0 10px;">至</span>
              <el-select v-model="form.yearTo" placeholder="结束届" style="width: 45%;" clearable>
                <el-option v-for="y in availableYears" :key="y" :label="y + '届'" :value="y" />
              </el-select>
            </el-form-item>
            <el-form-item label="申请理由" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请详细说明申请查看数据的目的和用途" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleApply" :loading="submitting">提交申请</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card style="margin-top: 16px;">
          <template #header>
            <div class="card-header">
              <span>申请记录</span>
              <el-button text size="small" @click="loadRequests">刷新</el-button>
            </div>
          </template>
          <div v-if="loading" class="loading-state">
            <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          </div>
          <div v-else-if="myRequests.length === 0" class="empty-state">
            <el-empty description="暂无申请记录" :image-size="60" />
          </div>
          <div v-else class="request-list">
            <el-timeline>
              <el-timeline-item
                v-for="req in myRequests"
                :key="req.id"
                :color="statusColor(req.status)"
                :timestamp="formatTime(req.createTime)"
                placement="top"
              >
                <el-card shadow="hover" class="request-card">
                  <div class="request-type">{{ req.requestType || '数据查看申请' }}</div>
                  <div class="request-dept" v-if="req.deptName">学院：{{ req.deptName }}</div>
                  <div class="request-years" v-if="req.yearFrom || req.yearTo">
                    {{ req.yearFrom || '-' }}届 - {{ req.yearTo || '-' }}届
                  </div>
                  <div class="request-reason" v-if="req.reason">{{ req.reason }}</div>
                  <div class="request-actions">
                    <el-tag :type="statusType(req.status)" size="small">{{ statusText(req.status) }}</el-tag>
                    <el-button
                      v-if="req.status === 'approved'"
                      type="primary"
                      size="small"
                      @click="viewData(req)"
                    >查看数据</el-button>
                    <el-button
                      v-if="req.status === 'pending'"
                      type="danger"
                      size="small"
                      text
                      @click="handleCancel(req)"
                    >取消申请</el-button>
                  </div>
                  <div class="request-remark" v-if="req.auditRemark">
                    <strong>审核备注：</strong>{{ req.auditRemark }}
                  </div>
                  <div class="request-time" v-if="req.auditTime">
                    审核时间：{{ req.auditTime }}
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据查看弹窗 -->
    <el-dialog v-model="dataDialogVisible" :title="dataDialogTitle" width="900px" @opened="onDialogOpened">
      <div v-if="dataLoading" class="loading-state">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <span>正在加载数据...</span>
      </div>
      <div v-else-if="approvedData">
        <!-- 核心统计卡片 -->
        <el-row :gutter="12" style="margin-bottom: 12px;">
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card blue">
              <div class="stat-label">学生总数</div>
              <div class="stat-value">{{ approvedData.totalStudents || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card green">
              <div class="stat-label">已就业</div>
              <div class="stat-value">{{ approvedData.employed || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card orange">
              <div class="stat-label">未就业</div>
              <div class="stat-value">{{ approvedData.unemployed || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card purple">
              <div class="stat-label">就业率</div>
              <div class="stat-value" style="color:#9c27b0;">{{ approvedData.employmentRate || '0.0' }}%</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 特殊就业类型 -->
        <el-row :gutter="12" style="margin-bottom: 12px;">
          <el-col :span="6" v-for="(count, label) in approvedData.specialTypes" :key="label">
            <el-card shadow="hover" class="stat-card teal">
              <div class="stat-label">{{ label }}</div>
              <div class="stat-value">{{ count || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 图表 -->
        <el-row :gutter="12" style="margin-bottom: 12px;">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span>薪资分布</span></template>
              <div ref="salaryChartRef" class="chart-container-sm"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span>行业分布 TOP10</span></template>
              <div ref="industryChartRef" class="chart-container-sm"></div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 省份分布地图 -->
        <el-card shadow="hover">
          <template #header>
            <span>就业地区分布 TOP15</span>
          </template>
          <div ref="provinceChartRef" class="chart-container-md"></div>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { dataPermissionApi, dictApi, analyticsApi } from '@/api'

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

const submitting = ref(false)
const loading = ref(false)
const dataLoading = ref(false)
const myRequests = ref([])
const depts = ref([])
const formRef = ref(null)
const approvedData = ref(null)
const dataDialogVisible = ref(false)
const dataDialogTitle = ref('就业数据查看')

const salaryChartRef = ref(null)
const industryChartRef = ref(null)
const provinceChartRef = ref(null)
let salaryChart = null
let industryChart = null
let provinceChart = null

const availableYears = ref([])

const form = reactive({
  deptId: null,
  requestType: '全部数据',
  yearFrom: null,
  yearTo: null,
  reason: ''
})

const rules = {
  requestType: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  reason: [{ required: true, message: '请填写申请理由', trigger: 'blur' }]
}

function statusColor(status) {
  const map = { pending: '#e6a23c', approved: '#67c23a', rejected: '#f56c6c' }
  return map[status] || '#909399'
}

function statusType(status) {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

function statusText(status) {
  const map = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status || '-'
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(0, 10)
}

function loadYears() {
  analyticsApi.getAvailableYears().then(res => {
    availableYears.value = Array.isArray(res) ? res : []
  }).catch(() => {
    availableYears.value = []
  })
}

function loadDepts() {
  dictApi.getDepartments().then(res => {
    depts.value = res || []
  }).catch(() => {})
}

function loadRequests() {
  loading.value = true
  dataPermissionApi.getMyRequests().then(res => {
    myRequests.value = Array.isArray(res) ? res : []
  }).catch(() => {
    myRequests.value = []
  }).finally(() => {
    loading.value = false
  })
}

function handleApply() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    dataPermissionApi.apply({
      deptId: form.deptId,
      requestType: form.requestType,
      yearFrom: form.yearFrom,
      yearTo: form.yearTo,
      reason: form.reason
    }).then(() => {
      ElMessage.success('申请提交成功，等待审批')
      form.reason = ''
      form.deptId = null
      loadRequests()
    }).catch(err => {
      ElMessage.error(err.message || '提交失败')
    }).finally(() => {
      submitting.value = false
    })
  })
}

function handleCancel(req) {
  ElMessageBox.confirm('确定要取消该申请吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    dataPermissionApi.cancelRequest(req.id).then(() => {
      ElMessage.success('已取消申请')
      loadRequests()
    }).catch(err => {
      ElMessage.error(err.message || '取消失败')
    })
  }).catch(() => {})
}

async function viewData(req) {
  dataDialogVisible.value = true
  dataDialogTitle.value = `就业数据查看 - ${req.deptName || '全校'} ${req.yearFrom || ''}届${req.yearTo ? '-' + req.yearTo + '届' : ''}`
  dataLoading.value = true
  approvedData.value = null
  destroyDataCharts()

  try {
    const res = await dataPermissionApi.getApprovedData(req.id)
    approvedData.value = res
  } catch (err) {
    ElMessage.error(err.message || '加载数据失败')
    approvedData.value = null
  } finally {
    dataLoading.value = false
  }
}

function onDialogOpened() {
  if (!approvedData.value) return
  renderSalaryAndIndustryCharts()
  renderMapChart()
}

function initDataCharts() {
  const refs = [
    { ref: salaryChartRef, chart: salaryChart, name: 'salaryChart' },
    { ref: industryChartRef, chart: industryChart, name: 'industryChart' },
    { ref: provinceChartRef, chart: provinceChart, name: 'provinceChart' }
  ]
  refs.forEach(({ ref, chart, name }) => {
    if (!chart && ref.value && ref.value.clientWidth > 0 && ref.value.clientHeight > 0) {
      if (name === 'salaryChart') salaryChart = echarts.init(ref.value)
      else if (name === 'industryChart') industryChart = echarts.init(ref.value)
      else if (name === 'provinceChart') provinceChart = echarts.init(ref.value)
    }
  })
}

function destroyDataCharts() {
  salaryChart?.dispose()
  industryChart?.dispose()
  provinceChart?.dispose()
  salaryChart = null
  industryChart = null
  provinceChart = null
}

function renderSalaryAndIndustryCharts() {
  if (!approvedData.value) return
  initDataCharts()
  if (!salaryChart || !industryChart) return

  // 薪资分布
  const salaryDist = approvedData.value.salaryDistribution || {}
  const salaryLabels = ['5k以下', '5k-8k', '8k-12k', '12k-20k', '20k以上']
  const salaryValues = salaryLabels.map(l => salaryDist[l] || 0)
  salaryChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: salaryLabels, axisLabel: { fontSize: 11 }, name: '薪资范围' },
    yAxis: { type: 'value', name: '人数', axisLabel: { fontSize: 11 } },
    color: ['#73c0de'],
    series: [{
      type: 'bar',
      data: salaryValues,
      barMaxWidth: 35,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', fontSize: 11 }
    }]
  }, true)

  // 行业分布 - 饼图
  const industryDist = approvedData.value.industryDistribution || []
  industryChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}人 ({d}%)'
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { fontSize: 11 }
    },
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#6600ff'],
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 'bold' }
      },
      data: industryDist.map(d => ({ name: d.name, value: d.value }))
    }]
  }, true)
}

async function renderMapChart() {
  if (!approvedData.value) return
  initDataCharts()
  if (!provinceChart) return

  const provinceDist = approvedData.value.provinceDistribution || []
  const mapData = (Array.isArray(provinceDist) ? provinceDist : []).map(d => ({
    name: normalizeProvince(d.name),
    value: d.value
  })).filter(d => d.name !== null)

  try {
    const response = await fetch('/china.json')
    const chinaJson = await response.json()
    echarts.registerMap('china', chinaJson)

    const maxVal = mapData.length > 0 ? Math.max(...mapData.map(d => d.value)) : 1

    provinceChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: params => `${params.name}: ${params.value || 0}人`
      },
      visualMap: {
        min: 0,
        max: maxVal,
        text: ['高', '低'],
        realtime: false,
        calculable: true,
        inRange: { color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#f46d43', '#d73027', '#a50026'] },
        left: 'left',
        bottom: 20,
        textStyle: { fontSize: 11 }
      },
      series: [{
        name: '就业人数',
        type: 'map',
        map: 'china',
        roam: true,
        zoom: 1.2,
        scaleLimit: { min: 0.8, max: 3 },
        center: [105, 36],
        label: { show: false, fontSize: 10 },
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

onMounted(() => {
  loadYears()
  loadDepts()
  loadRequests()
})
</script>

<style scoped>
.page-container { padding: 20px; }
h2 { margin: 0 0 8px; font-size: 18px; font-weight: 600; color: #303133; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 0; color: #909399; gap: 12px; font-size: 15px; }
.empty-state { display: flex; flex-direction: column; align-items: center; padding: 30px 0; }
.request-list { max-height: 500px; overflow-y: auto; }
.request-card { font-size: 13px; }
.request-type { font-weight: 600; color: #303133; margin-bottom: 4px; }
.request-dept { color: #409eff; font-size: 12px; margin-bottom: 2px; }
.request-years { color: #909399; font-size: 12px; margin-bottom: 4px; }
.request-reason { color: #606266; margin-bottom: 8px; font-size: 12px; }
.request-actions { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.request-remark { font-size: 12px; color: #f56c6c; margin-top: 4px; padding: 6px 8px; background: #fef0f0; border-radius: 4px; }
.request-time { font-size: 12px; color: #909399; margin-top: 4px; }

/* 数据查看弹窗 */
.stat-card { text-align: center; }
.stat-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
.stat-value { font-size: 22px; font-weight: 700; color: #303133; line-height: 1.2; }
.stat-card.blue .stat-value { color: #409eff; }
.stat-card.green .stat-value { color: #67c23a; }
.stat-card.orange .stat-value { color: #e6a23c; }
.stat-card.purple .stat-value { color: #9c27b0; }
.stat-card.teal .stat-value { color: #009688; }

.chart-container-sm { height: 220px; width: 100%; }
.chart-container-md { height: 280px; width: 100%; }
</style>
