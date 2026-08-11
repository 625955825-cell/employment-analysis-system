<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>我的 Offer</span>
          <span style="font-size:13px;color:#999;">查看收到的录用通知，并决定接受或拒绝</span>
        </div>
      </template>

      <el-table :data="offerList" v-loading="loading" stripe>
        <el-table-column prop="companyName" label="企业名称" min-width="180" />
        <el-table-column prop="jobName" label="岗位" width="150" />
        <el-table-column prop="positionName" label="岗位名称" width="150" />
        <el-table-column prop="salary" label="薪资" width="120" />
        <el-table-column prop="workCity" label="工作城市" width="120" />
        <el-table-column prop="startDate" label="入职日期" width="120" />
        <el-table-column prop="probationPeriod" label="试用期" width="100" />
        <el-table-column prop="probationSalary" label="试用期薪资" width="120" />
        <el-table-column prop="responseDeadline" label="回复截止" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="收到时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button type="success" size="small" @click="handleAccept(row)">接受</el-button>
              <el-button type="danger" size="small" plain @click="handleDecline(row)">拒绝</el-button>
            </template>
            <span v-else style="color:#999;font-size:12px;">-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && offerList.length === 0" description="暂无Offer" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { studentApi } from '@/api'

const offerList = ref([])
const loading = ref(false)

function statusTagType(status) {
  const map = { pending: 'warning', accepted: 'success', declined: 'danger' }
  return map[status] || 'info'
}
function statusText(status) {
  const map = { pending: '待回复', accepted: '已接受', declined: '已拒绝' }
  return map[status] || status
}
function formatTime(ts) {
  if (!ts) return '-'
  try { return ts.substring(0, 19).replace('T', ' ') } catch { return ts }
}

async function loadOffers() {
  loading.value = true
  try {
    const res = await studentApi.getOffers()
    offerList.value = res?.records || []
  } catch (e) {
    offerList.value = []
  } finally {
    loading.value = false
  }
}

async function handleAccept(row) {
  try {
    await ElMessageBox.confirm('确定接受该 Offer 吗？接受后可申请签订三方协议', '接受Offer', { type: 'success' })
    await studentApi.acceptOffer(row.id)
    ElMessage.success('已接受 Offer')
    loadOffers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handleDecline(row) {
  try {
    await ElMessageBox.confirm('确定拒绝该 Offer 吗？', '拒绝Offer', { type: 'warning' })
    await studentApi.declineOffer(row.id)
    ElMessage.success('已拒绝 Offer')
    loadOffers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(() => { loadOffers() })
</script>

<style scoped>
.page-container { padding: 20px; }
</style>
