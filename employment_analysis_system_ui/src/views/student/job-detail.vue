<template>
  <div class="page-container">
    <el-page-header @back="$router.back()" title="返回" content="职位详情" />

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <div v-else-if="job" class="detail-layout">
      <div class="detail-main">
        <el-card style="margin-top: 16px;">
          <div class="job-header">
            <div class="job-info">
              <div class="job-title-row">
                <h2 class="job-title">{{ job.jobName }}</h2>
                <el-tag v-if="job.isHighSalary === '1'" type="danger">高薪</el-tag>
                <el-tag v-if="job.isRemote === '1'">远程</el-tag>
              </div>
              <div class="salary-row">
                <span class="salary" v-if="job.salaryMin && job.salaryMax">
                  {{ formatSalaryK(job.salaryMin) }}～{{ formatSalaryK(job.salaryMax) }}元/月
                </span>
                <span class="salary" v-else>薪资面议</span>
              </div>
              <div class="base-tags">
                <span class="base-tag"><el-icon><Location /></el-icon> {{ job.workCity || '待定' }}</span>
                <span class="base-tag"><el-icon><Reading /></el-icon> {{ job.educationRequired || '学历不限' }}</span>
                <span class="base-tag"><el-icon><Briefcase /></el-icon> {{ job.experienceRequired || '经验不限' }}</span>
                <span class="base-tag"><el-icon><User /></el-icon> 招聘{{ job.recruitNumber || '若干' }}人</span>
              </div>
            </div>
            <div class="action-buttons">
              <el-button
                v-if="!job.hasApplied"
                type="primary"
                size="large"
                :loading="applying"
                @click="openApplyDialog"
              >投递简历</el-button>
              <el-button v-else type="info" size="large" disabled>已投递</el-button>
              <el-button
                size="large"
                :icon="job.hasFavorite ? StarFilled : Star"
                :type="job.hasFavorite ? 'warning' : 'default'"
                @click="handleToggleFavorite"
              >{{ job.hasFavorite ? '已收藏' : '收藏' }}</el-button>
            </div>
          </div>
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header><div class="section-title">职位详情</div></template>
          <div class="section-content">
            <div class="section-item" v-if="job.responsibility">
              <div class="section-label">岗位职责</div>
              <div class="section-text">{{ job.responsibility }}</div>
            </div>
            <div class="section-item" v-if="job.requirement">
              <div class="section-label">任职要求</div>
              <div class="section-text">{{ job.requirement }}</div>
            </div>
            <div class="section-item" v-if="job.skillRequired">
              <div class="section-label">技能要求</div>
              <div class="section-text">{{ job.skillRequired }}</div>
            </div>
            <div class="section-item" v-if="job.benefits">
              <div class="section-label">福利待遇</div>
              <div class="section-text">{{ job.benefits }}</div>
            </div>
            <div class="section-item" v-if="job.workAddress">
              <div class="section-label">工作地址</div>
              <div class="section-text">{{ job.workAddress }}</div>
            </div>
          </div>
        </el-card>

        <el-card style="margin-top: 16px;" v-if="job.companyIntroduction">
          <template #header><div class="section-title">公司信息</div></template>
          <div class="section-content">
            <div class="company-intro">{{ job.companyIntroduction }}</div>
          </div>
        </el-card>
      </div>

      <div class="detail-side">
        <el-card style="margin-top: 16px;">
          <template #header><div class="section-title">公司概况</div></template>
          <div class="company-card">
            <div class="company-name-lg">{{ job.companyName }}</div>
            <div class="company-info-list">
              <div class="ci-item" v-if="job.companyIndustry">
                <el-icon><OfficeBuilding /></el-icon>
                <span>{{ job.companyIndustry }}</span>
              </div>
              <div class="ci-item" v-if="job.companyNature">
                <el-icon><Grid /></el-icon>
                <span>{{ job.companyNature }}</span>
              </div>
              <div class="ci-item" v-if="job.companyScale">
                <el-icon><User /></el-icon>
                <span>{{ job.companyScale }}</span>
              </div>
              <div class="ci-item" v-if="job.companyCity">
                <el-icon><Location /></el-icon>
                <span>{{ job.companyCity }}</span>
              </div>
              <div class="ci-item" v-if="job.companyAddress">
                <el-icon><HomeFilled /></el-icon>
                <span>{{ job.companyAddress }}</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header><div class="section-title">职位统计</div></template>
          <div class="stats-list">
            <div class="stat-item">
              <span class="stat-label">浏览次数</span>
              <span class="stat-value">{{ job.viewCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">投递人数</span>
              <span class="stat-value">{{ job.applyCount || 0 }}</span>
            </div>
            <div class="stat-item" v-if="job.publishTime">
              <span class="stat-label">发布时间</span>
              <span class="stat-value">{{ job.publishTime?.substring(0, 10) }}</span>
            </div>
            <div class="stat-item" v-if="job.deadline">
              <span class="stat-label">截止日期</span>
              <span class="stat-value">{{ job.deadline?.substring(0, 10) }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <div v-else class="loading-state">
      <el-empty description="职位不存在或已下架" />
    </div>

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
import { useRoute, useRouter } from 'vue-router'
import {
  Location, Reading, Briefcase, User, Star, StarFilled,
  OfficeBuilding, Grid, HomeFilled, Loading
} from '@element-plus/icons-vue'
import { jobApi, studentApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const applying = ref(false)
const job = ref(null)

const applyDialogVisible = ref(false)
const resumes = ref([])
const resumesLoading = ref(false)
const selectedResumeId = ref(null)
const applyLoading = ref(false)

function loadDetail() {
  const id = route.params.id
  if (!id) { loading.value = false; return }
  loading.value = true
  jobApi.getDetail(id).then(res => {
    job.value = res || null
  }).catch(() => {
    job.value = null
  }).finally(() => {
    loading.value = false
  })
}

function openApplyDialog() {
  selectedResumeId.value = null
  applyDialogVisible.value = true
  loadResumes()
}

function loadResumes() {
  resumesLoading.value = true
  studentApi.getResumes().then(res => {
    resumes.value = res || []
    const defaultResume = resumes.value.find(r => r.isDefault === '1')
    if (defaultResume) {
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
  applying.value = true
  jobApi.applyJob(job.value.id, { resumeId: selectedResumeId.value }).then(() => {
    ElMessage.success('投递成功')
    job.value.hasApplied = true
    job.value.applyCount = (job.value.applyCount || 0) + 1
    applyDialogVisible.value = false
  }).catch(err => {
    ElMessage.error(err.message || '投递失败，请稍后重试')
  }).finally(() => {
    applyLoading.value = false
    applying.value = false
  })
}

function goToCreateResume() {
  applyDialogVisible.value = false
  router.push('/student/resume/create')
}

function handleToggleFavorite() {
  if (job.value.hasFavorite) {
    jobApi.unfavoriteJob(job.value.id).then(() => {
      ElMessage.success('已取消收藏')
      job.value.hasFavorite = false
    }).catch(err => {
      ElMessage.error(err.message || '操作失败')
    })
  } else {
    jobApi.favoriteJob(job.value.id).then(() => {
      ElMessage.success('收藏成功')
      job.value.hasFavorite = true
    }).catch(err => {
      ElMessage.error(err.message || '请先登录')
    })
  }
}

function formatSalaryK(v) {
  const n = parseInt(v)
  if (isNaN(n)) return v
  if (n >= 10000) return (n / 1000) + 'K'
  return v
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; color: #999; gap: 12px; }
.detail-layout { display: flex; gap: 16px; align-items: flex-start; }
.detail-main { flex: 1; min-width: 0; }
.detail-side { width: 300px; flex-shrink: 0; }
.job-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 24px; }
.job-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.job-title { font-size: 22px; font-weight: 700; color: #222; margin: 0; }
.salary-row { margin-bottom: 12px; }
.salary { font-size: 20px; font-weight: 700; color: #f56c6c; }
.base-tags { display: flex; gap: 16px; flex-wrap: wrap; }
.base-tag { font-size: 13px; color: #666; display: flex; align-items: center; gap: 4px; }
.action-buttons { display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; }
.section-title { font-weight: 600; font-size: 16px; }
.section-content { font-size: 14px; color: #555; line-height: 1.8; }
.section-item { margin-bottom: 16px; }
.section-item:last-child { margin-bottom: 0; }
.section-label { font-weight: 600; color: #333; margin-bottom: 6px; font-size: 14px; }
.section-text { white-space: pre-wrap; line-height: 1.8; }
.company-intro { white-space: pre-wrap; line-height: 1.8; color: #555; }
.company-name-lg { font-size: 18px; font-weight: 700; color: #222; margin-bottom: 12px; }
.company-info-list { display: flex; flex-direction: column; gap: 8px; }
.ci-item { font-size: 13px; color: #666; display: flex; align-items: flex-start; gap: 8px; }
.ci-item .el-icon { margin-top: 2px; flex-shrink: 0; color: #999; }
.stats-list { display: flex; flex-direction: column; gap: 10px; }
.stat-item { display: flex; justify-content: space-between; font-size: 13px; }
.stat-label { color: #999; }
.stat-value { color: #333; font-weight: 500; }
.resume-radio { width: 100%; padding: 12px 16px; margin-right: 0; }
.resume-radio-content { display: flex; align-items: center; gap: 8px; }
.resume-name { font-weight: 500; }
</style>
