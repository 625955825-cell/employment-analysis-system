<template>
  <div class="page-container">
    <div class="page-header">
      <h2>职位管理</h2>
      <el-button type="primary" :disabled="authStatus === 'rejected' || authStatus === 'pending'" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> 发布新职位
      </el-button>
    </div>

    <div v-if="authStatus === 'rejected'" style="margin-bottom: 16px;">
      <el-alert
        title="您的企业入驻申请已被驳回，暂时无法发布职位"
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
      title="您的企业入驻申请正在审核中，暂时无法发布职位，请耐心等待"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 16px;"
    />

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner blue">
            <el-icon class="stat-icon"><Briefcase /></el-icon>
            <div class="stat-text">
              <p class="stat-label">全部职位</p>
              <p class="stat-value">{{ stats.total }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner green">
            <el-icon class="stat-icon"><CircleCheck /></el-icon>
            <div class="stat-text">
              <p class="stat-label">招聘中</p>
              <p class="stat-value">{{ stats.published }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner orange">
            <el-icon class="stat-icon"><VideoPause /></el-icon>
            <div class="stat-text">
              <p class="stat-label">已下架</p>
              <p class="stat-value">{{ stats.paused }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner purple">
            <el-icon class="stat-icon"><Tickets /></el-icon>
            <div class="stat-text">
              <p class="stat-label">累计投递</p>
              <p class="stat-value">{{ stats.totalApplications }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 职位列表 -->
    <el-card shadow="hover" class="list-card">
      <template #header>
        <div class="card-header">
          <span>职位列表</span>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="搜索职位名称"
              style="width: 200px;"
              clearable
              @clear="loadJobs"
              @keyup.enter="loadJobs"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 130px;" @change="loadJobs">
              <el-option label="全部" value="" />
              <el-option label="招聘中" value="published" />
              <el-option label="已下架" value="paused" />
            </el-select>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-empty v-else-if="jobList.length === 0" description="暂无职位，点击上方按钮发布" />

      <el-table v-else :data="jobList" stripe border>
        <el-table-column prop="jobName" label="职位名称" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="job-name-link" @click="openDetailDialog(row)">{{ row.jobName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="jobCategory" label="职位类别" width="120" show-overflow-tooltip />
        <el-table-column prop="workCity" label="工作城市" width="100" />
        <el-table-column prop="salaryMin" label="薪资范围" width="140">
          <template #default="{ row }">
            <span class="salary-text">
              {{ row.salaryMin ? row.salaryMin + 'k' : '--' }} ~ {{ row.salaryMax ? row.salaryMax + 'k' : '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="recruitNumber" label="招聘人数" width="90" align="center">
          <template #default="{ row }">{{ row.recruitNumber || '-' }}</template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="80" align="center" />
        <el-table-column prop="applyCount" label="投递数" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.applyCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止日期" width="110" />
        <el-table-column prop="publishTime" label="发布时间" width="110" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'" size="small">
              {{ row.status === 'published' ? '招聘中' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetailDialog(row)">查看</el-button>
            <el-button type="warning" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'published'"
              type="danger" link size="small"
              @click="handlePause(row)">下架
            </el-button>
            <el-button
              v-else
              type="success" link size="small"
              @click="handlePublish(row)">上架
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
        style="margin-top: 16px; justify-content: center;"
      />
    </el-card>

    <!-- 职位详情对话框 -->
    <el-dialog v-model="detailVisible" title="职位详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentJob">
        <el-descriptions-item label="职位名称" :span="2">{{ currentJob.jobName }}</el-descriptions-item>
        <el-descriptions-item label="职位类别">{{ currentJob.jobCategory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工作城市">{{ currentJob.workCity || '-' }}</el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="2">{{ currentJob.workAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="薪资范围" :span="2">
          <span class="salary-text">
            {{ currentJob.salaryMin ? currentJob.salaryMin + 'k' : '--' }} ~ {{ currentJob.salaryMax ? currentJob.salaryMax + 'k' : '--' }} /月
            {{ currentJob.salaryMonths ? '(' + currentJob.salaryMonths + '薪)' : '' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="招聘人数">{{ currentJob.recruitNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学历要求">{{ currentJob.educationRequired || '-' }}</el-descriptions-item>
        <el-descriptions-item label="经验要求">{{ currentJob.experienceRequired || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工作性质">{{ currentJob.jobType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="远程办公">{{ currentJob.isRemote === '1' ? '支持' : '不支持' }}</el-descriptions-item>
        <el-descriptions-item label="职位诱惑" :span="2">{{ currentJob.isHighSalary === '1' ? '高薪职位' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="截止日期">{{ currentJob.deadline || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ currentJob.publishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="浏览量">{{ currentJob.viewCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="投递数">{{ currentJob.applyCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="职位要求" :span="2">
          <div class="rich-text" v-html="formatText(currentJob.requirement)"></div>
        </el-descriptions-item>
        <el-descriptions-item label="岗位职责" :span="2">
          <div class="rich-text" v-html="formatText(currentJob.responsibility)"></div>
        </el-descriptions-item>
        <el-descriptions-item label="福利待遇" :span="2">
          <div class="rich-text" v-html="formatText(currentJob.benefits)"></div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openEditDialog(currentJob); detailVisible = false">编辑职位</el-button>
      </template>
    </el-dialog>

    <!-- 创建/编辑职位对话框 -->
    <el-dialog
      v-model="formVisible"
      :title="isEdit ? '编辑职位' : '发布新职位'"
      width="780px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="100px" :rules="formRules" ref="formRef" class="job-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="职位名称" prop="jobName">
              <el-input v-model="form.jobName" placeholder="如：Java开发工程师" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位类别" prop="jobCategory">
              <el-select v-model="form.jobCategory" placeholder="请选择" clearable style="width:100%;">
                <el-option label="技术" value="技术" />
                <el-option label="产品" value="产品" />
                <el-option label="设计" value="设计" />
                <el-option label="运营" value="运营" />
                <el-option label="市场" value="市场" />
                <el-option label="销售" value="销售" />
                <el-option label="职能" value="职能" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作城市" prop="workCity">
              <el-input v-model="form.workCity" placeholder="如：北京" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="详细地址">
              <el-input v-model="form.workAddress" placeholder="如：中关村软件园" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最低薪资(k)">
              <el-input-number v-model="form.salaryMin" :min="0" :max="999" placeholder="如：10" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最高薪资(k)">
              <el-input-number v-model="form.salaryMax" :min="0" :max="999" placeholder="如：20" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="招聘人数">
              <el-input-number v-model="form.recruitNumber" :min="1" :max="999" placeholder="如：5" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作性质">
              <el-select v-model="form.jobType" placeholder="请选择" clearable style="width:100%;">
                <el-option label="全职" value="全职" />
                <el-option label="兼职" value="兼职" />
                <el-option label="实习" value="实习" />
                <el-option label="外包" value="外包" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学历要求">
              <el-select v-model="form.educationRequired" placeholder="请选择" clearable style="width:100%;">
                <el-option label="不限" value="不限" />
                <el-option label="大专" value="大专" />
                <el-option label="本科" value="本科" />
                <el-option label="硕士" value="硕士" />
                <el-option label="博士" value="博士" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="经验要求">
              <el-select v-model="form.experienceRequired" placeholder="请选择" clearable style="width:100%;">
                <el-option label="不限" value="不限" />
                <el-option label="应届生" value="应届生" />
                <el-option label="1年以内" value="1年以内" />
                <el-option label="1-3年" value="1-3年" />
                <el-option label="3-5年" value="3-5年" />
                <el-option label="5年以上" value="5年以上" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="截止日期">
              <el-date-picker v-model="form.deadline" type="date" placeholder="选择日期"
                value-format="YYYY-MM-DD" style="width:100%;" :disabled-date="disabledDate" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发薪月份">
              <el-select v-model="form.salaryMonths" placeholder="请选择" clearable style="width:100%;">
                <el-option label="12薪" value="12" />
                <el-option label="13薪" value="13" />
                <el-option label="14薪" value="14" />
                <el-option label="15薪" value="15" />
                <el-option label="16薪" value="16" />
                <el-option label="按月发放" value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="远程办公">
              <el-radio-group v-model="form.isRemote">
                <el-radio label="1">支持</el-radio>
                <el-radio label="0">不支持</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="技能要求">
              <el-input v-model="form.skillRequired" type="textarea" :rows="2"
                placeholder="请输入技能要求，多个技能用逗号分隔，如：Java, Spring Boot, MySQL" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="职位要求">
              <el-input v-model="form.requirement" type="textarea" :rows="3" placeholder="请输入职位要求" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="岗位职责">
              <el-input v-model="form.responsibility" type="textarea" :rows="3" placeholder="请输入岗位职责" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="福利待遇">
              <el-input v-model="form.benefits" type="textarea" :rows="3" placeholder="请输入福利待遇，如：五险一金、带薪年假、节日福利、弹性工作" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存修改' : '立即发布' }}
        </el-button>
      </template>
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
import { Loading, Plus, Search, Briefcase, CircleCheck, VideoPause, Tickets, RefreshRight } from '@element-plus/icons-vue'
import { companyApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const jobList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const keyword = ref('')
const statusFilter = ref('')
const authStatus = ref('approved')

const stats = ref({ total: 0, published: 0, paused: 0, totalApplications: 0 })

const detailVisible = ref(false)
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const currentJob = ref(null)
const formRef = ref(null)

const defaultForm = () => ({
  jobName: '',
  jobCategory: '',
  jobType: '',
  workCity: '',
  workAddress: '',
  salaryMin: null,
  salaryMax: null,
  salaryMonths: '',
  recruitNumber: null,
  requirement: '',
  responsibility: '',
  benefits: '',
  educationRequired: '',
  experienceRequired: '',
  skillRequired: '',
  isRemote: '0',
  deadline: ''
})

const form = reactive(defaultForm())

const formRules = {
  jobName: [{ required: true, message: '请输入职位名称', trigger: 'blur' }],
  workCity: [{ required: true, message: '请输入工作城市', trigger: 'blur' }]
}

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

function disabledDate(date) {
  return date < new Date(new Date().setHours(0, 0, 0, 0))
}

function formatText(text) {
  if (!text) return '-'
  return text.replace(/\n/g, '<br/>')
}

async function loadJobs() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize,
      keyword: keyword.value,
      status: statusFilter.value
    }
    const res = await companyApi.getJobs(params)
    const page = res || {}
    jobList.value = Array.isArray(page.records) ? page.records : []
    total.value = page.total || 0

    // 统计
    const statsRes = await companyApi.getJobs({ page: 1, size: 1000 })
    const allJobs = Array.isArray(statsRes?.records) ? statsRes.records : []
    stats.value.total = allJobs.length
    stats.value.published = allJobs.filter(j => j.status === 'published').length
    stats.value.paused = allJobs.filter(j => j.status === 'paused').length
    stats.value.totalApplications = allJobs.reduce((sum, j) => sum + (j.applyCount || 0), 0)
  } catch (e) {
    console.error('加载职位列表失败', e)
    jobList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  currentPage.value = page
  loadJobs()
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

function openCreateDialog() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  formVisible.value = true
}

function openDetailDialog(row) {
  currentJob.value = { ...row }
  detailVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.keys(form).forEach(key => {
    if (row[key] !== undefined && row[key] !== null) {
      form[key] = row[key]
    }
  })
  currentJob.value = row
  formVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await companyApi.updateJob(currentJob.value.id, { ...form })
      ElMessage.success('职位信息已更新')
    } else {
      await companyApi.createJob({ ...form })
      ElMessage.success('职位发布成功')
    }
    formVisible.value = false
    currentPage.value = 1
    loadJobs()
  } catch (e) {
    console.error('提交失败', e)
  } finally {
    submitting.value = false
  }
}

async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(`确认上架职位「${row.jobName}」？`, '上架确认', {
      confirmButtonText: '确认上架',
      cancelButtonText: '取消',
      type: 'info'
    })
    await companyApi.publishJob(row.id)
    ElMessage.success('职位已上架')
    loadJobs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handlePause(row) {
  try {
    await ElMessageBox.confirm(`确认下架职位「${row.jobName}」？`, '下架确认', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await companyApi.pauseJob(row.id)
    ElMessage.success('职位已下架')
    loadJobs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除职位「${row.jobName}」？此操作不可恢复！`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    await companyApi.deleteJob(row.id)
    ElMessage.success('职位已删除')
    loadJobs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadJobs()
  loadAuthStatus()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }

.stats-row { margin-bottom: 16px; }
.stat-inner { display: flex; align-items: center; gap: 14px; }
.stat-icon { font-size: 28px; }
.stat-text { flex: 1; }
.stat-label { font-size: 13px; color: #606266; margin-bottom: 4px; }
.stat-value { font-size: 22px; font-weight: 700; line-height: 1.2; }
.blue .stat-icon { color: #409eff; }
.green .stat-icon { color: #67c23a; }
.orange .stat-icon { color: #e6a23c; }
.purple .stat-icon { color: #9c27b0; }

.list-card { margin-top: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 10px; align-items: center; }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }

.job-name-link { color: #409eff; cursor: pointer; font-weight: 500; }
.job-name-link:hover { text-decoration: underline; }
.salary-text { color: #67c23a; font-weight: 600; }
.rich-text { line-height: 1.8; color: #606266; font-size: 13px; white-space: pre-wrap; }
</style>
