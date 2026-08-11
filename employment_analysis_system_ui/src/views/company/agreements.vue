<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <span>三方协议管理</span>
      </template>

      <el-table :data="agreementList" v-loading="loading" stripe>
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="agreementNo" label="协议编号" min-width="200" />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentSignTime" label="学生签署时间" width="170">
          <template #default="{ row }">{{ formatTime(row.studentSignTime) }}</template>
        </el-table-column>
        <el-table-column prop="companySignTime" label="企业签署时间" width="170">
          <template #default="{ row }">{{ formatTime(row.companySignTime) }}</template>
        </el-table-column>
        <el-table-column prop="schoolSignTime" label="学校签署时间" width="170">
          <template #default="{ row }">{{ formatTime(row.schoolSignTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button type="primary" size="small"
              v-if="row.status === 'pending' || row.status === 'student_signed'"
              @click="handleSign(row)">签署协议</el-button>
            <span v-else style="color:#999;font-size:12px;">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { companyApi } from '@/api'

const agreementList = ref([])
const loading = ref(false)

function statusTagType(status) {
  const map = { pending: 'info', student_signed: 'warning', company_signed: 'primary', all_signed: 'success', completed: 'success' }
  return map[status] || 'info'
}
function statusText(status) {
  const map = { pending: '待学生签署', student_signed: '学生已签', company_signed: '企业已签', all_signed: '全部签署', completed: '已完成' }
  return map[status] || status
}
function formatTime(ts) {
  if (!ts) return '-'
  try { return ts.substring(0, 19).replace('T', ' ') } catch { return ts }
}

async function loadAgreements() {
  loading.value = true
  try {
    const res = await companyApi.getAgreements({})
    agreementList.value = res?.records || []
  } catch (e) {
    agreementList.value = []
  } finally {
    loading.value = false
  }
}

async function handleSign(row) {
  try {
    await ElMessageBox.confirm('确认签署该三方协议？', '签署确认', { type: 'info' })
    await companyApi.signAgreement(row.id)
    ElMessage.success('签署成功')
    loadAgreements()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(() => { loadAgreements() })
</script>

<style scoped>
.page-container { padding: 20px; }
</style>
