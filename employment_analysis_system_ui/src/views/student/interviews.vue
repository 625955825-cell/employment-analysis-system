<template>
  <div class="page-container">
    <h2>面试邀约</h2>

    <el-tabs v-model="activeTab" class="interview-tabs">
      <el-tab-pane label="面试邀请" name="interviews">
        <el-card style="margin-top: 16px;">
          <div v-if="loading" class="loading-state">
            <el-icon class="is-loading" :size="28"><Loading /></el-icon>
            <span>加载中...</span>
          </div>

          <div v-else-if="records.length === 0" class="empty-state">
            <el-empty description="暂无面试邀约" />
          </div>

          <div v-else>
            <el-table :data="records" stripe style="width: 100%">
              <el-table-column prop="jobName" label="职位名称" min-width="160">
                <template #default="{ row }">
                  <span class="job-link" @click="$router.push(`/student/job-detail/${row.jobId}`)">{{ row.jobName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="companyName" label="企业名称" min-width="160" />
              <el-table-column prop="interviewTime" label="面试时间" width="160">
                <template #default="{ row }">
                  {{ formatDateTime(row.interviewTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="interviewType" label="面试方式" width="100" />
              <el-table-column prop="interviewAddress" label="面试地点" min-width="160" show-overflow-tooltip />
              <el-table-column prop="contactPerson" label="联系人" width="100" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <template v-if="row.status === 'pending'">
                    <el-button type="success" size="small" link @click="handleAccept(row)">接受</el-button>
                    <el-button type="danger" size="small" link @click="handleReject(row)">拒绝</el-button>
                  </template>
                  <el-button type="primary" size="small" link v-else @click="$router.push(`/student/job-detail/${row.jobId}`)">查看详情</el-button>
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
                @size-change="loadData"
                @current-change="loadData"
              />
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="录用通知" name="offers">
        <el-card style="margin-top: 16px;">
          <div style="margin-bottom:12px;">
            <el-button type="primary" size="small" @click="$router.push('/student/offers')">查看我的Offer</el-button>
          </div>
          <div v-if="offers.length === 0" class="empty-state">
            <el-empty description="暂无录用通知" />
          </div>
          <div v-else>
            <el-table :data="offers" stripe style="width: 100%">
              <el-table-column prop="positionName" label="录用职位" min-width="160">
                <template #default="{ row }">
                  <span class="job-link" @click="$router.push(`/student/job-detail/${row.jobId}`)">{{ row.positionName || row.jobName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="companyName" label="企业名称" min-width="160" />
              <el-table-column prop="salary" label="薪资" width="140">
                <template #default="{ row }">
                  <span style="color:#f56c6c;font-weight:600;">{{ row.salary || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="workCity" label="工作城市" width="100" />
              <el-table-column prop="startDate" label="入职日期" width="120" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="offerStatusTagType(row.status)">{{ offerStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <template v-if="row.status === 'pending'">
                    <el-button type="success" size="small" link @click="handleAcceptOffer(row)">接受</el-button>
                    <el-button type="danger" size="small" link @click="handleRejectOffer(row)">拒绝</el-button>
                  </template>
                  <el-button type="primary" size="small" link v-else @click="$router.push(`/student/job-detail/${row.jobId}`)">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { studentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('interviews')
const loading = ref(false)
const records = ref([])
const offers = ref([])
const total = ref(0)
const params = reactive({ page: 1, size: 10 })

const statusMap = {
  pending: '待确认',
  confirmed: '已确认',
  cancelled: '已取消',
  completed: '已完成'
}

const statusTagType = (status) => {
  const map = {
    pending: 'warning',
    accepted: 'success',
    confirmed: 'success',
    rejected: 'danger',
    cancelled: 'info',
    completed: ''
  }
  return map[status] || 'info'
}

const statusText = (status) => statusMap[status] || status || '未知'

const offerStatusTagType = (status) => {
  const map = { pending: 'warning', accepted: 'success', declined: 'danger' }
  return map[status] || 'info'
}

const offerStatusText = (status) => {
  const map = { pending: '待回复', accepted: '已接受', declined: '已拒绝' }
  return map[status] || status || '未知'
}

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  try { return dateStr.substring(0, 16).replace('T', ' ') } catch { return dateStr }
}

function loadData() {
  loading.value = true
  studentApi.getMyInterviews().then(res => {
    records.value = res?.records || []
    total.value = res?.total || records.value.length
  }).catch(() => {
    records.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function loadOffers() {
  studentApi.getOffers().then(res => {
    offers.value = res?.records || []
  }).catch(() => {
    offers.value = []
  })
}

function handleAccept(row) {
  ElMessageBox.confirm('确定接受该面试邀约吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    studentApi.acceptInterview(row.id).then(() => {
      ElMessage.success('已接受面试')
      loadData()
    })
  }).catch(() => {})
}

function handleReject(row) {
  ElMessageBox.confirm('确定拒绝该面试邀约吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    studentApi.declineInterview(row.id).then(() => {
      ElMessage.success('已拒绝面试')
      loadData()
    })
  }).catch(() => {})
}

function handleAcceptOffer(row) {
  ElMessageBox.confirm('确定接受该Offer吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    studentApi.acceptOffer(row.id).then(() => {
      ElMessage.success('已接受Offer')
      loadOffers()
    })
  }).catch(() => {})
}

function handleRejectOffer(row) {
  ElMessageBox.confirm('确定拒绝该Offer吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    studentApi.declineOffer(row.id).then(() => {
      ElMessage.success('已拒绝Offer')
      loadOffers()
    })
  }).catch(() => {})
}

onMounted(() => {
  loadData()
  loadOffers()
})
</script>

<style scoped>
.interview-tabs { margin-top: 0; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.job-link { color: #409eff; cursor: pointer; }
.job-link:hover { text-decoration: underline; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
