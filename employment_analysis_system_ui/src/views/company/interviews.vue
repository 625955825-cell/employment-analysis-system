<template>
  <div class="page-container">
    <div class="page-header">
      <h2>面试管理</h2>
    </div>

    <div v-if="authStatus === 'rejected'" style="margin-bottom: 16px;">
      <el-alert
        title="您的企业入驻申请已被驳回，暂时无法管理面试"
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
      title="您的企业入驻申请正在审核中，暂时无法管理面试，请耐心等待"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 16px;"
    />

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-inner blue">
            <el-icon class="stat-icon"><Calendar /></el-icon>
            <div class="stat-text">
              <p class="stat-label">全部面试</p>
              <p class="stat-value">{{ stats.total }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-inner orange">
            <el-icon class="stat-icon"><Clock /></el-icon>
            <div class="stat-text">
              <p class="stat-label">待确认</p>
              <p class="stat-value">{{ stats.pending }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-inner green">
            <el-icon class="stat-icon"><CircleCheck /></el-icon>
            <div class="stat-text">
              <p class="stat-label">已接受</p>
              <p class="stat-value">{{ stats.accepted }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-inner red">
            <el-icon class="stat-icon"><CloseBold /></el-icon>
            <div class="stat-text">
              <p class="stat-label">已拒绝/已取消</p>
              <p class="stat-value">{{ stats.other }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待安排面试的学生列表（从简历投递中选择） -->
    <el-card shadow="hover" class="section-card">
      <template #header>
        <div class="card-header">
          <span>发送面试邀请</span>
          <span style="font-size:12px;color:#909399;">从已收到的简历中选择学生发送面试邀请</span>
        </div>
      </template>

      <div v-if="loadingResumes" class="loading-state">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <span>加载投递记录...</span>
      </div>

      <el-table v-else-if="pendingApplications.length > 0" :data="pendingApplications" stripe border size="small">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="realName" label="姓名" width="90" />
        <el-table-column prop="jobName" label="应聘职位" width="140" show-overflow-tooltip />
        <el-table-column prop="deptName" label="院系" min-width="120" show-overflow-tooltip />
        <el-table-column prop="majorName" label="专业" min-width="120" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="简历" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.resumeName" type="success" size="small">有</el-tag>
            <el-tag v-else type="info" size="small">无</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openInviteDialog(row)">邀请面试</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="暂无待处理的投递记录，请先等待学生投递简历" />
    </el-card>

    <!-- 面试安排列表 -->
    <el-card shadow="hover" class="section-card">
      <template #header>
        <div class="card-header">
          <span>面试安排</span>
          <div class="header-actions">
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:130px;" @change="loadInterviews">
              <el-option label="全部" value="" />
              <el-option label="待确认" value="pending" />
              <el-option label="已接受" value="accepted" />
              <el-option label="已拒绝" value="rejected" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-empty v-else-if="interviewList.length === 0" description="暂无面试安排" />

      <el-table v-else :data="interviewList" stripe border>
        <el-table-column prop="studentName" label="学生姓名" width="100" />
        <el-table-column prop="jobName" label="应聘职位" width="140" show-overflow-tooltip />
        <el-table-column prop="interviewTime" label="面试时间" width="160">
          <template #default="{ row }">
            <span v-if="row.interviewTime" class="interview-time">{{ row.interviewTime }}</span>
            <span v-else style="color:#999;">待定</span>
          </template>
        </el-table-column>
        <el-table-column prop="interviewAddress" label="面试地点" min-width="160" show-overflow-tooltip />
        <el-table-column prop="interviewNote" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发送时间" width="160" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              type="danger" link size="small"
              @click="handleCancel(row)">取消
            </el-button>
            <el-button
              v-if="row.status === 'confirmed' || row.status === 'completed'"
              type="primary" link size="small"
              @click="openFeedback(row)">录入反馈
            </el-button>
            <el-button v-else-if="row.status === 'cancelled'" type="info" link size="small" disabled>已取消</el-button>
            <span v-else style="color:#999;font-size:12px;">-</span>
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

    <!-- 发送面试邀请对话框 -->
    <el-dialog v-model="inviteVisible" title="发送面试邀请" width="550px" destroy-on-close>
      <el-form :model="inviteForm" label-width="100px" :rules="inviteRules" ref="inviteFormRef">
        <el-form-item label="学生姓名">
          <el-input v-model="inviteForm.studentName" disabled />
        </el-form-item>
        <el-form-item label="应聘职位">
          <el-input v-model="inviteForm.jobName" disabled />
        </el-form-item>
        <el-form-item label="面试时间" prop="interviewTime">
          <el-date-picker
            v-model="inviteForm.interviewTime"
            type="datetime"
            placeholder="选择面试时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:100%;"
          />
        </el-form-item>
        <el-form-item label="面试地点" prop="interviewAddress">
          <el-input v-model="inviteForm.interviewAddress" placeholder="请输入面试地点或线上会议链接" />
        </el-form-item>
        <el-form-item label="面试方式">
          <el-select v-model="inviteForm.interviewType" placeholder="请选择" style="width:100%;">
            <el-option label="线下面试" value="offline" />
            <el-option label="线上面试" value="online" />
            <el-option label="电话面试" value="phone" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="inviteForm.contactPerson" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="inviteForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inviteForm.interviewNote" type="textarea" :rows="3"
            placeholder="可填写面试注意事项、需准备的材料等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSendInvite" :loading="sending">发送邀请</el-button>
      </template>
    </el-dialog>

    <!-- 录入面试反馈对话框 -->
    <el-dialog v-model="feedbackDialogVisible" title="录入面试反馈" width="500px" destroy-on-close>
      <el-form :model="feedbackForm" label-width="100px">
        <el-form-item label="学生姓名">
          <el-input v-model="feedbackForm.studentName" disabled />
        </el-form-item>
        <el-form-item label="面试结果">
          <el-select v-model="feedbackForm.interviewResult" placeholder="请选择" style="width:100%;">
            <el-option label="通过" value="通过" />
            <el-option label="不通过" value="不通过" />
            <el-option label="待定" value="待定" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试评分">
          <el-rate v-model="feedbackForm.score" allow-half show-text :texts="['很差','较差','一般','较好','很好']" />
        </el-form-item>
        <el-form-item label="面试反馈">
          <el-input v-model="feedbackForm.interviewFeedback" type="textarea" :rows="4"
            placeholder="请输入面试反馈，如：表现优秀/沟通能力有待提升..." />
        </el-form-item>
        <el-form-item label="企业备注">
          <el-input v-model="feedbackForm.companyRemark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackLoading" @click="submitFeedback">保存反馈</el-button>
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
import { Loading, Calendar, Clock, CircleCheck, CloseBold, RefreshRight } from '@element-plus/icons-vue'
import { companyApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const loadingResumes = ref(false)
const sending = ref(false)
const authStatus = ref('approved')

const interviewList = ref([])
const pendingApplications = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const statusFilter = ref('')

const stats = ref({ total: 0, pending: 0, accepted: 0, other: 0 })

const inviteVisible = ref(false)
const inviteFormRef = ref(null)
const inviteForm = reactive({
  applicationId: null,
  studentId: null,
  studentName: '',
  jobName: '',
  interviewTime: '',
  interviewAddress: '',
  interviewType: '',
  contactPerson: '',
  contactPhone: '',
  interviewNote: ''
})

const inviteRules = {
  interviewTime: [{ required: true, message: '请选择面试时间', trigger: 'change' }],
  interviewAddress: [{ required: true, message: '请输入面试地点', trigger: 'blur' }]
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

const feedbackDialogVisible = ref(false)
const feedbackLoading = ref(false)
const feedbackForm = reactive({
  studentName: '', interviewResult: '', score: 0,
  interviewFeedback: '', companyRemark: ''
})

function getStatusType(status) {
  const map = {
    'pending': 'warning',
    'accepted': 'success',
    'rejected': 'danger',
    'cancelled': 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    'pending': '待确认',
    'accepted': '已接受',
    'rejected': '已拒绝',
    'cancelled': '已取消'
  }
  return map[status] || status
}

async function loadInterviews() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize,
      status: statusFilter.value
    }
    const res = await companyApi.getInterviews(params)
    const page = res || {}
    interviewList.value = Array.isArray(page.records) ? page.records : []
    total.value = page.total || 0

    // 统计
    const allRes = await companyApi.getInterviews({ page: 1, size: 1000 })
    const all = Array.isArray(allRes?.records) ? allRes.records : []
    stats.value.total = all.length
    stats.value.pending = all.filter(i => i.status === 'pending').length
    stats.value.accepted = all.filter(i => i.status === 'accepted').length
    stats.value.other = all.filter(i => ['rejected', 'cancelled'].includes(i.status)).length
  } catch (e) {
    console.error('加载面试列表失败', e)
    interviewList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadPendingApplications() {
  loadingResumes.value = true
  try {
    const res = await companyApi.getReceivedResumes({ page: 1, size: 1000 })
    const page = res || {}
    const all = Array.isArray(page.records) ? page.records : []
    pendingApplications.value = all.filter(a => ['pending', 'reviewing'].includes(a.status))
  } catch (e) {
    pendingApplications.value = []
  } finally {
    loadingResumes.value = false
  }
}

function openInviteDialog(row) {
  inviteForm.applicationId = row.id
  inviteForm.studentId = row.studentId
  inviteForm.studentName = row.realName
  inviteForm.jobName = row.jobName
  inviteForm.interviewTime = ''
  inviteForm.interviewAddress = ''
  inviteForm.interviewType = ''
  inviteForm.contactPerson = ''
  inviteForm.contactPhone = ''
  inviteForm.interviewNote = ''
  inviteVisible.value = true
}

async function handleSendInvite() {
  const valid = await inviteFormRef.value.validate().catch(() => false)
  if (!valid) return

  sending.value = true
  try {
    await companyApi.createInterview({
      applicationId: inviteForm.applicationId,
      studentId: inviteForm.studentId,
      interviewTime: inviteForm.interviewTime,
      interviewAddress: inviteForm.interviewAddress,
      interviewType: inviteForm.interviewType,
      contactPerson: inviteForm.contactPerson,
      contactPhone: inviteForm.contactPhone,
      remark: inviteForm.interviewNote
    })
    ElMessage.success('面试邀请已发送')
    inviteVisible.value = false
    loadInterviews()
    loadPendingApplications()
  } catch (e) {
    console.error('发送面试邀请失败', e)
  } finally {
    sending.value = false
  }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确认取消该面试安排？', '取消确认', {
      confirmButtonText: '确认取消',
      cancelButtonText: '暂不取消',
      type: 'warning'
    })
    await companyApi.cancelInterview(row.id)
    ElMessage.success('面试已取消')
    loadInterviews()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

function handlePageChange(page) {
  currentPage.value = page
  loadInterviews()
}

function loadAuthStatus() {
  companyApi.getHomeStats().then(res => {
    authStatus.value = res?.authStatus || 'none'
  }).catch(() => {})
}

function openFeedback(row) {
  currentInvitationId.value = row.id
  Object.assign(feedbackForm, {
    studentName: row.realName || '',
    interviewResult: '', score: 0, interviewFeedback: '', companyRemark: ''
  })
  feedbackDialogVisible.value = true
}

const currentInvitationId = ref(null)

function submitFeedback() {
  feedbackLoading.value = true
  companyApi.addInterviewRecord({
    invitationId: currentInvitationId.value,
    ...feedbackForm
  }).then(() => {
    ElMessage.success('反馈已录入')
    feedbackDialogVisible.value = false
    loadInterviews()
  }).catch(err => {
    ElMessage.error(err.message || '录入失败')
  }).finally(() => {
    feedbackLoading.value = false
  })
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
  loadInterviews()
  loadPendingApplications()
  loadAuthStatus()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }

.stats-row { margin-bottom: 16px; }
.stat-inner { display: flex; align-items: center; gap: 14px; }
.stat-icon { font-size: 28px; }
.stat-text { flex: 1; }
.stat-label { font-size: 13px; color: #606266; margin-bottom: 4px; }
.stat-value { font-size: 22px; font-weight: 700; line-height: 1.2; }
.blue .stat-icon { color: #409eff; }
.orange .stat-icon { color: #e6a23c; }
.green .stat-icon { color: #67c23a; }
.red .stat-icon { color: #f56c6c; }

.section-card { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 10px; }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 0; color: #999; gap: 12px; }
.interview-time { color: #409eff; font-weight: 600; }
</style>
