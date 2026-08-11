<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>三方协议</span>
          <el-button type="primary" size="small" @click="applyDialogVisible = true" v-if="acceptedOffers.length > 0">
            申请签订协议
          </el-button>
          <span v-else style="font-size:13px;color:#999;">接受Offer后即可申请签订三方协议</span>
        </div>
      </template>

      <el-table :data="agreementList" v-loading="loading" stripe>
        <el-table-column prop="companyName" label="企业名称" min-width="180" />
        <el-table-column prop="agreementNo" label="协议编号" min-width="200" />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentSignTime" label="我方签署时间" width="170">
          <template #default="{ row }">{{ formatTime(row.studentSignTime) }}</template>
        </el-table-column>
        <el-table-column prop="companySignTime" label="企业签署时间" width="170">
          <template #default="{ row }">{{ formatTime(row.companySignTime) }}</template>
        </el-table-column>
        <el-table-column prop="schoolSignTime" label="学校签署时间" width="170">
          <template #default="{ row }">{{ formatTime(row.schoolSignTime) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && agreementList.length === 0" description="暂无三方协议" />
    </el-card>

    <!-- 申请签订协议弹窗 -->
    <el-dialog v-model="applyDialogVisible" title="申请签订三方协议" width="500px" destroy-on-close>
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="选择企业">
          <el-select v-model="applyForm.companyId" placeholder="请选择企业" style="width:100%;">
            <el-option v-for="o in acceptedOffers" :key="o.companyId"
              :label="o.companyName + ' - ' + o.jobName" :value="o.companyId" />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false"
          title="申请后将自动完成您的签署，等待企业签署后由学校盖章" style="margin-top:10px;" />
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">确认申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { studentApi } from '@/api'

const agreementList = ref([])
const acceptedOffers = ref([])
const loading = ref(false)
const applyDialogVisible = ref(false)
const applying = ref(false)
const applyForm = ref({ companyId: null })

function statusTagType(status) {
  const map = { pending: 'info', student_signed: 'warning', company_signed: 'primary', all_signed: 'success', completed: 'success' }
  return map[status] || 'info'
}
function statusText(status) {
  const map = { pending: '待签署', student_signed: '学生已签', company_signed: '企业已签', all_signed: '全部签署', completed: '已完成' }
  return map[status] || status
}
function formatTime(ts) {
  if (!ts) return '-'
  try { return ts.substring(0, 19).replace('T', ' ') } catch { return ts }
}

async function loadData() {
  loading.value = true
  try {
    const [agreements, offers] = await Promise.all([
      studentApi.getAgreements(),
      studentApi.getOffers()
    ])
    agreementList.value = agreements?.records || []
    acceptedOffers.value = (offers?.records || []).filter(o => o.status === 'accepted')
  } catch (e) {
    agreementList.value = []
    acceptedOffers.value = []
  } finally {
    loading.value = false
  }
}

async function submitApply() {
  if (!applyForm.value.companyId) {
    ElMessage.warning('请选择企业')
    return
  }
  applying.value = true
  try {
    await studentApi.applyAgreement({ companyId: applyForm.value.companyId })
    ElMessage.success('申请已提交')
    applyDialogVisible.value = false
    loadData()
  } catch (err) {
    ElMessage.error(err.message || '提交失败')
  } finally {
    applying.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { padding: 20px; }
</style>
