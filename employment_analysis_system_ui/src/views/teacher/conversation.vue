<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background:#409eff22;">
              <el-icon :size="24" color="#409eff"><ChatLineSquare /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">总谈话次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background:#67c23a22;">
              <el-icon :size="24" color="#67c23a"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.studentCount }}</div>
              <div class="stat-label">谈话学生数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background:#e6a23c22;">
              <el-icon :size="24" color="#e6a23c"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.thisMonth }}</div>
              <div class="stat-label">本月谈话</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background:#f56c6c22;">
              <el-icon :size="24" color="#f56c6c"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.unemployedCount }}</div>
              <div class="stat-label">未就业学生谈话</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 页面标题和操作 -->
    <div class="page-header">
      <h2>谈心谈话</h2>
      <div class="header-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索学生姓名/学号"
          style="width:220px;margin-right:12px;"
          clearable
          @clear="loadData"
          @keyup.enter="loadData"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterType" placeholder="谈话类型" style="width:140px;margin-right:12px;" clearable>
          <el-option label="全部类型" value="" />
          <el-option label="就业指导" value="就业指导" />
          <el-option label="心理疏导" value="心理疏导" />
          <el-option label="学业帮扶" value="学业帮扶" />
          <el-option label="生活关心" value="生活关心" />
          <el-option label="其他" value="其他" />
        </el-select>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增谈话记录
        </el-button>
      </div>
    </div>

    <!-- 谈话类型分布 -->
    <el-card shadow="hover" class="type-dist-card" v-if="typeDistData.length > 0">
      <template #header>
        <span class="card-title">谈话类型分布</span>
      </template>
      <div class="type-tags">
        <el-tag v-for="item in typeDistData" :key="item.type" size="large" class="type-tag" effect="plain">
          {{ item.type || '未分类' }}: {{ item.count }}次
        </el-tag>
      </div>
    </el-card>

    <!-- 谈话记录列表 -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>谈话记录列表</span>
          <span style="font-size:12px;color:#999;">共 {{ filteredData.length }} 条记录</span>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-empty v-else-if="filteredData.length === 0" description="暂无谈话记录" />

      <el-table v-else :data="pagedData" stripe border>
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="studentName" label="学生姓名" width="100">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.studentName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="140" show-overflow-tooltip />
        <el-table-column prop="conversationTime" label="谈话时间" width="160" />
        <el-table-column prop="conversationType" label="谈话类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.conversationType)" size="small">{{ row.conversationType || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="topic" label="谈话主题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="conversationPlace" label="谈话地点" width="120" show-overflow-tooltip />
        <el-table-column prop="result" label="谈话结果" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="记录时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="filteredData.length > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredData.length"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑谈话记录' : '新增谈话记录'"
      width="700px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学生姓名" prop="studentId">
              <el-select
                v-model="form.studentId"
                placeholder="请选择学生"
                filterable
                style="width:100%;"
                :disabled="isEdit"
              >
                <el-option
                  v-for="s in students"
                  :key="s.id"
                  :label="`${s.studentNo} - ${s.realName}`"
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="谈话时间" prop="conversationTime">
              <el-date-picker
                v-model="form.conversationTime"
                type="datetime"
                placeholder="选择时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="谈话类型">
              <el-select v-model="form.conversationType" placeholder="请选择" style="width:100%;">
                <el-option label="就业指导" value="就业指导" />
                <el-option label="心理疏导" value="心理疏导" />
                <el-option label="学业帮扶" value="学业帮扶" />
                <el-option label="生活关心" value="生活关心" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="谈话地点">
              <el-input v-model="form.conversationPlace" placeholder="请输入谈话地点" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="谈话主题">
          <el-input v-model="form.topic" placeholder="请输入谈话主题" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="谈话内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="请详细记录谈话内容..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="谈话结果">
          <el-input
            v-model="form.result"
            type="textarea"
            :rows="3"
            placeholder="请输入谈话结果或学生反馈..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="后续计划">
          <el-input
            v-model="form.nextPlan"
            type="textarea"
            :rows="3"
            placeholder="请输入后续跟进计划..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="viewVisible" title="谈话记录详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="selectedRow">
        <el-descriptions-item label="学号">{{ selectedRow.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="学生姓名">{{ selectedRow.studentName }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ selectedRow.className }}</el-descriptions-item>
        <el-descriptions-item label="谈话时间">{{ selectedRow.conversationTime }}</el-descriptions-item>
        <el-descriptions-item label="谈话类型">
          <el-tag :type="typeTagType(selectedRow.conversationType)" size="small">{{ selectedRow.conversationType || '未分类' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="谈话地点">{{ selectedRow.conversationPlace || '-' }}</el-descriptions-item>
        <el-descriptions-item label="谈话主题" :span="2">{{ selectedRow.topic || '-' }}</el-descriptions-item>
        <el-descriptions-item label="谈话内容" :span="2">
          <div class="content-text">{{ selectedRow.content || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="谈话结果" :span="2">
          <div class="content-text">{{ selectedRow.result || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="后续计划" :span="2">
          <div class="content-text">{{ selectedRow.nextPlan || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="记录时间" :span="2">{{ selectedRow.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Loading, Plus, Search, ChatLineSquare, User, TrendCharts, Warning } from '@element-plus/icons-vue'
import { teacherApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const students = ref([])
const dialogVisible = ref(false)
const viewVisible = ref(false)
const isEdit = ref(false)
const selectedRow = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const filterType = ref('')
const formRef = ref(null)
const form = reactive({})

const formRules = {
  studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
  conversationTime: [{ required: true, message: '请选择谈话时间', trigger: 'change' }],
  content: [{ required: true, message: '请输入谈话内容', trigger: 'blur' }]
}

// 统计数据
const stats = computed(() => {
  const now = new Date()
  const thisMonth = tableData.value.filter(r => {
    if (!r.conversationTime) return false
    const t = new Date(r.conversationTime)
    return t.getFullYear() === now.getFullYear() && t.getMonth() === now.getMonth()
  }).length

  const uniqueStudents = new Set(tableData.value.map(r => r.studentId))

  // 简单统计未就业谈话（含有"就业"关键字或心理疏导类型的）
  const unemployedCount = tableData.value.filter(r =>
    (r.conversationType && (r.conversationType.includes('就业') || r.conversationType.includes('心理')))
  ).length

  return {
    total: tableData.value.length,
    studentCount: uniqueStudents.size,
    thisMonth,
    unemployedCount
  }
})

// 谈话类型分布
const typeDistData = computed(() => {
  const map = {}
  tableData.value.forEach(r => {
    const t = r.conversationType || '未分类'
    map[t] = (map[t] || 0) + 1
  })
  return Object.entries(map).map(([type, count]) => ({ type, count }))
})

// 过滤后的数据
const filteredData = computed(() => {
  let data = tableData.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(r =>
      (r.studentName && r.studentName.toLowerCase().includes(kw)) ||
      (r.studentNo && r.studentNo.toLowerCase().includes(kw)) ||
      (r.topic && r.topic.toLowerCase().includes(kw))
    )
  }
  if (filterType.value) {
    data = data.filter(r => r.conversationType === filterType.value)
  }
  return data
})

// 分页数据
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

// 谈话类型标签颜色
function typeTagType(type) {
  const map = {
    '就业指导': 'success',
    '心理疏导': 'warning',
    '学业帮扶': 'primary',
    '生活关心': 'info',
    '其他': 'default'
  }
  return map[type] || 'info'
}

function loadData() {
  loading.value = true
  Promise.all([
    teacherApi.getConversations(),
    teacherApi.getStudents({ keyword: '' })
  ]).then(([convs, studs]) => {
    // conversations: 分页格式 { records: [], total: 0 } 或直接数组
    if (convs && typeof convs === 'object' && !Array.isArray(convs)) {
      tableData.value = convs.records || convs.list || []
    } else {
      tableData.value = Array.isArray(convs) ? convs : []
    }
    // students: 分页格式
    if (studs && typeof studs === 'object' && !Array.isArray(studs)) {
      students.value = studs.records || studs.list || []
    } else {
      students.value = Array.isArray(studs) ? studs : []
    }
  }).catch(() => {
    tableData.value = []
    students.value = []
  }).finally(() => {
    loading.value = false
  })
}

function handleAdd() {
  isEdit.value = false
  Object.keys(form).forEach(k => delete form[k])
  // 如果 URL 有 preSelectStudent 参数，预填学生
  if (route.query.studentId) {
    form.studentId = Number(route.query.studentId)
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  // 加载该记录的完整信息
  teacherApi.getConversation(row.id).then(res => {
    const data = res && typeof res === 'object' ? res : row
    Object.keys(form).forEach(k => delete form[k])
    Object.assign(form, {
      id: data.id,
      studentId: data.studentId,
      conversationTime: data.conversationTime,
      conversationType: data.conversationType || '',
      conversationPlace: data.conversationPlace || '',
      topic: data.topic || '',
      content: data.content || '',
      result: data.result || '',
      nextPlan: data.nextPlan || ''
    })
    dialogVisible.value = true
  }).catch(() => {
    // fallback: 直接用 row 数据
    Object.keys(form).forEach(k => delete form[k])
    Object.assign(form, {
      id: row.id,
      studentId: row.studentId,
      conversationTime: row.conversationTime,
      conversationType: row.conversationType || '',
      conversationPlace: row.conversationPlace || '',
      topic: row.topic || '',
      content: row.content || '',
      result: row.result || '',
      nextPlan: row.nextPlan || ''
    })
    dialogVisible.value = true
  })
}

function handleSubmit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const action = isEdit.value
      ? teacherApi.updateConversation(form.id, form)
      : teacherApi.createConversation(form)
    action.then(() => {
      ElMessage.success(isEdit.value ? '修改成功' : '保存成功')
      dialogVisible.value = false
      loadData()
    }).catch(() => {
      ElMessage.error(isEdit.value ? '修改失败' : '保存失败')
    }).finally(() => {
      submitting.value = false
    })
  })
}

function handleView(row) {
  selectedRow.value = row
  viewVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确认删除"${row.studentName}"的谈话记录？`, '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    teacherApi.deleteConversation(row.id).then(() => {
      ElMessage.success('删除成功')
      loadData()
    }).catch(() => {
      ElMessage.error('删除失败')
    })
  }).catch(() => {})
}

// 预选学生时，滚动到对话框
watch(() => dialogVisible, (val) => {
  if (val && route.query.studentId && !isEdit.value) {
    // 确保 form 中的 studentId 已设置
    form.studentId = Number(route.query.studentId)
  }
})

onMounted(() => {
  loadData()
  // 如果 URL 有 studentId 参数，预填学生并打开对话框
  if (route.query.studentId) {
    handleAdd()
  }
})
</script>

<style scoped>
.page-container { padding: 20px; }

.stats-row { margin-bottom: 16px; }
.stat-card { cursor: default; }
.stat-item { display: flex; align-items: center; gap: 14px; }
.stat-icon { width: 50px; height: 50px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-info { flex: 1; }
.stat-value { font-size: 24px; font-weight: 700; color: #303133; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.header-actions { display: flex; align-items: center; }

.type-dist-card { margin-bottom: 16px; }
.card-title { font-weight: 600; font-size: 14px; color: #303133; }
.type-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.type-tag { font-size: 13px; }

.card-header { display: flex; justify-content: space-between; align-items: center; }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.content-text { white-space: pre-wrap; word-break: break-word; color: #606266; line-height: 1.6; }
</style>
