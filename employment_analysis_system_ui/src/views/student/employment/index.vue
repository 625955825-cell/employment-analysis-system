<template>
  <div class="page-container">
    <h2>就业去向</h2>

    <el-card style="margin-top: 16px;" v-loading="loading">
      <!-- 三方协议提示区域 -->
      <div v-if="tripartiteAgreements.length > 0" class="tripartite-banner">
        <el-alert type="success" :closable="false" show-icon>
          <template #title>
            <span>您已与 <strong>{{ tripartiteAgreements[0].companyName }}</strong> 签订三方协议</span>
          </template>
        </el-alert>
        <div style="margin-top: 12px;">
          <el-button type="primary" size="small" @click="autoFillFromTripartite">
            <el-icon><MagicStick /></el-icon> 从三方协议自动填入信息
          </el-button>
          <span style="margin-left: 12px; font-size: 12px; color: #909399;">
            协议编号：{{ tripartiteAgreements[0].agreementNo }} &nbsp;|&nbsp;
            状态：<el-tag size="small" type="info">{{ tripartiteStatusText(tripartiteAgreements[0].status) }}</el-tag>
          </span>
        </div>
      </div>

      <el-divider v-if="tripartiteAgreements.length > 0" />

      <div v-if="!hasRecord" class="empty-state">
        <el-empty description="暂无就业记录">
          <el-button type="primary" @click="dialogVisible = true; isEdit = false; resetForm()">填写就业去向</el-button>
        </el-empty>
      </div>

      <div v-else>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="就业类型">{{ record.employmentType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="就业状态">
            <el-tag :type="auditTagType(record.auditStatus)" size="small">{{ auditText(record.auditStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="公司名称">{{ record.companyName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公司规模">{{ record.companyScale || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公司行业">{{ record.companyIndustry || '-' }}</el-descriptions-item>
          <el-descriptions-item label="岗位名称">{{ record.positionName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="工作城市">{{ record.workCity || '-' }}</el-descriptions-item>
          <el-descriptions-item label="工作省份">{{ record.workProvince || '-' }}</el-descriptions-item>
          <el-descriptions-item label="薪资">{{ record.salary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="是否签三方">
            <el-tag :type="record.isThreePartySigned === '1' ? 'success' : 'info'" size="small">
              {{ record.isThreePartySigned === '1' ? '已签署' : '未签署' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="三方协议号" :span="2">{{ record.threePartyNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同开始日期">{{ record.contractStartDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同结束日期">{{ record.contractEndDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="试用期薪资">{{ record.probationSalary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ record.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">{{ record.auditRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(record.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />
        <h4 style="margin-bottom: 12px;">附件材料</h4>
        <div v-if="attachmentsLoading" class="loading-mini">
          <el-icon class="is-loading" :size="16"><Loading /></el-icon>
        </div>
        <div v-else-if="attachments.length === 0" class="empty-mini">暂无附件</div>
        <div v-else class="attachment-list">
          <div v-for="att in attachments" :key="att.id" class="attachment-item">
            <el-icon><Document /></el-icon>
            <span class="attachment-name">{{ att.attachmentName }}</span>
            <el-tag size="small" type="success">{{ att.attachmentType }}</el-tag>
          </div>
        </div>

        <div style="margin-top: 20px; display: flex; gap: 12px;">
          <el-button type="primary" @click="openEdit">编辑就业去向</el-button>
          <el-button type="info" @click="showUploadDialog = true" v-if="hasRecord">上传附件</el-button>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑就业去向' : '填写就业去向'" width="650px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="就业类型" required>
          <el-select v-model="form.employmentType" placeholder="请选择就业类型" style="width: 100%;">
            <el-option label="签订劳动合同" value="签订劳动合同" />
            <el-option label="签订三方协议" value="签订三方协议" />
            <el-option label="应征入伍" value="应征入伍" />
            <el-option label="自主创业" value="自主创业" />
            <el-option label="自由职业" value="自由职业" />
            <el-option label="继续深造" value="继续深造" />
            <el-option label="暂未就业" value="暂未就业" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="公司名称">
          <el-input v-model="form.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="公司规模">
          <el-select v-model="form.companyScale" placeholder="请选择" style="width: 100%;">
            <el-option label="0-20人" value="0-20人" />
            <el-option label="20-99人" value="20-99人" />
            <el-option label="100-499人" value="100-499人" />
            <el-option label="500-999人" value="500-999人" />
            <el-option label="1000人以上" value="1000人以上" />
          </el-select>
        </el-form-item>
        <el-form-item label="公司行业">
          <el-select v-model="form.companyIndustry" placeholder="请选择" style="width: 100%;">
            <el-option label="互联网/IT" value="互联网/IT" />
            <el-option label="金融" value="金融" />
            <el-option label="教育" value="教育" />
            <el-option label="医疗健康" value="医疗健康" />
            <el-option label="制造业" value="制造业" />
            <el-option label="房地产" value="房地产" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位名称">
          <el-input v-model="form.positionName" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="工作城市">
          <el-input v-model="form.workCity" placeholder="如：深圳" />
        </el-form-item>
        <el-form-item label="工作省份">
          <el-input v-model="form.workProvince" placeholder="如：广东省" />
        </el-form-item>
        <el-form-item label="薪资">
          <el-input v-model="form.salary" placeholder="如：8000-12000元/月" />
        </el-form-item>
        <el-form-item label="是否签三方">
          <el-radio-group v-model="form.isThreePartySigned">
            <el-radio value="1">是</el-radio>
            <el-radio value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="三方协议号" v-if="form.isThreePartySigned === '1'">
          <el-input v-model="form.threePartyNo" placeholder="请输入三方协议号" />
        </el-form-item>
        <el-form-item label="合同开始日期">
          <el-date-picker v-model="form.contractStartDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="合同结束日期">
          <el-date-picker v-model="form.contractEndDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="试用期薪资">
          <el-input v-model="form.probationSalary" placeholder="如：8000元/月" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="其他备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showUploadDialog" title="上传附件" width="400px" destroy-on-close>
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="附件类型" required>
          <el-select v-model="uploadForm.attachmentType" placeholder="请选择类型" style="width: 100%;">
            <el-option label="劳动合同" value="劳动合同" />
            <el-option label="三方协议" value="三方协议" />
            <el-option label="Offer" value="Offer" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            accept=".pdf,.doc,.docx,.jpg,.png"
          >
            <el-button type="primary" plain>选择文件</el-button>
          </el-upload>
          <div class="el-upload__tip">支持 pdf/doc/docx/jpg/png 格式</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Loading, Document, MagicStick } from '@element-plus/icons-vue'
import { employmentApi, studentApi } from '@/api'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const hasRecord = ref(false)
const record = ref({})
const form = reactive(getDefaultForm())

const tripartiteAgreements = ref([])
const attachments = ref([])
const attachmentsLoading = ref(false)
const showUploadDialog = ref(false)
const uploadRef = ref(null)
const uploadForm = reactive({ attachmentType: '劳动合同', file: null })

function getDefaultForm() {
  return {
    employmentType: '',
    companyName: '',
    companyScale: '',
    companyIndustry: '',
    positionName: '',
    workCity: '',
    workProvince: '',
    salary: '',
    isThreePartySigned: '0',
    threePartyNo: '',
    contractStartDate: '',
    contractEndDate: '',
    probationSalary: '',
    remark: ''
  }
}

function resetForm() {
  Object.assign(form, getDefaultForm())
}

const auditTagType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}
const auditText = (status) => {
  const map = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status || '-'
}
const tripartiteStatusText = (status) => {
  const map = {
    pending: '待签署',
    student_signed: '学生已签',
    company_signed: '企业已签',
    school_signed: '学校已签',
    completed: '已完成'
  }
  return map[status] || status || '-'
}
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  try { return dateStr.substring(0, 16).replace('T', ' ') } catch { return dateStr }
}

function loadTripartiteAgreements() {
  studentApi.getMyAgreements().then(res => {
    tripartiteAgreements.value = res?.records || []
  }).catch(() => {
    tripartiteAgreements.value = []
  })
}

async function autoFillFromTripartite() {
  if (tripartiteAgreements.value.length === 0) {
    ElMessage.warning('暂无可填入的三方协议信息')
    return
  }
  // 找出最新已完成的三方协议
  const latest = tripartiteAgreements.value.find(a =>
    ['student_signed', 'company_signed', 'school_signed', 'completed'].includes(a.status)
  )
  if (!latest) {
    ElMessage.warning('三方协议尚未签署，无法填入')
    return
  }
  // 打开编辑/新建弹窗
  if (hasRecord.value && record.value.id) {
    // 已有记录则编辑更新
    isEdit.value = true
  } else {
    isEdit.value = false
    resetForm()
  }
  dialogVisible.value = true
  // 从后端获取最新的三方详情（含企业信息）
  request.get('/student/agreement/' + latest.id + '/detail').then(detail => {
    const d = detail || latest
    Object.assign(form, {
      employmentType: '签订三方协议',
      companyName: d.companyName || '',
      companyScale: d.companyScale || '',
      companyIndustry: d.companyIndustry || '',
      positionName: d.positionName || '',
      workCity: d.workCity || '',
      workProvince: d.workProvince || '',
      salary: d.salary || '',
      isThreePartySigned: '1',
      threePartyNo: d.agreementNo || d.threePartyNo || '',
      contractStartDate: '',
      contractEndDate: '',
      probationSalary: '',
      remark: ''
    })
  }).catch(() => {
    // 网络异常时只用列表数据
    Object.assign(form, {
      employmentType: '签订三方协议',
      companyName: latest.companyName || '',
      isThreePartySigned: '1',
      threePartyNo: latest.agreementNo || '',
      workCity: latest.workCity || '',
      workProvince: latest.workProvince || ''
    })
  })
}

function loadRecord() {
  loading.value = true
  employmentApi.getMyRecord().then(res => {
    if (res && res.id) {
      record.value = res
      hasRecord.value = true
      loadAttachments(res.id)
    } else {
      hasRecord.value = false
      attachments.value = []
    }
  }).catch(() => {
    hasRecord.value = false
  }).finally(() => {
    loading.value = false
  })
}

function loadAttachments(employmentId) {
  attachmentsLoading.value = true
  request.get(`/employment/record/${employmentId}/attachments`).then(attRes => {
    attachments.value = Array.isArray(attRes) ? attRes : []
  }).catch(() => { attachments.value = [] }).finally(() => {
    attachmentsLoading.value = false
  })
}

function openEdit() {
  isEdit.value = true
  Object.assign(form, {
    employmentType: record.value.employmentType || '',
    companyName: record.value.companyName || '',
    companyScale: record.value.companyScale || '',
    companyIndustry: record.value.companyIndustry || '',
    positionName: record.value.positionName || '',
    workCity: record.value.workCity || '',
    workProvince: record.value.workProvince || '',
    salary: record.value.salary || '',
    isThreePartySigned: record.value.isThreePartySigned || '0',
    threePartyNo: record.value.threePartyNo || '',
    contractStartDate: record.value.contractStartDate || '',
    contractEndDate: record.value.contractEndDate || '',
    probationSalary: record.value.probationSalary || '',
    remark: record.value.remark || ''
  })
  dialogVisible.value = true
}

function handleSubmit() {
  if (!form.employmentType) {
    ElMessage.warning('请选择就业类型')
    return
  }
  submitting.value = true
  const apiCall = isEdit.value && record.value.id
    ? employmentApi.updateRecord(record.value.id, form)
    : employmentApi.createRecord(form)
  apiCall.then(() => {
    ElMessage.success(isEdit.value ? '更新成功' : '提交成功')
    dialogVisible.value = false
    loadRecord()
  }).catch(err => {
    ElMessage.error(err.message || '操作失败')
  }).finally(() => {
    submitting.value = false
  })
}

function handleFileChange(file) {
  uploadForm.file = file.raw
}

function handleUpload() {
  if (!uploadForm.attachmentType) {
    ElMessage.warning('请选择附件类型')
    return
  }
  if (!uploadForm.file) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  const formData = new FormData()
  formData.append('attachmentType', uploadForm.attachmentType)
  formData.append('file', uploadForm.file)
  employmentApi.uploadAttachment(record.value.id, formData).then(() => {
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    loadRecord()
  }).catch(err => {
    ElMessage.error(err.message || '上传失败')
  }).finally(() => {
    uploading.value = false
  })
}

onMounted(() => {
  loadTripartiteAgreements()
  loadRecord()
})
</script>

<style scoped>
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; }
.loading-mini { display: flex; justify-content: center; padding: 10px; }
.empty-mini { text-align: center; color: #999; padding: 10px; }
.attachment-list { display: flex; flex-direction: column; gap: 8px; }
.attachment-item { display: flex; align-items: center; gap: 8px; padding: 8px; border: 1px solid #ebeef5; border-radius: 6px; }
.attachment-name { flex: 1; font-size: 13px; }
.tripartite-banner { margin-bottom: 16px; }
</style>
