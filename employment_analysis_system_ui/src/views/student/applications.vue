<template>
  <div class="page-container">
    <h2>投递记录</h2>

    <el-card style="margin-top: 16px;">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="records.length === 0" class="empty-state">
        <el-empty description="暂无投递记录" :image-size="80">
          <el-button type="primary" @click="$router.push('/student/job-search')">去搜索职位</el-button>
        </el-empty>
      </div>

      <div v-else>
        <el-table :data="records" stripe style="width: 100%">
          <el-table-column prop="jobName" label="职位名称" min-width="160">
            <template #default="{ row }">
              <span class="job-link" @click="$router.push(`/student/job-detail/${row.jobId}`)">{{ row.jobName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="workCity" label="工作地点" width="120" />
          <el-table-column prop="status" label="投递状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="readStatus" label="查阅状态" width="100">
            <template #default="{ row }">
              <span :style="{ color: row.readStatus === '1' ? '#67c23a' : '#999' }">
                {{ row.readStatus === '1' ? '已读' : '未读' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="投递时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'pending'"
                type="danger"
                size="small"
                link
                @click="handleCancel(row)"
              >撤回</el-button>
              <el-button
                v-else
                type="primary"
                size="small"
                link
                @click="$router.push(`/student/job-detail/${row.jobId}`)"
              >查看</el-button>
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
            @size-change="loadRecords"
            @current-change="loadRecords"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { studentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const params = reactive({ page: 1, size: 10 })

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

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try { return dateStr.substring(0, 16).replace('T', ' ') } catch { return dateStr }
}

function loadRecords() {
  loading.value = true
  studentApi.getMyApplications(params).then(res => {
    records.value = res?.records || []
    total.value = res?.total || 0
  }).catch(() => {
    records.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function handleCancel(row) {
    ElMessageBox.confirm('确定撤回该投递记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    studentApi.cancelApplication(row.id).then(() => {
      ElMessage.success('撤回成功')
      loadRecords()
    }).catch(err => {
      ElMessage.error(err.message || '撤回失败')
    })
  }).catch(() => {})
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.job-link { color: #409eff; cursor: pointer; }
.job-link:hover { text-decoration: underline; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
