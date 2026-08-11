<template>
  <div class="page-container">
    <div class="page-header">
      <h2>数据统计</h2>
    </div>

    <div v-if="authStatus === 'rejected'" style="margin-bottom: 16px;">
      <el-alert
        title="您的企业入驻申请已被驳回，暂时无法查看数据统计"
        type="error"
        show-icon
        :closable="false"
      />
      <el-button type="danger" plain style="margin-top: 10px;" @click="openReApplyDialog">
        <el-icon><RefreshRight /></el-icon> 重新申请入驻
      </el-button>
    </div>
    <el-alert
      v-else-if="authStatus === 'pending'"
      title="您的企业入驻申请正在审核中，暂时无法查看数据统计，请耐心等待"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 16px;"
    />

    <!-- 核心统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-inner primary">
            <el-icon class="stat-icon"><User /></el-icon>
            <div class="stat-text">
              <p class="stat-label">收到简历</p>
              <p class="stat-value">{{ stats.totalResumes || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-inner success">
            <el-icon class="stat-icon"><CircleCheck /></el-icon>
            <div class="stat-text">
              <p class="stat-label">已录用</p>
              <p class="stat-value">{{ (stats.offerCount || 0) + (stats.acceptedCount || 0) }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-inner info">
            <el-icon class="stat-icon"><Briefcase /></el-icon>
            <div class="stat-text">
              <p class="stat-label">在招职位</p>
              <p class="stat-value">{{ stats.activeJobCount || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 投递状态分布 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <span>投递状态分布</span>
      </template>
      <div class="chart-grid-2">
        <div ref="statusPieRef" class="chart-container"></div>
        <div class="status-breakdown">
          <div
            v-for="item in statusBreakdown"
            :key="item.label"
            class="breakdown-item"
          >
            <div class="breakdown-info">
              <span class="breakdown-dot" :style="{ background: item.color }"></span>
              <span class="breakdown-label">{{ item.label }}</span>
            </div>
            <div class="breakdown-value">
              <span class="breakdown-count">{{ item.value }}</span>
              <span class="breakdown-percent">({{ item.percent }}%)</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 投递趋势 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>投递进度漏斗</span>
          </template>
          <div class="funnel-container">
            <div
              v-for="(item, index) in funnelData"
              :key="item.label"
              class="funnel-item"
            >
              <div class="funnel-bar">
                <div
                  class="funnel-fill"
                  :style="{
                    width: (item.value / funnelMax * 100) + '%',
                    background: item.color,
                    opacity: 1 - index * 0.15
                  }"
                >
                  <span class="funnel-text">{{ item.value }}</span>
                </div>
              </div>
              <span class="funnel-label">{{ item.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>录用转化率</span>
          </template>
          <div class="conversion-container">
            <div class="conversion-main">
              <div class="conversion-circle" :style="conversionStyle">
                <span class="conversion-value">{{ conversionRate }}%</span>
                <span class="conversion-label">总转化率</span>
              </div>
            </div>
            <div class="conversion-detail">
              <div class="conversion-item">
                <span class="conv-label">收到简历</span>
                <span class="conv-value">{{ stats.totalResumes || 0 }}</span>
              </div>
              <div class="conversion-arrow">→</div>
              <div class="conversion-item">
                <span class="conv-label">进入面试</span>
                <span class="conv-value">{{ stats.interviewCount || 0 }}</span>
              </div>
              <div class="conversion-arrow">→</div>
              <div class="conversion-item">
                <span class="conv-label">发放Offer</span>
                <span class="conv-value">{{ stats.offerCount || 0 }}</span>
              </div>
              <div class="conversion-arrow">→</div>
              <div class="conversion-item">
                <span class="conv-label">最终录用</span>
                <span class="conv-value success">{{ stats.acceptedCount || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 投递数据明细表 -->
    <el-card shadow="hover">
      <template #header>
        <span>投递数据明细</span>
      </template>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-value" style="color:#e6a23c;">{{ stats.pendingCount || 0 }}</div>
          <div class="detail-label">待处理</div>
          <div class="detail-desc">等待HR查看简历</div>
        </div>
        <div class="detail-item">
          <div class="detail-value" style="color:#409eff;">{{ stats.reviewingCount || 0 }}</div>
          <div class="detail-label">已查看</div>
          <div class="detail-desc">简历已被查看</div>
        </div>
        <div class="detail-item">
          <div class="detail-value" style="color:#909399;">{{ stats.interviewCount || 0 }}</div>
          <div class="detail-label">面试中</div>
          <div class="detail-desc">已安排面试</div>
        </div>
        <div class="detail-item">
          <div class="detail-value" style="color:#67c23a;">{{ stats.offerCount || 0 }}</div>
          <div class="detail-label">已发Offer</div>
          <div class="detail-desc">已发放录用通知</div>
        </div>
        <div class="detail-item">
          <div class="detail-value" style="color:#f56c6c;">{{ stats.rejectedCount || 0 }}</div>
          <div class="detail-label">不合适</div>
          <div class="detail-desc">已婉拒候选人</div>
        </div>
        <div class="detail-item">
          <div class="detail-value" style="color:#67c23a;">{{ stats.acceptedCount || 0 }}</div>
          <div class="detail-label">已入职</div>
          <div class="detail-desc">候选人已接受Offer</div>
        </div>
      </div>
    </el-card>

    <!-- 重新申请入驻对话框 -->
    <el-dialog v-model="reApplyDialogVisible" title="重新申请入驻" width="700px" destroy-on-close :close-on-click-modal="false">
      <el-form :model="reApplyForm" label-width="110px" :rules="reApplyRules" ref="reApplyFormRef">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="企业名称" prop="companyName">
              <el-input v-model="reApplyForm.companyName" placeholder="请输入企业名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="reApplyForm.contactPerson" placeholder="请输入联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="reApplyForm.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="reApplyForm.contactEmail" placeholder="请输入联系邮箱（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在省份" prop="province">
              <el-input v-model="reApplyForm.province" placeholder="请输入所在省份" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在城市" prop="city">
              <el-input v-model="reApplyForm.city" placeholder="请输入所在城市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" prop="address">
              <el-input v-model="reApplyForm.address" placeholder="请输入详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属行业" prop="industry">
              <el-select v-model="reApplyForm.industry" placeholder="请选择" style="width:100%;">
                <el-option label="互联网/IT" value="互联网/IT" />
                <el-option label="金融" value="金融" />
                <el-option label="制造业" value="制造业" />
                <el-option label="房地产" value="房地产" />
                <el-option label="教育" value="教育" />
                <el-option label="医疗健康" value="医疗健康" />
                <el-option label="电子/半导体" value="电子/半导体" />
                <el-option label="通信" value="通信" />
                <el-option label="能源/化工" value="能源/化工" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业规模" prop="scale">
              <el-select v-model="reApplyForm.scale" placeholder="请选择" style="width:100%;">
                <el-option label="20人以下" value="20人以下" />
                <el-option label="20-99人" value="20-99人" />
                <el-option label="100-499人" value="100-499人" />
                <el-option label="500-999人" value="500-999人" />
                <el-option label="1000-4999人" value="1000-4999人" />
                <el-option label="5000人以上" value="5000人以上" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="企业性质" prop="nature">
              <el-select v-model="reApplyForm.nature" placeholder="请选择" style="width:100%;">
                <el-option label="民营企业" value="民营企业" />
                <el-option label="国有企业" value="国有企业" />
                <el-option label="外资企业" value="外资企业" />
                <el-option label="合资企业" value="合资企业" />
                <el-option label="上市公司" value="上市公司" />
                <el-option label="事业单位" value="事业单位" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="企业简介">
              <el-input v-model="reApplyForm.introduction" type="textarea" :rows="3" placeholder="请输入企业简介（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="reApplyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reApplyLoading" @click="submitReApply">重新提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { User, CircleCheck, Briefcase, RefreshRight } from '@element-plus/icons-vue'
import { companyApi } from '@/api'
import { ElMessage } from 'element-plus'

const stats = ref({})
const authStatus = ref('approved')
const statusPieRef = ref(null)
let pieChart = null
let resizeObserver = null

const reApplyDialogVisible = ref(false)
const reApplyFormRef = ref(null)
const reApplyLoading = ref(false)
const reApplyForm = reactive({
  companyName: '', contactPerson: '', contactPhone: '', contactEmail: '',
  province: '', city: '', address: '', industry: '', scale: '', nature: '', introduction: ''
})
const reApplyRules = {
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入所在省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入所在城市', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  industry: [{ required: true, message: '请选择所属行业', trigger: 'change' }],
  scale: [{ required: true, message: '请选择企业规模', trigger: 'change' }],
  nature: [{ required: true, message: '请选择企业性质', trigger: 'change' }]
}

const COLOR_PALETTE = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399', '#5470c6', '#fac858']

const statusBreakdown = computed(() => {
  const total = stats.value.totalResumes || 0
  const items = [
    { label: '待处理', value: stats.value.pendingCount || 0, color: '#f56c6c' },
    { label: '已查看', value: stats.value.reviewingCount || 0, color: '#e6a23c' },
    { label: '面试中', value: stats.value.interviewCount || 0, color: '#409eff' },
    { label: '已发Offer', value: stats.value.offerCount || 0, color: '#67c23a' },
    { label: '不合适', value: stats.value.rejectedCount || 0, color: '#909399' },
    { label: '已入职', value: stats.value.acceptedCount || 0, color: '#5470c6' }
  ]
  return items.map(item => ({
    ...item,
    percent: total > 0 ? Math.round(item.value / total * 100) : 0
  })).filter(i => i.value > 0)
})

const funnelData = computed(() => [
  { label: '收到简历', value: stats.value.totalResumes || 0, color: '#5470c6' },
  { label: '已查看', value: (stats.value.reviewingCount || 0) + (stats.value.interviewCount || 0) + (stats.value.offerCount || 0) + (stats.value.rejectedCount || 0) + (stats.value.acceptedCount || 0), color: '#409eff' },
  { label: '面试中', value: stats.value.interviewCount || 0, color: '#e6a23c' },
  { label: '发放Offer', value: stats.value.offerCount || 0, color: '#67c23a' },
  { label: '最终录用', value: (stats.value.offerCount || 0) + (stats.value.acceptedCount || 0), color: '#f56c6c' }
].filter(i => i.value > 0))

const funnelMax = computed(() => {
  const max = Math.max(...funnelData.value.map(i => i.value))
  return max || 1
})

const conversionRate = computed(() => {
  const total = stats.value.totalResumes || 0
  const accepted = stats.value.acceptedCount || 0
  return total > 0 ? Math.round(accepted / total * 100) : 0
})

const conversionStyle = computed(() => {
  const rate = conversionRate.value
  const color = rate >= 20 ? '#67c23a' : rate >= 10 ? '#e6a23c' : '#f56c6c'
  return {
    background: `conic-gradient(${color} 0% ${rate}%, #f0f0f0 ${rate}% 100%)`
  }
})

function initChart() {
  if (!statusPieRef.value) return
  pieChart = echarts.init(statusPieRef.value)
}

function renderChart() {
  if (!pieChart || statusBreakdown.value.length === 0) return
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => `${params.name}: ${params.value}人 (${params.percent}%)`
    },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 12 } },
    color: COLOR_PALETTE,
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: statusBreakdown.value.length <= 6, fontSize: 12 },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: statusBreakdown.value.map(i => ({ name: i.label, value: i.value }))
    }]
  }, true)
}

