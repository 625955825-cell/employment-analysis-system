<template>
  <div class="page-container">
    <h2>各班人数统计</h2>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>班级学生人数</span>
          <el-input
            v-model="keyword"
            placeholder="搜索班级名称"
            style="width: 200px;"
            clearable
            @input="filterClasses"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="filteredClasses.length === 0" class="empty-state">
        <el-empty description="暂无班级数据" />
      </div>

      <div v-else class="class-grid">
        <div v-for="cls in filteredClasses" :key="cls.id" class="class-card">
          <div class="class-header">
            <div class="class-name">{{ cls.className }}</div>
            <div class="class-grade">{{ cls.grade || '' }}</div>
          </div>
          <div class="class-body">
            <div class="stat-row">
              <span class="stat-label">班主任</span>
              <span class="stat-value">{{ cls.advisor || '未分配' }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">学生总数</span>
              <span class="stat-value primary">{{ cls.studentCount || 0 }} 人</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">专业</span>
              <span class="stat-value">{{ cls.majorName || '-' }}</span>
            </div>
          </div>
          <div class="class-footer">
            <el-button type="primary" size="small" plain @click="viewClassDetail(cls)">
              查看详情
            </el-button>
          </div>
        </div>
      </div>

      <el-pagination
        v-if="filteredClasses.length > 0"
        layout="total, prev, pager, next"
        :total="filteredClasses.length"
        :page-size="12"
        :current-page="currentPage"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: center;"
      />
    </el-card>

    <!-- 班级详情对话框 -->
    <el-dialog v-model="detailVisible" :title="'「' + selectedClass?.className + '」学生详情'" width="700px" destroy-on-close>
      <el-table :data="studentList" stripe max-height="400">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">
            {{ row.gender === 'male' ? '男' : row.gender === 'female' ? '女' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Loading, Search } from '@element-plus/icons-vue'
import { teacherApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const classList = ref([])
const filteredClasses = ref([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 12

const detailVisible = ref(false)
const selectedClass = ref(null)
const studentList = ref([])

function filterClasses() {
  currentPage.value = 1
  if (keyword.value) {
    filteredClasses.value = classList.value.filter(cls =>
      cls.className && cls.className.includes(keyword.value)
    )
  } else {
    filteredClasses.value = [...classList.value]
  }
}

function handlePageChange(page) {
  currentPage.value = page
}

async function loadClasses() {
  loading.value = true
  try {
    const res = await teacherApi.getClasses()
    classList.value = Array.isArray(res) ? res : []
    filteredClasses.value = [...classList.value]
  } catch (e) {
    console.error('加载班级列表失败', e)
    classList.value = []
    filteredClasses.value = []
  } finally {
    loading.value = false
  }
}

async function viewClassDetail(cls) {
  selectedClass.value = cls
  detailVisible.value = true
  try {
    const res = await teacherApi.getStudents({ page: 1, size: 1000 })
    const data = res && typeof res === 'object' ? res : {}
    const list = data.records || []
    studentList.value = list.filter(s => s.classId === cls.id)
  } catch {
    studentList.value = []
  }
}

onMounted(() => {
  loadClasses()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }

.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.class-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
}
.class-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.class-header {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.class-name { font-weight: 600; font-size: 16px; }
.class-grade { font-size: 12px; opacity: 0.9; }

.class-body { padding: 16px; }
.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.stat-row:last-child { border-bottom: none; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-weight: 500; font-size: 13px; }
.stat-value.primary { color: #409eff; font-size: 15px; }

.class-footer { padding: 12px 16px; border-top: 1px solid #f0f0f0; text-align: center; }
</style>
