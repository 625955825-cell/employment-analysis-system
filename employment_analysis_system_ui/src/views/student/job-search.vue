<template>
  <div class="page-container">
    <h2>职位搜索</h2>

    <el-card style="margin-top: 16px;">
      <div class="search-bar">
        <el-input
          v-model="params.keyword"
          placeholder="搜索职位名称或公司名称"
          clearable
          style="width: 280px;"
          @keyup.enter="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="params.city" placeholder="工作城市" clearable style="width: 150px;">
          <el-option label="北京" value="北京" />
          <el-option label="上海" value="上海" />
          <el-option label="广州" value="广州" />
          <el-option label="深圳" value="深圳" />
          <el-option label="杭州" value="杭州" />
          <el-option label="成都" value="成都" />
          <el-option label="南京" value="南京" />
          <el-option label="武汉" value="武汉" />
          <el-option label="西安" value="西安" />
          <el-option label="苏州" value="苏州" />
          <el-option label="重庆" value="重庆" />
          <el-option label="天津" value="天津" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <div v-if="loadError" class="error-wrapper">
        <el-alert type="error" :title="loadError" :closable="false" show-icon />
        <el-button type="primary" plain style="margin-top:12px" @click="loadJobs">重试</el-button>
      </div>

      <div v-else-if="loading" class="loading-wrapper">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="jobs.length === 0" class="empty-wrapper">
        <el-empty description="暂无符合条件的职位" :image-size="80">
          <template #image>
            <el-icon :size="60" color="#ccc"><Search /></el-icon>
          </template>
        </el-empty>
      </div>

      <div v-else class="job-list">
        <div v-for="job in jobs" :key="job.id" class="job-card">
          <div class="job-main">
            <div class="job-title-row">
              <span class="job-name" @click="$router.push(`/student/job-detail/${job.id}`)">{{ job.jobName }}</span>
              <el-tag v-if="job.isHighSalary === '1'" type="danger" size="small">高薪</el-tag>
              <el-tag v-if="job.isRemote === '1'" size="small">远程</el-tag>
            </div>
            <div class="company-name">{{ job.companyName }}</div>
            <div class="job-tags">
              <span class="tag salary" v-if="job.salaryMin && job.salaryMax">
                {{ formatSalary(job.salaryMin, job.salaryMax) }}
              </span>
              <span class="tag" v-if="job.workCity">{{ job.workCity }}</span>
              <span class="tag" v-if="job.educationRequired">{{ job.educationRequired }}</span>
              <span class="tag" v-if="job.experienceRequired">{{ job.experienceRequired }}</span>
            </div>
          </div>
          <div class="job-side">
            <div class="job-meta">
              <span class="meta-item">
                <el-icon><View /></el-icon> {{ job.viewCount || 0 }}
              </span>
              <span class="meta-item">
                <el-icon><Document /></el-icon> {{ job.applyCount || 0 }}人投递
              </span>
              <span class="meta-item" v-if="job.publishTime">{{ formatDate(job.publishTime) }}</span>
            </div>
            <div class="job-actions">
              <el-button
                v-if="!job.hasApplied"
                type="primary"
                size="small"
                @click="openApplyDialog(job)"
              >投递简历</el-button>
              <el-button v-else type="info" size="small" disabled>已投递</el-button>
              <el-button
                size="small"
                :icon="job.hasFavorite ? StarFilled : Star"
                :type="job.hasFavorite ? 'warning' : 'default'"
                @click="handleToggleFavorite(job)"
              >{{ job.hasFavorite ? '已收藏' : '收藏' }}</el-button>
            </div>
          </div>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="params.page"
            v-model:page-size="params.size"
            :page-sizes="[10, 20, 30]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadJobs"
            @current-change="loadJobs"
          />
        </div>
      </div>
    </el-card>

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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh, View, Document, Star, StarFilled, Loading } from '@element-plus/icons-vue'
import { jobApi, studentApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const jobs = ref([])
const total = ref(0)
const params = reactive({
  keyword: '',
  city: '',
  page: 1,
  size: 10
})

const applyDialogVisible = ref(false)
const applyingJob = ref(null)
const resumes = ref([])
const resumesLoading = ref(false)
const selectedResumeId = ref(null)
const applyLoading = ref(false)

function formatSalary(min, max) {
  if (!min && !max) return '薪资面议'
  const fmt = (v) => {
    const n = parseInt(v)
    if (isNaN(n)) return v
    if (n >= 10000) return (n / 1000) + 'K'
    return v
  }
  return `${fmt(min)}～${fmt(max)}元/月`
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  try {
    return dateStr.substring(0, 10)
  } catch {
    return dateStr
  }
}

function loadJobs() {
  loading.value = true
  loadError.value = ''
  jobApi.getList(params).then(res => {
    jobs.value = res?.records || []
    total.value = res?.total || 0
  }).catch(err => {
    console.error('职位加载失败:', err)
    loadError.value = '加载失败：' + (err.message || '请检查后端服务是否启动')
    jobs.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function handleSearch() {
  params.page = 1
  loadJobs()
}

function handleReset() {
  params.keyword = ''
  params.city = ''
  params.page = 1
  loadJobs()
}

function openApplyDialog(job) {
  applyingJob.value = job
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
  jobApi.applyJob(applyingJob.value.id, { resumeId: selectedResumeId.value }).then(() => {
    ElMessage.success('投递成功')
    applyingJob.value.hasApplied = true
    applyingJob.value.applyCount = (applyingJob.value.applyCount || 0) + 1
    applyDialogVisible.value = false
  }).catch(err => {
    ElMessage.error(err.message || '投递失败')
  }).finally(() => {
    applyLoading.value = false
  })
}

function goToCreateResume() {
  applyDialogVisible.value = false
  router.push('/student/resume/create')
}

function handleToggleFavorite(job) {
  if (job.hasFavorite) {
    jobApi.unfavoriteJob(job.id).then(() => {
      ElMessage.success('已取消收藏')
      job.hasFavorite = false
    }).catch(err => {
      ElMessage.error(err.message || '操作失败')
    })
  } else {
    jobApi.favoriteJob(job.id).then(() => {
      ElMessage.success('收藏成功')
      job.hasFavorite = true
    }).catch(err => {
      ElMessage.error(err.message || '收藏失败')
    })
  }
}

onMounted(() => {
  loadJobs()
})
</script>

<style scoped>
.search-bar { display: flex; gap: 10px; align-items: center; margin-bottom: 16px; flex-wrap: wrap; }
.loading-wrapper, .empty-wrapper, .error-wrapper { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.job-list { display: flex; flex-direction: column; gap: 0; }
.job-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  gap: 16px;
}
.job-card:last-child { border-bottom: none; }
.job-main { flex: 1; min-width: 0; }
.job-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.job-name { font-size: 16px; font-weight: 600; color: #333; cursor: pointer; }
.job-name:hover { color: #409eff; }
.company-name { font-size: 14px; color: #666; margin-bottom: 8px; }
.job-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.tag { font-size: 12px; color: #666; background: #f5f5f5; padding: 2px 8px; border-radius: 4px; }
.tag.salary { color: #f56c6c; font-weight: 600; background: #fef0f0; }
.job-side { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
.job-meta { display: flex; gap: 12px; align-items: center; }
.meta-item { font-size: 12px; color: #999; display: flex; align-items: center; gap: 3px; }
.job-actions { display: flex; gap: 8px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 20px; }
.resume-radio { width: 100%; padding: 12px 16px; margin-right: 0; }
.resume-radio-content { display: flex; align-items: center; gap: 8px; }
.resume-name { font-weight: 500; }
</style>