async function loadData() {
  try {
    const res = await companyApi.getStatistics({})
    if (res && typeof res === 'object') {
      stats.value = res
      await new Promise(r => setTimeout(r, 50))
      renderChart()
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

function loadAuthStatus() {
  companyApi.getHomeStats().then(res => {
    authStatus.value = res?.authStatus || 'none'
  }).catch(() => {})
}

function openReApplyDialog() {
  companyApi.getProfile().then(profile => {
    Object.assign(reApplyForm, {
      companyName: profile?.companyName || '',
      contactPerson: profile?.contactPerson || '',
      contactPhone: profile?.contactPhone || '',
      contactEmail: profile?.contactEmail || '',
      province: profile?.province || '',
      city: profile?.city || '',
      address: profile?.address || '',
      industry: profile?.industry || '',
      scale: profile?.scale || '',
      nature: profile?.nature || '',
      introduction: profile?.introduction || ''
    })
    reApplyDialogVisible.value = true
  }).catch(() => {
    reApplyDialogVisible.value = true
  })
}

async function submitReApply() {
  const valid = await reApplyFormRef.value.validate().catch(() => false)
  if (!valid) return
  reApplyLoading.value = true
  try {
    await companyApi.reApply(reApplyForm)
    ElMessage.success('重新申请已提交，请等待审核')
    reApplyDialogVisible.value = false
    loadAuthStatus()
  } catch (err) {
    ElMessage.error(err.message || '提交失败')
  } finally {
    reApplyLoading.value = false
  }
}

onMounted(() => {
  initChart()
  loadData()
  loadAuthStatus()
  resizeObserver = new ResizeObserver(() => {
    pieChart?.resize()
  })
  if (statusPieRef.value) {
    resizeObserver.observe(statusPieRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  pieChart?.dispose()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }

.stats-row { margin-bottom: 16px; }
.stat-inner { display: flex; align-items: center; gap: 16px; }
.stat-icon { font-size: 32px; }
.stat-text { flex: 1; }
.stat-label { font-size: 13px; color: #606266; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; line-height: 1.2; }
.primary .stat-icon { color: #409eff; }
.success .stat-icon { color: #67c23a; }
.info .stat-icon { color: #5470c6; }

.chart-card { margin-bottom: 16px; }
.chart-row { margin-bottom: 16px; }

.chart-grid-2 { display: flex; gap: 24px; align-items: center; }
.chart-container { height: 280px; flex: 1; }

.status-breakdown { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.breakdown-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-radius: 8px;
  background: #f9f9f9;
  transition: all 0.3s;
}
.breakdown-item:hover { background: #f0f0f0; }
.breakdown-info { display: flex; align-items: center; gap: 10px; }
.breakdown-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.breakdown-label { font-size: 14px; color: #303133; font-weight: 500; }
.breakdown-value { display: flex; align-items: baseline; gap: 6px; }
.breakdown-count { font-size: 20px; font-weight: 700; color: #303133; }
.breakdown-percent { font-size: 12px; color: #909399; }

.funnel-container { display: flex; flex-direction: column; gap: 14px; padding: 10px 0; }
.funnel-item { display: flex; align-items: center; gap: 12px; }
.funnel-bar { flex: 1; height: 36px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.funnel-fill {
  height: 100%;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 12px;
  transition: width 0.8s ease;
  min-width: 40px;
}
.funnel-text { color: #fff; font-size: 14px; font-weight: 700; }
.funnel-label { width: 70px; text-align: right; font-size: 13px; color: #606266; }

.conversion-container { display: flex; flex-direction: column; align-items: center; gap: 20px; padding: 10px 0; }
.conversion-main { display: flex; justify-content: center; }
.conversion-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.conversion-value { font-size: 28px; font-weight: 700; color: #303133; }
.conversion-label { font-size: 12px; color: #909399; margin-top: 4px; }

.conversion-detail { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; justify-content: center; }
.conversion-item { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.conv-label { font-size: 11px; color: #909399; }
.conv-value { font-size: 20px; font-weight: 700; color: #303133; }
.conv-value.success { color: #67c23a; }
.conversion-arrow { font-size: 18px; color: #c0c4cc; }

.detail-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 16px; }
.detail-item { text-align: center; padding: 16px 8px; border: 1px solid #ebeef5; border-radius: 8px; transition: all 0.3s; }
.detail-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); transform: translateY(-2px); }
.detail-value { font-size: 32px; font-weight: 700; line-height: 1.2; }
.detail-label { font-size: 14px; font-weight: 600; color: #303133; margin-top: 6px; }
.detail-desc { font-size: 12px; color: #909399; margin-top: 4px; }

@media (max-width: 1200px) {
  .detail-grid { grid-template-columns: repeat(3, 1fr); }
  .chart-grid-2 { flex-direction: column; }
}
@media (max-width: 768px) {
  .detail-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
