<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>Offer 管理</span>
          <span style="font-size:13px;color:#999;">发放/撤回 Offer，学生接受后可用于签订三方协议</span>
        </div>
      </template>

      <el-table :data="offerList" v-loading="loading" stripe>
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="positionName" label="岗位" width="150" />
        <el-table-column prop="salary" label="薪资" width="120" />
        <el-table-column prop="workCity" label="工作城市" width="120" />
        <el-table-column prop="startDate" label="入职日期" width="120" />
        <el-table-column prop="responseDeadline" label="回复截止" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发放时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button type="danger" size="small" plain
              v-if="row.status === 'pending'"
              @click="handleWithdraw(row)">撤回</el-button>
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

const offerList = ref([])
const loading = ref(false)

function statusTagType(status) {
  const map = { pending: 'warning', accepted: 'success', declined: 'danger', withdrawn: 'info' }
  return map[status] || 'info'
}
function statusText(status) {
  const map = { pending: '待回复', accepted: '已接受', declined: '已拒绝', withdrawn: '已撤回' }
  return map[status] || status
}
function formatTime(ts) {
  if (!ts) return '-'
  try { return ts.substring(0, 19).replace('T', ' ') } catch { return ts }
}

async function loadOffers() {
  loading.value = true
  try {
    const res = await companyApi.getOffers({})
    offerList.value = res?.records || []
  } catch (e) {
    offerList.value = []
  } finally {
    loading.value = false
  }
}

async function handleWithdraw(row) {
  try {
    await ElMessageBox.confirm('确定撤回该 Offer 吗？撤回后学生将无法再接受', '撤回确认', { type: 'warning' })
    await companyApi.withdrawOffer(row.id)
    ElMessage.success('已撤回')
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
