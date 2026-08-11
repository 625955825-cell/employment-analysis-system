<template>
  <div class="page-container">
    <div class="page-header">
      <h2>数据查看审批</h2>
    </div>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="待审批" name="pending">
              <template #label>
                <span>待审批 <el-badge :value="pendingList.length" :hidden="pendingList.length === 0" type="warning" /></span>
              </template>
            </el-tab-pane>
            <el-tab-pane label="历史记录" name="history" />
          </el-tabs>
        </div>
      </template>

      <!-- 待审批列表 -->
      <div v-if="activeTab === 'pending'">
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <el-empty v-else-if="pendingList.length === 0" description="暂无待审批的申请" />

        <el-table v-else :data="pendingList" stripe border>
          <el-table-column prop="studentNo" label="学号" width="130" />
          <el-table-column prop="studentName" label="学生姓名" width="100" />
          <el-table-column prop="className" label="班级" width="140" />
          <el-table-column prop="deptName" label="学院" width="140" show-overflow-tooltip />
          <el-table-column prop="requestType" label="申请类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.requestType || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="yearFrom" label="查询年份范围" width="140">
            <template #default="{ row }">
              {{ row.yearFrom || '-' }} {{ row.yearTo ? ` ~ ${row.yearTo}` : '' }}
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="申请理由" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="申请时间" width="160" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">驳回</el-button>
              <el-button type="info" size="small" link @click="handleDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 历史记录 -->
      <div v-else>
        <div class="filter-bar">
          <el-select v-model="filterStatus" placeholder="审核状态" style="width: 150px;" clearable @change="handleFilterChange">
            <el-option label="全部" value="" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </div>

        <div v-if="historyLoading" class="loading-state">
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <el-empty v-else-if="filteredHistory.length === 0" description="暂无历史记录" />

        <el-table v-else :data="paginatedHistory" stripe border>
          <el-table-column prop="studentNo" label="学号" width="130" />
          <el-table-column prop="studentName" label="学生姓名" width="100" />
          <el-table-column prop="className" label="班级" width="140" />
          <el-table-column prop="deptName" label="学院" width="140" show-overflow-tooltip />
          <el-table-column prop="requestType" label="申请类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.requestType || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="yearFrom" label="年份范围" width="140">
            <template #default="{ row }">
              {{ row.yearFrom || '-' }} {{ row.yearTo ? ` ~ ${row.yearTo}` : '' }}
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="申请理由" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="审核状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'approved' ? 'success' : 'danger'" size="small">
                {{ row.status === 'approved' ? '已通过' : '已驳回' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="auditTime" label="审核时间" width="160" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="info" size="small" link @click="handleDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="filteredHistory.length > 0"
          class="pagination"
          background
          layout="total, prev, pager, next"
          :total="filteredHistory.length"
          :page-size="pageSize"
          v-model:current-page="currentPage"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="申请详情" width="550px" destroy-on-close>
      <el-descriptions :column="1" border v-if="selectedRecord">
        <el-descriptions-item label="学号">{{ selectedRecord.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="学生姓名">{{ selectedRecord.studentName }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ selectedRecord.className }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ selectedRecord.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请类型">{{ selectedRecord.requestType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="查询年份范围">
          {{ selectedRecord.yearFrom || '-' }} {{ selectedRecord.yearTo ? ` ~ ${selectedRecord.yearTo}` : '' }}
        </el-descriptions-item>
        <el-descriptions-item label="申请理由">{{ selectedRecord.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ selectedRecord.createTime }}</el-descriptions-item>
        <el-descriptions-item label="审核状态" v-if="activeTab === 'history'">
          <el-tag :type="selectedRecord.status === 'approved' ? 'success' : 'danger'" size="small">
            {{ selectedRecord.status === 'approved' ? '已通过' : '已驳回' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核备注" v-if="activeTab === 'history'">
          {{ selectedRecord.auditRemark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="审核时间" v-if="activeTab === 'history'">
          {{ selectedRecord.auditTime || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="activeTab === 'pending'" type="success" @click="handleApprove(selectedRecord)">通过</el-button>
        <el-button v-if="activeTab === 'pending'" type="danger" @click="handleReject(selectedRecord)">驳回</el-button>
      </template>
    </el-dialog>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回原因" width="400px">
      <el-input v-model="rejectRemark" type="textarea" :rows="3" placeholder="请输入驳回原因（可选）" maxlength="200" show-word-limit />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { teacherApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('pending')
const loading = ref(false)
const historyLoading = ref(false)
const pendingList = ref([])
const historyList = ref([])
const detailVisible = ref(false)
const selectedRecord = ref(null)
const rejectVisible = ref(false)
const rejectRemark = ref('')
const pendingRow = ref(null)

const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = 10

const filteredHistory = computed(() => {
  if (!filterStatus.value) return historyList.value
  return historyList.value.filter(r => r.status === filterStatus.value)
})

const paginatedHistory = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredHistory.value.slice(start, start + pageSize)
})

function loadPending() {
  loading.value = true
  teacherApi.getPermissionRequests().then(res => {
    pendingList.value = Array.isArray(res) ? res : []
  }).catch(() => {
    pendingList.value = []
  }).finally(() => {
    loading.value = false
  })
}

function loadHistory() {
  historyLoading.value = true
  teacherApi.getPermissionHistory().then(res => {
    historyList.value = Array.isArray(res) ? res : []
    currentPage.value = 1
  }).catch(() => {
    historyList.value = []
  }).finally(() => {
    historyLoading.value = false
  })
}

function handleTabChange(tab) {
  if (tab === 'pending') {
    loadPending()
  } else {
    loadHistory()
  }
}

function handleFilterChange() {
  currentPage.value = 1
}

function handleDetail(row) {
  selectedRecord.value = row
  detailVisible.value = true
}

function handleApprove(row) {
  ElMessageBox.confirm(`确认通过学生「${row.studentName}」的数据查看申请？`, '审批确认', {
    confirmButtonText: '确认通过',
    cancelButtonText: '取消',
    type: 'success'
  }).then(() => {
    teacherApi.auditPermissionRequest(row.id, 'approve').then(() => {
      ElMessage.success('审批通过')
      detailVisible.value = false
      loadPending()
    }).catch(() => {
      ElMessage.error('操作失败')
    })
  }).catch(() => {})
}

function handleReject(row) {
  pendingRow.value = row
  rejectRemark.value = ''
  rejectVisible.value = true
}

function confirmReject() {
  teacherApi.auditPermissionRequest(pendingRow.value.id, 'reject', rejectRemark.value).then(() => {
    ElMessage.success('已驳回')
    rejectVisible.value = false
    detailVisible.value = false
    loadPending()
  }).catch(() => {
    ElMessage.error('操作失败')
  })
}

onMounted(() => {
  loadPending()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { margin-bottom: 16px; display: flex; gap: 12px; }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
