<template>
  <div class="company-home">
    <!-- 审核状态提示 -->
    <div v-if="homeAuthStatus === 'rejected'" class="auth-alert">
      <el-alert
        title="您的企业入驻申请已被驳回，暂时无法发布职位和管理简历"
        type="error"
        show-icon
        :closable="false"
      />
      <el-button type="danger" plain style="margin-top: 10px;" @click="openReApplyDialog">
        <el-icon><RefreshRight /></el-icon> 重新申请入驻
      </el-button>
    </div>
    <el-alert
      v-else-if="homeAuthStatus === 'pending'"
      title="您的企业入驻申请正在审核中，暂时无法发布职位和管理简历"
      type="warning"
      show-icon
      :closable="false"
      class="auth-alert"
    />

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <h2 class="page-title">企业首页</h2>
        <p class="page-welcome">欢迎回来，查看企业招聘进展与人才投递动态</p>
      </div>
    </div>

    <!-- 第一行：统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card" @click="$router.push('/company/jobs')">
          <div class="stat-icon-wrap stat-icon-blue">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">在招职位</p>
            <p class="stat-value">{{ stats.jobCount ?? 0 }}</p>
          </div>
          <div class="stat-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" @click="$router.push('/company/resumes')">
          <div class="stat-icon-wrap stat-icon-green">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">收到简历</p>
            <p class="stat-value">{{ stats.resumeCount ?? 0 }}</p>
          </div>
          <div class="stat-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" @click="$router.push('/company/interviews')">
          <div class="stat-icon-wrap stat-icon-orange">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">面试安排</p>
            <p class="stat-value">{{ stats.interviewCount ?? 0 }}</p>
          </div>
          <div class="stat-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon-wrap stat-icon-red">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="stat-body">
            <p class="stat-label">录用人数</p>
            <p class="stat-value">{{ stats.offerCount ?? 0 }}</p>
          </div>
          <div class="stat-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 第二行：企业信息 + 快捷操作/公告 -->
    <el-row :gutter="20" class="second-row">
      <!-- 左侧：企业信息卡片 -->
      <el-col :xs="24" :lg="15">
        <div class="section-card">
          <div class="section-header">
            <span class="section-title">企业信息</span>
            <el-button type="primary" size="small" @click="openProfileDialog">
              <el-icon><Edit /></el-icon> 编辑信息
            </el-button>
          </div>

          <div v-if="loadingProfile" class="loading-state">
            <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          </div>

          <div v-else-if="profile" class="company-info-wrap">
            <!-- 企业名称 + 认证标签 -->
            <div class="company-name-row">
              <div class="company-name-wrap">
                <h3 class="company-name">{{ profile.companyName || '-' }}</h3>
                <div class="company-tags">
                  <span v-if="profile.industry" class="company-tag">{{ profile.industry }}</span>
                  <span v-if="profile.scale" class="company-tag">{{ profile.scale }}</span>
                  <span v-if="profile.nature" class="company-tag">{{ profile.nature }}</span>
                </div>
              </div>
              <el-tag
                :type="profile.authStatus === 'approved' ? 'success' : profile.authStatus === 'rejected' ? 'danger' : 'warning'"
                effect="dark"
                size="small"
                class="auth-tag"
              >
                {{ authStatusText(profile.authStatus) }}
              </el-tag>
            </div>

            <!-- 联系信息网格 -->
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">联系人</span>
                <span class="info-value">{{ profile.contactPerson || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">联系电话</span>
                <span class="info-value">{{ profile.contactPhone || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">联系邮箱</span>
                <span class="info-value">{{ profile.contactEmail || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">所在地</span>
                <span class="info-value">{{ [profile.province, profile.city, profile.address].filter(Boolean).join(' ') || '-' }}</span>
              </div>
            </div>

            <!-- 企业简介 -->
            <div v-if="profile.introduction" class="company-intro">
              <div class="intro-label">企业简介</div>
              <div class="intro-content">{{ profile.introduction }}</div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：快捷操作 + 公告 -->
      <el-col :xs="24" :lg="9">
        <div class="right-sidebar">
          <!-- 快捷操作 -->
          <div class="section-card quick-actions-card">
            <div class="section-header">
              <span class="section-title">快捷操作</span>
            </div>
            <div class="quick-actions-list">
              <div class="quick-action-item" @click="$router.push('/company/jobs')">
                <div class="qa-icon qa-icon-blue">
                  <el-icon><Document /></el-icon>
                </div>
                <div class="qa-text">
                  <span class="qa-title">管理职位</span>
                  <span class="qa-desc">发布与维护招聘岗位</span>
                </div>
                <el-icon class="qa-arrow"><ArrowRight /></el-icon>
              </div>
              <div class="quick-action-item" @click="$router.push('/company/resumes')">
                <div class="qa-icon qa-icon-green">
                  <el-icon><Tickets /></el-icon>
                </div>
                <div class="qa-text">
                  <span class="qa-title">处理简历</span>
                  <span class="qa-desc">查看并筛选学生简历</span>
                </div>
                <el-icon class="qa-arrow"><ArrowRight /></el-icon>
              </div>
              <div class="quick-action-item" @click="$router.push('/company/interviews')">
                <div class="qa-icon qa-icon-orange">
                  <el-icon><Calendar /></el-icon>
                </div>
                <div class="qa-text">
                  <span class="qa-title">面试管理</span>
                  <span class="qa-desc">安排和跟进面试流程</span>
                </div>
                <el-icon class="qa-arrow"><ArrowRight /></el-icon>
              </div>
            </div>
          </div>

          <!-- 系统公告 -->
          <div class="section-card notice-card">
            <div class="section-header">
              <span class="section-title">系统公告</span>
            </div>
            <div class="notice-body-wrap">
              <div v-if="noticesLoading" class="loading-state">
                <el-icon class="is-loading" :size="20"><Loading /></el-icon>
              </div>
              <div v-else-if="notices.length === 0" class="empty-notices">
                <el-icon :size="28" class="empty-icon"><Bell /></el-icon>
                <p>暂无公告</p>
              </div>
              <div v-else class="notice-list">
                <div
                  v-for="notice in notices.slice(0, 4)"
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
        </div>
      </el-col>
    </el-row>

    <!-- 第三行：招聘进度 -->
    <el-row :gutter="20" class="third-row">
      <el-col :span="24">
        <div class="section-card">
          <div class="section-header">
            <span class="section-title">招聘进度</span>
          </div>
          <div class="progress-steps">
            <div class="step-item" :class="{ active: (stats.jobCount ?? 0) > 0 }">
              <div class="step-icon">
                <el-icon><EditPen /></el-icon>
              </div>
              <div class="step-info">
                <span class="step-num">{{ stats.jobCount ?? 0 }}</span>
                <span class="step-label">职位发布</span>
              </div>
            </div>
            <div class="step-line"></div>
            <div class="step-item" :class="{ active: (stats.resumeCount ?? 0) > 0 }">
              <div class="step-icon">
                <el-icon><Tickets /></el-icon>
              </div>
              <div class="step-info">
                <span class="step-num">{{ stats.resumeCount ?? 0 }}</span>
                <span class="step-label">简历筛选</span>
              </div>
            </div>
            <div class="step-line"></div>
            <div class="step-item" :class="{ active: (stats.interviewCount ?? 0) > 0 }">
              <div class="step-icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="step-info">
                <span class="step-num">{{ stats.interviewCount ?? 0 }}</span>
                <span class="step-label">面试安排</span>
              </div>
            </div>
            <div class="step-line"></div>
            <div class="step-item" :class="{ active: (stats.offerCount ?? 0) > 0 }">
              <div class="step-icon">
                <el-icon><CircleCheck /></el-icon>
              </div>
              <div class="step-info">
                <span class="step-num">{{ stats.offerCount ?? 0 }}</span>
                <span class="step-label">录用确认</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ========== 对话框（完全保留原有逻辑） ========== -->

    <!-- 编辑企业信息对话框 -->
    <el-dialog v-model="profileDialogVisible" title="编辑企业信息" width="600px" destroy-on-close>
      <el-form :model="profileForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="企业名称">
              <el-input v-model="profileForm.companyName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="profileForm.contactPerson" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="profileForm.contactPhone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="profileForm.contactEmail" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属行业">
              <el-input v-model="profileForm.industry" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业规模">
              <el-input v-model="profileForm.scale" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="企业简介">
              <el-input v-model="profileForm.introduction" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

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
import { ref, reactive, onMounted } from 'vue'
import {
  Document, Tickets, User, DataLine, Loading, Calendar, RefreshRight,
  ArrowRight, Edit, Bell, EditPen, CircleCheck
} from '@element-plus/icons-vue'
import { companyApi, noticeApi } from '@/api'
import { ElMessage } from 'element-plus'

// ========== 状态和变量（完全保留原有逻辑）==========
const stats = ref({})
const homeAuthStatus = ref('approved')
const profile = ref(null)
const loadingProfile = ref(false)
const profileDialogVisible = ref(false)
const savingProfile = ref(false)
const profileForm = reactive({
  companyName: '', contactPerson: '', contactPhone: '', contactEmail: '',
  industry: '', scale: '', introduction: ''
})

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
const notices = ref([])
const noticesLoading = ref(false)
const noticeDetailVisible = ref(false)
const currentNotice = ref(null)

// ========== 方法（完全保留原有逻辑）==========
function authStatusText(status) {
  return { approved: '已认证', rejected: '已拒绝', pending: '待审核' }[status] || '待审核'
}

function loadStats() {
  companyApi.getHomeStats().then(res => {
    stats.value = res || {}
    homeAuthStatus.value = res?.authStatus || 'none'
  }).catch(() => {})
}

function loadProfile() {
  loadingProfile.value = true
  companyApi.getProfile().then(res => {
    profile.value = res
  }).catch(() => {
    profile.value = null
  }).finally(() => {
    loadingProfile.value = false
  })
}

function openProfileDialog() {
  Object.assign(profileForm, {
    companyName: profile.value?.companyName || '',
    contactPerson: profile.value?.contactPerson || '',
    contactPhone: profile.value?.contactPhone || '',
    contactEmail: profile.value?.contactEmail || '',
    industry: profile.value?.industry || '',
    scale: profile.value?.scale || '',
    introduction: profile.value?.introduction || ''
  })
  profileDialogVisible.value = true
}

function saveProfile() {
  savingProfile.value = true
  companyApi.updateProfile(profileForm).then(() => {
    ElMessage.success('保存成功')
    profileDialogVisible.value = false
    loadProfile()
  }).catch(err => {
    ElMessage.error(err.message || '保存失败')
  }).finally(() => {
    savingProfile.value = false
  })
}

function openReApplyDialog() {
  Object.assign(reApplyForm, {
    companyName: profile.value?.companyName || '',
    contactPerson: profile.value?.contactPerson || '',
    contactPhone: profile.value?.contactPhone || '',
    contactEmail: profile.value?.contactEmail || '',
    province: profile.value?.province || '',
    city: profile.value?.city || '',
    address: profile.value?.address || '',
    industry: profile.value?.industry || '',
    scale: profile.value?.scale || '',
    nature: profile.value?.nature || '',
    introduction: profile.value?.introduction || ''
  })
  reApplyDialogVisible.value = true
}

async function submitReApply() {
  const valid = await reApplyFormRef.value.validate().catch(() => false)
  if (!valid) return
  reApplyLoading.value = true
  try {
    await companyApi.reApply(reApplyForm)
    ElMessage.success('重新申请已提交，请等待审核')
    reApplyDialogVisible.value = false
    loadStats()
  } catch (err) {
    ElMessage.error(err.message || '提交失败')
  } finally {
    reApplyLoading.value = false
  }
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
  noticeDetailVisible.value = true
}

onMounted(() => {
  loadStats()
  loadProfile()
  loadNotices()
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.company-home {
  padding: 24px;
  max-width: 1400px;
}

/* ===== 审核提示 ===== */
.auth-alert {
  margin-bottom: 20px;
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

/* ===== 企业信息卡片 ===== */
.company-info-wrap {
  padding: 20px;
}

.company-name-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.company-name-wrap {
  flex: 1;
}

.company-name {
  font-size: 18px;
  font-weight: 800;
  color: #0f2a5f;
  margin: 0 0 8px;
}

.company-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.company-tag {
  display: inline-block;
  padding: 3px 10px;
  background: #f0f6ff;
  color: #2f6bff;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.auth-tag {
  flex-shrink: 0;
}

/* 联系信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 32px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-label {
  font-size: 12px;
  color: #8aa0c8;
  font-weight: 500;
}

.info-value {
  font-size: 13.5px;
  color: #374151;
  font-weight: 500;
}

/* 企业简介 */
.company-intro {
  background: #f5f9ff;
  border-radius: 10px;
  padding: 14px 16px;
  border: 1px solid #e5eaf3;
}

.intro-label {
  font-size: 12px;
  color: #8aa0c8;
  font-weight: 500;
  margin-bottom: 6px;
}

.intro-content {
  font-size: 13px;
  color: #5f6f8f;
  line-height: 1.7;
  white-space: pre-wrap;
}

/* ===== 右侧栏 ===== */
.right-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

/* 快捷操作卡片 */
.quick-actions-card {
  flex-shrink: 0;
}

.quick-actions-list {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quick-action-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.quick-action-item:hover {
  background: #f5f9ff;
  border-color: #c8dcff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(47, 107, 255, 0.08);
}

.qa-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.quick-action-item:hover .qa-icon {
  transform: scale(1.05);
}

.qa-icon .el-icon {
  font-size: 18px;
  color: #fff;
}

.qa-icon-blue { background: linear-gradient(135deg, #2f6bff, #60a5fa); }
.qa-icon-green { background: linear-gradient(135deg, #4caf50, #8bc34a); }
.qa-icon-orange { background: linear-gradient(135deg, #f59e0b, #fbbf24); }

.qa-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.qa-title {
  font-size: 13px;
  font-weight: 600;
  color: #2d4a72;
}

.qa-desc {
  font-size: 11px;
  color: #9ab5d6;
}

.qa-arrow {
  font-size: 14px;
  color: #c0d0e8;
  flex-shrink: 0;
  transition: color 0.2s, transform 0.2s;
}

.quick-action-item:hover .qa-arrow {
  color: #2f6bff;
  transform: translateX(3px);
}

/* ===== 公告 ===== */
.notice-card {
  flex: 1;
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
  height: 120px;
  color: #8aa0c8;
}

.empty-notices {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 120px;
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
  padding: 10px 6px;
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

/* ===== 第三行：招聘进度 ===== */
.third-row {
  margin-bottom: 20px;
}

.progress-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
  gap: 0;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.step-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #f0f4ff;
  border: 2px solid #e5eaf3;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.step-icon .el-icon {
  font-size: 22px;
  color: #8aa0c8;
  transition: color 0.3s;
}

.step-item.active .step-icon {
  background: linear-gradient(135deg, #eaf2ff, #f0f6ff);
  border-color: #2f6bff;
}

.step-item.active .step-icon .el-icon {
  color: #2f6bff;
}

.step-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.step-num {
  font-size: 20px;
  font-weight: 800;
  color: #8aa0c8;
  transition: color 0.3s;
}

.step-item.active .step-num {
  color: #0f2a5f;
}

.step-label {
  font-size: 12px;
  color: #9ab5d6;
  font-weight: 500;
}

.step-item.active .step-label {
  color: #5f6f8f;
}

.step-line {
  flex: 1;
  height: 2px;
  background: linear-gradient(90deg, #e5eaf3, #e5eaf3);
  margin: 0 8px;
  margin-bottom: 28px;
  max-width: 100px;
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

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .stats-row .el-col {
    margin-bottom: 0;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .progress-steps {
    flex-wrap: wrap;
    gap: 16px;
  }

  .step-line {
    display: none;
  }

  .third-row .el-col {
    margin-bottom: 16px;
  }
}

@media (max-width: 640px) {
  .company-home {
    padding: 16px;
  }

  .right-sidebar {
    gap: 12px;
  }
}
</style>
