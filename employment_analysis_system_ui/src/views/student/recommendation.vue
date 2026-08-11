<template>
  <div class="page-container">
    <h2>职位推荐</h2>

    <!-- 状态一：算法维护中 -->
    <el-card v-if="recommendStatus && recommendStatus.reason === 'ALGORITHM_DISABLED'" style="margin-top: 16px;">
      <div class="status-panel maintenance">
        <el-icon :size="48" color="#e6a23c"><Clock /></el-icon>
        <h3>算法功能已关闭</h3>
        <p>{{ recommendStatus.message }}</p>
        <el-tag type="warning">请等待数据分析员重新开启</el-tag>
      </div>
    </el-card>

    <!-- 状态二：无专业信息 -->
    <el-card v-else-if="recommendStatus && recommendStatus.reason === 'NO_MAJOR'" style="margin-top: 16px;">
      <div class="status-panel warning">
        <el-icon :size="48" color="#909399"><Warning /></el-icon>
        <h3>请完善专业信息</h3>
        <p>{{ recommendStatus.message }}</p>
        <el-button type="primary" @click="$router.push('/student/profile')">去完善信息</el-button>
      </div>
    </el-card>

    <!-- 状态三：模型未训练 -->
    <el-card v-else-if="recommendStatus && recommendStatus.reason === 'MODEL_NOT_TRAINED'" style="margin-top: 16px;">
      <div class="status-panel waiting">
        <el-icon :size="48" color="#e6a23c"><Cpu /></el-icon>
        <h3>推荐模型尚未训练</h3>
        <p>{{ recommendStatus.message }}</p>
        <el-tag type="warning">请耐心等待数据分析员训练模型</el-tag>
        <div style="margin-top: 16px;">
          <el-button @click="loadStatus" :loading="statusLoading">刷新状态</el-button>
        </div>
      </div>
    </el-card>

    <!-- 正常推荐入口 -->
    <el-card v-else-if="recommendStatus && recommendStatus.reason === 'AVAILABLE'" style="margin-top: 16px;">

      <!-- 推荐入口区 -->
      <div class="recommend-entry">
        <div class="entry-left">
          <el-icon :size="32" color="#409eff"><MagicStick /></el-icon>
          <div>
            <h3>开始智能推荐</h3>
            <p>选择简历后，系统将根据您的简历信息匹配最合适的职位</p>
          </div>
        </div>
        <div class="entry-right">
          <div class="resume-select-row">
            <el-select
              v-model="selectedResumeId"
              placeholder="请先选择简历"
              size="large"
              style="width: 260px;"
              :loading="resumesLoading"
              @change="onResumeChange"
            >
              <el-option
                v-for="r in resumes"
                :key="r.id"
                :label="r.resumeName + (r.isDefault === '1' ? ' ★ 默认' : '')"
                :value="r.id"
              />
            </el-select>
            <el-button type="primary" size="large" @click="loadRecommendations" :loading="loading" :disabled="!selectedResumeId">
              <el-icon v-if="!loading"><Refresh /></el-icon> 开始推荐
            </el-button>
            <span style="font-size:12px;color:#909399;margin-left:8px;">来源筛选: </span>
            <el-select v-model="sourceFilter" size="small" style="width: 180px;">
              <el-option label="全部职位" value="all" />
              <el-option label="仅看入驻企业职位" value="hr" />
              <el-option label="过滤掉第三方职位" value="hr" />
              <el-option label="仅看第三方职位" value="spider" />
            </el-select>
            <span style="font-size:12px;color:#909399;margin-left:8px;">推荐数量: </span>
            <el-input-number v-model="topN" :min="5" :max="50" size="small" style="width:100px;" />
          </div>
          <div v-if="!resumesLoading && resumes.length === 0" style="margin-top: 8px;">
            <el-button type="text" @click="$router.push('/student/resume/create')">还没有简历？去创建一份</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 推荐结果区域 -->
    <el-card v-if="recommendations.length > 0 || loading" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
            <span>推荐结果</span>
            <el-tag type="success" size="small">{{ recommendations.length }} 个职位</el-tag>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-select
              v-model="sourceFilter"
              placeholder="职位来源"
              size="small"
              style="width: 180px;"
            >
              <el-option label="全部职位" value="all" />
              <el-option label="仅看入驻企业职位" value="hr" />
              <el-option label="仅看第三方爬虫职位" value="spider" />
            </el-select>
            <el-select
              v-model="selectedResumeId"
              placeholder="切换简历"
              size="small"
              style="width: 180px;"
              @change="onResumeChange"
            >
              <el-option
                v-for="r in resumes"
                :key="r.id"
                :label="r.resumeName + (r.isDefault === '1' ? ' ★' : '')"
                :value="r.id"
              />
            </el-select>
            <el-button type="primary" size="small" @click="loadRecommendations" :loading="loading">
              <el-icon v-if="!loading"><Refresh /></el-icon> 重新推荐
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <span>正在分析您的简历，计算推荐结果...</span>
      </div>

      <div v-else-if="recommendations.length === 0" class="empty-state">
        <el-icon :size="48" color="#c0c4cc"><Briefcase /></el-icon>
        <p style="color: #666; margin: 16px 0;">暂无推荐结果，请尝试选择其他简历或完善简历信息</p>
        <el-button type="primary" @click="$router.push('/student/resume')">去完善简历</el-button>
      </div>

      <div v-else class="job-list">
        <div v-for="(job, index) in recommendations" :key="index" class="job-card">
          <div class="job-header">
            <div class="job-title">
              <span class="job-name" @click="viewJobDetail(job)">{{ job.jobName }}</span>
              <el-tag v-if="job.matchScore && job.matchScore >= 70" type="success" size="small">
                高度匹配
              </el-tag>
              <el-tag v-else-if="job.matchScore && job.matchScore >= 50" type="warning" size="small">
                较好匹配
              </el-tag>
              <el-tag v-else type="info" size="small">一般匹配</el-tag>
            </div>
            <div class="job-salary">
              <span class="salary-value">{{ job.salary || '面议' }}</span>
            </div>
          </div>

          <div class="match-reason" v-if="job.matchReason">
            <el-icon size="12" color="#409eff"><MagicStick /></el-icon>
            <span>{{ job.matchReason }}</span>
          </div>
          <!-- 爬虫来源标识 -->
          <div v-if="job.positionSource === 'spider'" class="spider-source-bar">
            <el-icon size="12" color="#909399"><Link /></el-icon>
            <span>来自 <strong>{{ getSourceLabel(job.source) }}</strong> · 点击"去原网站查看"可查看详情并自行投递</span>
          </div>

          <div class="job-info">
            <span><el-icon><Location /></el-icon> {{ job.city || '不限城市' }}</span>
            <span><el-icon><Reading /></el-icon> {{ job.education || '学历不限' }}</span>
            <span v-if="job.experience"><el-icon><Clock /></el-icon> {{ job.experience }}</span>
            <span><el-icon><TrendCharts /></el-icon> 匹配分: <strong :style="{ color: getScoreColor(job.matchScore) }">{{ job.matchScore }}</strong></span>
          </div>
          <div class="job-company">{{ job.companyName }}</div>
          <div class="job-footer">
            <div class="job-tags" v-if="job.responsibility">
              <el-tag
                v-for="tag in extractTags(job.responsibility).slice(0, 4)"
                :key="tag"
                size="small"
                type="info"
              >{{ tag }}</el-tag>
            </div>
            <!-- HR发布的职位 -->
            <div v-if="job.positionSource === 'hr'" class="job-actions">
              <el-tag type="success" size="small">本校HR发布</el-tag>
              <el-button type="success" size="small" @click="handleApply(job)">立即投递</el-button>
            </div>
            <!-- 爬虫职位：显示来源 + 跳转链接 -->
            <div v-else-if="job.positionSource === 'spider'" class="job-actions">
              <el-tag type="info" size="small">{{ getSourceLabel(job.source) }}</el-tag>
              <el-button
                type="primary"
                size="small"
                plain
                @click="openOriginalUrl(job)"
                :disabled="!job.detailUrl"
              >
                <el-icon><Link /></el-icon> 去原网站查看
              </el-button>
            </div>
          </div>

          <!-- 反馈区域 -->
          <div class="feedback-bar" v-if="job._showFeedback">
            <span style="font-size: 13px; color: #909399;">该推荐对您有帮助吗？</span>
            <el-button
              size="small"
              :type="job._feedbackGiven === 'positive' ? 'success' : 'default'"
              :icon="Check"
              circle
              @click="submitFeedback(job, 'positive')"
            />
            <el-button
              size="small"
              :type="job._feedbackGiven === 'negative' ? 'danger' : 'default'"
              :icon="Close"
              circle
              @click="submitFeedback(job, 'negative')"
            />
          </div>
          <div class="feedback-bar-toggle" @click="job._showFeedback = !job._showFeedback">
            <span>{{ job._showFeedback ? '收起反馈' : '反馈' }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- HR职位投递弹窗 -->
    <el-dialog v-model="applyDialogVisible" title="选择简历投递" width="500px" destroy-on-close>
      <div v-if="resumesLoading" style="text-align:center;padding:20px;">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <span style="margin-left:8px;">加载简历中...</span>
      </div>
      <div v-else-if="resumes.length === 0" style="text-align:center;padding:20px;color:#666;">
        <p style="margin-bottom:16px;">您还没有简历，请先创建简历再投递</p>
        <el-button type="primary" @click="goToCreateResume">去创建简历</el-button>
      </div>
      <div v-else>
        <p style="margin-bottom:12px;color:#666;font-size:13px;">请选择要投递的简历：</p>
        <p style="margin-bottom:12px;color:#409eff;font-size:13px;">
          投递职位：<strong>{{ applyingJob?.jobName }}</strong>（来自本校HR发布的职位）
        </p>
        <el-radio-group v-model="selectedResumeId" style="display:flex;flex-direction:column;gap:10px;">
          <el-radio v-for="r in resumes" :key="r.id" :value="r.id" :label="r.id" border class="resume-radio">
            <div class="resume-radio-content">
              <span class="resume-name">{{ r.resumeName }}</span>
              <el-tag v-if="r.isDefault === '1'" type="success" size="small">默认</el-tag>
            </div>
          </el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" :disabled="!selectedResumeId" @click="confirmApply">确认投递</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loading, Refresh, Briefcase, Location, Reading, Clock, Bell, Warning, TrendCharts, Check, Close, MagicStick, Cpu, Link } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { recommendApi, studentApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const statusLoading = ref(false)
const recommendations = ref([])
const recommendStatus = ref(null)

// 投递弹窗相关
const applyDialogVisible = ref(false)
const applyingJob = ref(null)
const resumes = ref([])
const resumesLoading = ref(false)
const selectedResumeId = ref(null)
const applyLoading = ref(false)
const topN = ref(20)
const sourceFilter = ref('all')

function loadStatus() {
  statusLoading.value = true
  recommendApi.getRecommendStatus().then(res => {
    recommendStatus.value = res || {}
  }).catch(() => {
    recommendStatus.value = null
  }).finally(() => {
    statusLoading.value = false
  })
}

function loadRecommendations() {
  if (!selectedResumeId.value) {
    ElMessage.warning('请先选择一份简历')
    return
  }
  loading.value = true
    recommendations.value = []
    recommendApi.getJobRecommendations({ topN: topN.value, resumeId: selectedResumeId.value, sourceFilter: sourceFilter.value }).then(res => {
    recommendations.value = Array.isArray(res) ? res : []
    recommendations.value.forEach(job => {
      job._showFeedback = false
      job._feedbackGiven = null
    })
    if (recommendations.value.length === 0) {
      ElMessage.info('暂无匹配职位，请尝试完善简历信息或选择其他简历')
    }
  }).catch(err => {
    recommendations.value = []
    const msg = err?.response?.data?.message
    if (msg === 'ALGORITHM_DISABLED') {
      recommendStatus.value = { reason: 'ALGORITHM_DISABLED', message: '该专业推荐功能已关闭，请等待数据分析员重新开启' }
    } else if (msg === 'MODEL_NOT_TRAINED') {
      recommendStatus.value = { reason: 'MODEL_NOT_TRAINED', message: '推荐模型尚未训练，请耐心等待数据分析员完成训练' }
    } else {
      ElMessage.error(msg || '加载推荐结果失败')
    }
  }).finally(() => {
    loading.value = false
  })
}

function onResumeChange() {
  recommendations.value = []
}

function handleApply(job) {
  if (job.positionSource === 'hr') {
    openApplyDialogForHr(job)
  }
}

function openApplyDialogForHr(job) {
  applyingJob.value = job
  selectedResumeId.value = null
  applyDialogVisible.value = true
  loadResumes()
}

function viewJobDetail(job) {
  if (job.positionSource === 'hr') {
    router.push(`/student/job-detail/${job.id}`)
  } else if (job.positionSource === 'spider') {
    openOriginalUrl(job)
  }
}

function getSourceLabel(source) {
  const map = {
    '51job': '前程无忧',
    'yingjiesheng': '应届生求职网',
    'moe': '教育部24365',
    'gov': '政府公共就业',
    'education': '高校就业网',
  }
  return map[source] || source || '第三方平台'
}

function openOriginalUrl(job) {
  if (!job.detailUrl) {
    ElMessage.warning('该职位暂无原始链接')
    return
  }
  window.open(job.detailUrl, '_blank')
}

function submitFeedback(job, feedback) {
  job._feedbackGiven = feedback
  recommendApi.submitFeedback({
    historyId: job.historyId,
    feedback: feedback,
    reason: feedback === 'positive' ? '感兴趣' : '不合适'
  }).then(() => {
    ElMessage.success('感谢您的反馈')
    job._showFeedback = false
  }).catch(() => {
    ElMessage.error('反馈提交失败')
    job._feedbackGiven = null
  })
}

function getScoreColor(score) {
  if (!score) return '#909399'
  if (score >= 70) return '#67c23a'
  if (score >= 50) return '#e6a23c'
  return '#909399'
}

function extractTags(text) {
  if (!text) return []
  const lines = text.split(/[。；;]/).filter(l => l.trim().length > 2 && l.trim().length < 30)
  return lines.map(l => l.trim()).filter(Boolean)
}

function loadResumes() {
  resumesLoading.value = true
  studentApi.getResumes().then(res => {
    resumes.value = res || []
    const defaultResume = resumes.value.find(r => r.isDefault === '1')
    if (defaultResume && !selectedResumeId.value) {
      selectedResumeId.value = defaultResume.id
    }
  }).catch(() => {
    resumes.value = []
  }).finally(() => {
    resumesLoading.value = false
  })
}

function confirmApply() {
  if (!selectedResumeId.value) {
    ElMessage.warning('请选择一份简历')
    return
  }
  applyLoading.value = true
  recommendApi.applyHrJob(applyingJob.value.id, { resumeId: selectedResumeId.value }).then(() => {
    ElMessage.success('投递成功')
    applyingJob.value.hasApplied = true
    applyDialogVisible.value = false
  }).catch(err => {
    ElMessage.error(err.message || err.response?.data?.message || '投递失败')
  }).finally(() => {
    applyLoading.value = false
  })
}

function goToCreateResume() {
  applyDialogVisible.value = false
  router.push('/student/resume/create')
}

onMounted(() => {
  loadStatus()
  loadResumes()
})
</script>

<style scoped>
.page-container { padding: 20px; }
h2 { margin: 0 0 8px; font-size: 18px; font-weight: 600; color: #303133; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; }

.status-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}
.status-panel h3 { margin: 16px 0 8px; font-size: 18px; color: #303133; }
.status-panel p { color: #666; margin: 0 0 16px; }

.recommend-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  background: #f5f7fa;
  border-radius: 12px;
  flex-wrap: wrap;
}
.entry-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.entry-left h3 { margin: 0 0 4px; font-size: 16px; color: #303133; }
.entry-left p { margin: 0; font-size: 13px; color: #909399; }
.entry-right { display: flex; flex-direction: column; gap: 8px; }
.resume-select-row { display: flex; gap: 10px; align-items: center; }

.job-list { display: flex; flex-direction: column; gap: 12px; }
.job-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 16px; transition: all 0.3s; position: relative; }
.job-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); border-color: #409eff; }
.job-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.job-title { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.job-name { font-size: 16px; font-weight: 600; color: #303133; cursor: pointer; }
.job-name:hover { color: #409eff; }
.job-salary { color: #f56c6c; font-weight: 600; font-size: 16px; }
.salary-value { white-space: nowrap; }
.match-reason {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #409eff;
  margin-bottom: 8px;
  background: #ecf5ff;
  padding: 4px 8px;
  border-radius: 4px;
}
.job-info { display: flex; gap: 20px; color: #909399; font-size: 13px; margin-bottom: 6px; flex-wrap: wrap; }
.job-info span { display: flex; align-items: center; gap: 4px; }
.job-company { color: #606266; font-size: 14px; margin-bottom: 10px; }
.job-footer { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.job-tags { display: flex; flex-wrap: wrap; gap: 6px; max-width: 60%; }
.job-actions { display: flex; gap: 8px; }
.spider-source-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
}
.spider-source-bar strong { color: #606266; }
.feedback-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}
.feedback-bar-toggle {
  text-align: center;
  margin-top: 8px;
  font-size: 12px;
  color: #c0c4cc;
  cursor: pointer;
}
.feedback-bar-toggle:hover { color: #409eff; }
</style>
