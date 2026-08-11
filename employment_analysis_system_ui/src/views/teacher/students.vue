<template>
  <div class="page-container">
    <h2>学生列表</h2>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>学生信息</span>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="records.length === 0" class="empty-state">
        <el-empty description="暂无学生数据" />
      </div>

      <div v-else>
        <el-table :data="records" stripe style="width: 100%">
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="gender" label="性别" width="70">
            <template #default="{ row }">
              {{ row.gender === 'male' ? '男' : row.gender === 'female' ? '女' : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="className" label="班级" width="140" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
          <el-table-column prop="graduationYear" label="毕业年份" width="100" />
          <el-table-column prop="employmentStatus" label="就业状态" width="100">
            <template #default="{ row }">
              <el-tag :type="employmentTagType(row.employmentStatus)" size="small">
                {{ employmentText(row.employmentStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleView(row)">查看详情</el-button>
              <el-button type="success" link size="small" @click="handleConversation(row)">谈话记录</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="params.page"
            v-model:page-size="params.size"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="学生详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="selectedStudent">
        <el-descriptions-item label="学号">{{ selectedStudent.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ selectedStudent.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ selectedStudent.gender === 'male' ? '男' : selectedStudent.gender === 'female' ? '女' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ selectedStudent.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="院系" :span="2">{{ selectedStudent.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业" :span="2">{{ selectedStudent.majorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ selectedStudent.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="毕业年份">{{ selectedStudent.graduationYear || '-' }}</el-descriptions-item>
        <el-descriptions-item label="就业状态" :span="2">
          <el-tag :type="employmentTagType(selectedStudent.employmentStatus)" size="small">
            {{ employmentText(selectedStudent.employmentStatus) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { teacherApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const records = ref([])
const total = ref(0)
const params = reactive({ page: 1, size: 10 })
const detailVisible = ref(false)
const selectedStudent = ref(null)

const employmentTagType = (status) => {
  const map = { employed: 'success', unemployed: 'warning', 'pending': 'info' }
  return map[status] || 'info'
}
const employmentText = (status) => {
  const map = { employed: '已就业', unemployed: '未就业', pending: '待审核' }
  return map[status] || status || '-'
}

function loadData() {
  loading.value = true
  teacherApi.getStudents(params).then(res => {
    const data = res && typeof res === 'object' ? res : {}
    records.value = data.records || data.list || []
    total.value = data.total || records.value.length
  }).catch(() => {
    records.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function handleView(row) {
  selectedStudent.value = row
  detailVisible.value = true
}

function handleConversation(row) {
  router.push({ path: '/teacher/conversation', query: { studentId: row.id } })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
