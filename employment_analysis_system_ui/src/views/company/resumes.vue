<template>
  <div class="page-container">
    <h2>收到的简历</h2>

    <div v-if="authStatus === 'rejected'" style="margin-bottom: 16px;">
      <el-alert
        title="您的企业入驻申请已被驳回，暂时无法处理简历"
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
      title="您的企业入驻申请正在审核中，暂时无法处理简历，请耐心等待"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 16px;"
    />

    <el-card style="margin-top: 16px;">
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="筛选状态" clearable style="width: 140px;">
          <el-option label="待处理" value="pending" />
          <el-option label="查看中" value="reviewing" />
          <el-option label="进入面试" value="interview" />
          <el-option label="拟录用" value="offer" />
          <el-option label="不合适" value="rejected" />
        </el-select>
        <el-button
          v-if="selectedRows.length > 0"
          type="danger"
          plain
          :icon="Delete"
          @click="handleBatchDelete"
        >批量删除 ({{ selectedRows.length }})</el-button>
        <el-button :icon="Refresh" @click="loadResumes">刷新</el-button>
      </div>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="resumes.length === 0" class="empty-state">
        <el-empty description="暂无投递简历" :image-size="80" />
      </div>

      <div v-else>
        <el-table :data="filteredRecords" stripe style="width: 100%" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="40" />
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="deptName" label="学院" min-width="140" />
          <el-table-column prop="majorName" label="专业" min-width="160" />
          <el-table-column prop="jobName" label="应聘职位" min-width="120" />
          <el-table-column prop="resumeName" label="简历名称" min-width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="投递时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="viewDetail(row)">查看简历</el-button>
              <el-button v-if="row.status === 'pending'" type="success" size="small" link @click="handleStatus(row, 'reviewing')">查看</el-button>
              <el-button v-if="row.status === 'reviewing'" type="warning" size="small" link @click="handleStatus(row, 'interview')">邀请面试</el-button>
              <el-button v-if="row.status === 'interview'" type="success" size="small" link @click="handleOffer(row)">发放offer</el-button>
              <el-button v-if="['pending','reviewing','interview'].includes(row.status)" type="danger" size="small" link @click="handleStatus(row, 'rejected')">不合适</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="params.page"
            v-model:page-size="params.size"
            :page-sizes="[10, 20, 30]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadResumes"
            @current-change="loadResumes"
          />
        </div>
      </div>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="简历详情" width="700px" destroy-on-close>
      <div v-if="selectedRecord">
        <el-descriptions :column="2" border style="margin-bottom: 16px;">
          <el-descriptions-item label="学号">{{ selectedRecord.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ selectedRecord.realName }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ selectedRecord.gender === 'male' ? '男' : selectedRecord.gender === 'female' ? '女' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ selectedRecord.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学院">{{ selectedRecord.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ selectedRecord.majorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ selectedRecord.className || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ selectedRecord.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="应聘职位" :span="2">{{ selectedRecord.jobName }}</el-descriptions-item>
          <el-descriptions-item label="投递简历" :span="2">{{ selectedRecord.resumeName }}</el-descriptions-item>
          <el-descriptions-item label="个人简介" :span="2">
            <div style="white-space:pre-wrap;">{{ selectedRecord.personalSummary || '暂无' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="申请信" :span="2">
            <div style="white-space:pre-wrap;">{{ selectedRecord.applyLetter || '暂无' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="企业备注" :span="2">
            {{ selectedRecord.companyRemark || '暂无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="['pending','reviewing','interview'].includes(selectedRecord.status)" style="margin-top: 16px;">
          <el-divider />
          <h4 style="margin-bottom:12px;">更新状态</h4>
          <el-select v-model="newStatus" placeholder="选择新状态" style="width: 200px; margin-right: 12px;">
            <el-option label="查看中" value="reviewing" />
            <el-option label="进入面试" value="interview" />
            <el-option label="拟录用" value="offer" />
            <el-option label="不合适" value="rejected" />
          </el-select>
          <el-button type="primary" :loading="statusLoading" @click="submitStatus">确认更新</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="interviewDialogVisible" title="邀请面试" width="500px" destroy-on-close>
      <el-form :model="interviewForm" label-width="100px">
        <el-form-item label="应聘职位">
          <span>{{ selectedRecord?.jobName }}</span>
        </el-form-item>
        <el-form-item label="候选人">
          <span>{{ selectedRecord?.realName }}</span>
        </el-form-item>
        <el-form-item label="面试时间" required>
          <el-date-picker v-model="interviewForm.interviewTime" type="datetime" placeholder="选择时间"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="面试地点">
          <el-input v-model="interviewForm.interviewAddress" placeholder="如：深圳市南山区科技园" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="interviewForm.interviewNote" type="textarea" :rows="3" placeholder="面试须知等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="interviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="interviewLoading" @click="submitInterview">发送邀请</el-button>
      </template>
    </el-dialog>

    <!-- 发放Offer对话框 -->
    <el-dialog v-model="offerDialogVisible" title="发放Offer" width="500px" destroy-on-close>
      <el-form :model="offerForm" label-width="100px">
        <el-form-item label="应聘职位">
          <span>{{ selectedRecord?.jobName }}</span>
        </el-form-item>
        <el-form-item label="候选人">
          <span>{{ selectedRecord?.realName }}</span>
        </el-form-item>
        <el-form-item label="岗位名称">
          <el-input v-model="offerForm.positionName" placeholder="如：Java开发工程师" />
        </el-form-item>
        <el-form-item label="薪资">
          <el-input v-model="offerForm.salary" placeholder="如：15000-20000元/月" />
        </el-form-item>
        <el-form-item label="工作城市">
          <el-input v-model="offerForm.workCity" placeholder="如：深圳" />
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker v-model="offerForm.startDate" type="date" placeholder="预计入职日期"
            value-format="YYYY-MM-DD" style="width:100%;" />
        </el-form-item>
        <el-form-item label="试用期">
          <el-select v-model="offerForm.probationPeriod" placeholder="请选择" style="width:100%;">
            <el-option label="1个月" value="1个月" />
            <el-option label="2个月" value="2个月" />
            <el-option label="3个月" value="3个月" />
            <el-option label="6个月" value="6个月" />
          </el-select>
        </el-form-item>
        <el-form-item label="试用期薪资">
          <el-input v-model="offerForm.probationSalary" placeholder="如：12000元/月" />
        </el-form-item>
        <el-form-item label="回复截止">
          <el-date-picker v-model="offerForm.responseDeadline" type="date" placeholder="学生回复截止日期"
            value-format="YYYY-MM-DD" style="width:100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="offerLoading" @click="submitOffer">确认发放</el-button>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { Refresh, Loading, RefreshRight, Delete } from '@element-plus/icons-vue'
import { companyApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const resumes = ref([])
const total = ref(0)
const params = reactive({ page: 1, size: 10 })
const filterStatus = ref('')
const authStatus = ref('approved')
const selectedRows = ref([])

const detailDialogVisible = ref(false)
const selectedRecord = ref(null)
const newStatus = ref('')
const statusLoading = ref(false)

const interviewDialogVisible = ref(false)
const interviewForm = reactive({
  interviewTime: '',
  interviewAddress: '',
  interviewNote: ''
})
const interviewLoading = ref(false)

const offerDialogVisible = ref(false)
const offerLoading = ref(false)
const offerForm = reactive({
  positionName: '', salary: '', workCity: '',
  startDate: '', probationPeriod: '', probationSalary: '', responseDeadline: ''
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

const statusMap = {
  pending: '待处理',
  reviewing: '查看中',
  interview: '进入面试',
  offer: '拟录用',
  rejected: '不合适',
  accepted: '已接受',
  withdrawn: '已撤回'
}

const statusTagType = (status) => {
  const map = {
    pending: 'info',
    reviewing: 'warning',
    interview: 'primary',
    offer: 'success',
    rejected: 'danger',
    accepted: 'success',
    withdrawn: 'info'
  }
  return map[status] || 'info'
}

const statusText = (status) => statusMap[status] || status || '未知'

const filteredRecords = computed(() => {
  if (!filterStatus.value) return resumes.value
  return resumes.value.filter(r => r.status === filterStatus.value)
})

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try { return dateStr.substring(0, 16).replace('T', ' ') } catch { return dateStr }
}

function loadResumes() {
  loading.value = true
  companyApi.getReceivedResumes(params).then(res => {
    resumes.value = res?.records || []
    total.value = res?.total || 0
  }).catch(() => {
    resumes.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
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

function viewDetail(row) {
  selectedRecord.value = row
  newStatus.value = ''
  detailDialogVisible.value = true
  if (row.status === 'pending') {
    companyApi.updateApplicationStatus(row.id, 'reviewing', null).then(() => {
      row.status = 'reviewing'
      ElMessage.success('已标记为查看')
    }).catch(() => {})
  }
}

function handleStatus(row, status) {
  if (status === 'interview') {
    selectedRecord.value = row
    interviewDialogVisible.value = true
    Object.assign(interviewForm, { interviewTime: '', interviewAddress: '', interviewNote: '' })
  } else if (status === 'offer') {
    handleOffer(row)
  } else {
    companyApi.updateApplicationStatus(row.id, status, null).then(() => {
      row.status = status
      ElMessage.success('状态已更新')
      detailDialogVisible.value = false
    }).catch(err => {
      ElMessage.error(err.message || '操作失败')
    })
  }
}

function submitStatus() {
  if (!newStatus.value) {
    ElMessage.warning('请选择状态')
    return
  }
  statusLoading.value = true
  companyApi.updateApplicationStatus(selectedRecord.value.id, newStatus.value, null).then(() => {
    selectedRecord.value.status = newStatus.value
    resumes.value.find(r => r.id === selectedRecord.value.id).status = newStatus.value
    ElMessage.success('状态已更新')
    detailDialogVisible.value = false
  }).catch(err => {
    ElMessage.error(err.message || '更新失败')
  }).finally(() => {
    statusLoading.value = false
  })
}

function submitInterview() {
  if (!interviewForm.interviewTime) {
    ElMessage.warning('请选择面试时间')
    return
  }
  interviewLoading.value = true
  companyApi.createInterview({
    applicationId: selectedRecord.value.id,
    studentId: selectedRecord.value.studentId,
    interviewTime: interviewForm.interviewTime,
    interviewAddress: interviewForm.interviewAddress,
    remark: interviewForm.interviewNote
  }).then(() => {
    ElMessage.success('面试邀请已发送')
    selectedRecord.value.status = 'interview'
    resumes.value.find(r => r.id === selectedRecord.value.id).status = 'interview'
    interviewDialogVisible.value = false
    detailDialogVisible.value = false
  }).catch(err => {
    ElMessage.error(err.message || '发送失败')
  }).finally(() => {
    interviewLoading.value = false
  })
}

function handleOffer(row) {
  selectedRecord.value = row
  Object.assign(offerForm, {
    positionName: row.jobName || '',
    salary: '', workCity: '', startDate: '',
    probationPeriod: '', probationSalary: '', responseDeadline: ''
  })
  offerDialogVisible.value = true
}

function submitOffer() {
  offerLoading.value = true
  companyApi.sendOffer({
    applicationId: selectedRecord.value.id,
    ...offerForm
  }).then(() => {
    ElMessage.success('Offer已发放')
    offerDialogVisible.value = false
    detailDialogVisible.value = false
    loadResumes()
  }).catch(err => {
    ElMessage.error(err.message || '发放失败')
  }).finally(() => {
    offerLoading.value = false
  })
}

onMounted(() => {
  loadResumes()
  loadAuthStatus()
})

function handleSelectionChange(selection) {
  selectedRows.value = selection
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 条简历投递记录吗？`, '批量删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const ids = selectedRows.value.map(r => r.id)
    await companyApi.deleteApplicationsBatch(ids)
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    loadResumes()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err.message || '删除失败')
  }
}
</script>

<style scoped>
.filter-bar { display: flex; gap: 10px; align-items: center; margin-bottom: 16px; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
