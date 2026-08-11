<template>
  <div class="page-container">
    <div class="page-header">
      <h2>材料审核</h2>
    </div>

    <el-card shadow="hover">
      <el-tabs v-model="activeTab" class="audit-tabs">
        <!-- 待审核标签页 -->
        <el-tab-pane label="待审核" name="pending">
          <div class="tab-header">
            <span style="color:#909399;font-size:13px;">共 <span style="color:#e6a23c;font-weight:700;">{{ pendingList.length }}</span> 条待审核</span>
          </div>

          <div v-if="loading" class="loading-state">
            <el-icon class="is-loading" :size="28"><Loading /></el-icon>
            <span>加载中...</span>
          </div>

          <el-empty v-else-if="pendingList.length === 0" description="暂无待审核的就业记录" />

          <el-table v-else :data="pendingList" stripe border>
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="realName" label="姓名" width="90" />
            <el-table-column prop="className" label="班级" width="130" />
            <el-table-column prop="employmentType" label="就业类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" type="success">{{ row.employmentType || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="companyName" label="就业单位" min-width="140" show-overflow-tooltip />
            <el-table-column prop="workProvince" label="省份" width="70" />
            <el-table-column prop="workCity" label="城市" width="70" />
            <el-table-column prop="positionName" label="岗位" width="90" show-overflow-tooltip />
            <el-table-column prop="salary" label="薪资" width="90">
              <template #default="{ row }">
                <span v-if="row.salary" style="color:#67c23a;font-weight:600;">{{ row.salary }}</span>
                <span v-else style="color:#999;">--</span>
              </template>
            </el-table-column>
            <el-table-column prop="isThreePartySigned" label="三方" width="70">
              <template #default="{ row }">
                <el-tag size="small" :type="row.isThreePartySigned === '1' ? 'success' : 'info'">
                  {{ row.isThreePartySigned === '1' ? '已签' : '未签' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="150" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
                <el-button type="danger" size="small" @click="handleReject(row)">驳回</el-button>
                <el-button type="info" size="small" link @click="handleDetail(row, 'pending')">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 历史记录标签页 -->
        <el-tab-pane name="history">
          <template #label>
            <span>历史记录 <el-badge :value="historyCount" :hidden="historyCount === 0" type="info" /></span>
          </template>
          <div class="tab-header">
            <span style="color:#909399;font-size:13px;">共 <span style="color:#409eff;font-weight:700;">{{ historyList.length }}</span> 条审核记录</span>
            <div class="header-filters">
              <el-select v-model="filterStatus" placeholder="审核状态" size="default" style="width:120px;" clearable>
                <el-option label="全部" value="" />
                <el-option label="已通过" value="approved" />
                <el-option label="已驳回" value="rejected" />
              </el-select>
            </div>
          </div>

          <div v-if="historyLoading" class="loading-state">
            <el-icon class="is-loading" :size="28"><Loading /></el-icon>
            <span>加载中...</span>
          </div>

          <el-empty v-else-if="filteredHistory.length === 0" description="暂无审核历史记录" />

          <el-table v-else :data="pagedHistory" stripe border>
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="realName" label="姓名" width="90" />
            <el-table-column prop="className" label="班级" width="130" />
            <el-table-column prop="employmentType" label="就业类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" type="success">{{ row.employmentType || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="companyName" label="就业单位" min-width="140" show-overflow-tooltip />
            <el-table-column prop="workProvince" label="省份" width="70" />
            <el-table-column prop="workCity" label="城市" width="70" />
            <el-table-column prop="salary" label="薪资" width="90">
              <template #default="{ row }">
                <span v-if="row.salary" style="color:#67c23a;font-weight:600;">{{ row.salary }}</span>
                <span v-else style="color:#999;">--</span>
              </template>
            </el-table-column>
            <el-table-column prop="auditStatus" label="审核结果" width="90">
              <template #default="{ row }">
                <el-tag :type="row.auditStatus === 'approved' ? 'success' : 'danger'" size="small">
                  {{ row.auditStatus === 'approved' ? '已通过' : '已驳回' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="auditTime" label="审核时间" width="150" />
            <el-table-column prop="auditRemark" label="审核备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button type="info" size="small" link @click="handleDetail(row, 'history')">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-wrapper" v-if="filteredHistory.length > pageSize">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="filteredHistory.length"
              layout="total, prev, pager, next"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="selectedRecord">
        <el-descriptions-item label="学号">{{ selectedRecord.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ selectedRecord.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ selectedRecord.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="就业状态">
          <el-tag v-if="selectedRecord.auditStatus === 'approved'" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="selectedRecord.auditStatus === 'rejected'" type="danger" size="small">已驳回</el-tag>
          <el-tag v-else type="warning" size="small">待审核</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="就业类型" :span="2">
          <el-tag type="success" size="small">{{ selectedRecord.employmentType || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="就业单位" :span="2">{{ selectedRecord.companyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公司规模">{{ selectedRecord.companyScale || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公司行业">{{ selectedRecord.companyIndustry || '-' }}</el-descriptions-item>
        <el-descriptions-item label="岗位名称">{{ selectedRecord.positionName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工作省份">{{ selectedRecord.workProvince || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工作城市">{{ selectedRecord.workCity || '-' }}</el-descriptions-item>
        <el-descriptions-item label="薪资">{{ selectedRecord.salary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="试用期薪资">{{ selectedRecord.probationSalary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否签三方">
          <el-tag size="small" :type="selectedRecord.isThreePartySigned === '1' ? 'success' : 'info'">
            {{ selectedRecord.isThreePartySigned === '1' ? '已签署' : '未签署' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="三方协议号">{{ selectedRecord.threePartyNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同开始日期">{{ selectedRecord.contractStartDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同结束日期">{{ selectedRecord.contractEndDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ selectedRecord.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间" :span="2">{{ selectedRecord.auditTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核备注" :span="2">
          <span v-if="selectedRecord.auditRemark" style="color:#f56c6c;">{{ selectedRecord.auditRemark }}</span>
          <span v-else style="color:#999;">--</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ selectedRecord.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectVisible" title="驳回原因" width="450px">
      <el-form>
        <el-form-item label="驳回原因">
          <el-input
            v-model="rejectRemark"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因，以便学生了解需要补充的材料..."
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { teacherApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const historyLoading = ref(false)
const activeTab = ref('pending')
const pendingList = ref([])
const historyList = ref([])
const detailVisible = ref(false)
const selectedRecord = ref(null)
const detailTitle = ref('就业记录详情')
const rejectVisible = ref(false)
const rejectRemark = ref('')
const pendingRow = ref(null)
const currentPage = ref(1)
const pageSize = ref(15)
const filterStatus = ref('')

const historyCount = computed(() => historyList.value.length)

const filteredHistory = computed(() => {
  if (!filterStatus.value) return historyList.value
  return historyList.value.filter(r => r.auditStatus === filterStatus.value)
})

const pagedHistory = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredHistory.value.slice(start, start + pageSize.value)
})

function loadPending() {
  loading.value = true
  teacherApi.getPendingEmployments().then(res => {
    pendingList.value = Array.isArray(res) ? res : []
  }).catch(() => {
    pendingList.value = []
  }).finally(() => {
    loading.value = false
  })
}

function loadHistory() {
  historyLoading.value = true
  teacherApi.getEmploymentHistory().then(res => {
    historyList.value = Array.isArray(res) ? res : []
  }).catch(() => {
    historyList.value = []
  }).finally(() => {
    historyLoading.value = false
  })
}

function handleDetail(row, source) {
  selectedRecord.value = row
  detailTitle.value = source === 'pending' ? '待审核 - 就业记录详情' : '历史审核 - 就业记录详情'
  detailVisible.value = true
}

function handleApprove(row) {
  ElMessageBox.confirm(`确认通过学生「${row.realName}」的就业记录？`, '审核确认', {
    confirmButtonText: '确认通过',
    cancelButtonText: '取消',
    type: 'success'
  }).then(() => {
    teacherApi.auditEmployment(row.id, 'approve').then(() => {
      ElMessage.success('审核通过')
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
  teacherApi.auditEmployment(pendingRow.value.id, 'reject', rejectRemark.value).then(() => {
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadPending()
  }).catch(() => {
    ElMessage.error('操作失败')
  })
}

watch(activeTab, (val) => {
  if (val === 'history' && historyList.value.length === 0) {
    loadHistory()
  }
})

onMounted(() => {
  loadPending()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.header-filters { display: flex; gap: 8px; }
</style>
