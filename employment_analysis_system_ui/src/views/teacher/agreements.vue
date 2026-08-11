<template>
  <div class="page-container">
    <h2>三方协议管理</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">协议总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #E6A23C;">{{ stats.pending }}</div>
            <div class="stat-label">待学校签署</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #409EFF;">{{ stats.companySigned }}</div>
            <div class="stat-label">企业已签</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #67C23A;">{{ stats.completed }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card style="margin-bottom: 16px;">
      <el-form :inline="true" :model="filters">
        <el-form-item label="状态筛选">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 160px;">
            <el-option label="全部" value="" />
            <el-option label="待学生签署" value="pending" />
            <el-option label="学生已签" value="student_signed" />
            <el-option label="企业已签" value="company_signed" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="学生姓名/学号/企业名称" clearable style="width: 200px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAgreements">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 协议列表 -->
    <el-card v-loading="loading">
      <el-table :data="filteredList" stripe>
        <el-table-column prop="studentName" label="学生姓名" width="100" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="className" label="班级" min-width="120" />
        <el-table-column prop="companyName" label="企业名称" min-width="180" />
        <el-table-column prop="companyScale" label="企业规模" width="110">
          <template #default="{ row }">{{ row.companyScale || '-' }}</template>
        </el-table-column>
        <el-table-column prop="agreementNo" label="协议编号" min-width="180" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentSignTime" label="学生签署" width="160">
          <template #default="{ row }">{{ formatTime(row.studentSignTime) }}</template>
        </el-table-column>
        <el-table-column prop="companySignTime" label="企业签署" width="160">
          <template #default="{ row }">{{ formatTime(row.companySignTime) }}</template>
        </el-table-column>
        <el-table-column prop="schoolSignTime" label="学校签署" width="160">
          <template #default="{ row }">{{ formatTime(row.schoolSignTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="120">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              v-if="row.status === 'company_signed'"
              :loading="signingId === row.id"
              @click="handleSign(row)"
            >
              签署协议
            </el-button>
            <span v-else style="color:#999;font-size:12px;">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredList.length === 0" description="暂无协议记录" style="padding: 40px 0;" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teacherApi } from '@/api'

const loading = ref(false)
const signingId = ref(null)
const agreementList = ref([])
const filters = reactive({ status: '', keyword: '' })

const stats = reactive({ total: 0, pending: 0, companySigned: 0, completed: 0 })

const statusTagType = (status) => {
  const map = {
    pending: 'info',
    student_signed: 'warning',
    company_signed: 'primary',
    all_signed: 'success',
    completed: 'success'
  }
  return map[status] || 'info'
}

const statusText = (status) => {
  const map = {
    pending: '待学生签署',
    student_signed: '学生已签',
    company_signed: '企业已签',
    all_signed: '全部签署',
    completed: '已完成'
  }
  return map[status] || status || '-'
}

const formatTime = (ts) => {
  if (!ts) return '-'
  try { return ts.substring(0, 19).replace('T', ' ') } catch { return ts }
}

const filteredList = computed(() => {
  let list = agreementList.value
  if (filters.status) {
    list = list.filter(a => a.status === filters.status)
  }
  if (filters.keyword) {
    const kw = filters.keyword.toLowerCase()
    list = list.filter(a =>
      (a.studentName && a.studentName.toLowerCase().includes(kw)) ||
      (a.studentNo && a.studentNo.toLowerCase().includes(kw)) ||
      (a.companyName && a.companyName.toLowerCase().includes(kw))
    )
  }
  return list
})

async function loadStats() {
  try {
    const res = await teacherApi.getAgreementStats()
    const data = Array.isArray(res) && res.length > 0 ? res[0] : res
    Object.assign(stats, {
      total: data.total || 0,
      pending: data.pending || 0,
      companySigned: data.companySigned || 0,
      completed: data.completed || 0
    })
  } catch {}
}

async function loadAgreements() {
  loading.value = true
  try {
    const res = await teacherApi.getAgreements({})
    agreementList.value = Array.isArray(res) ? res : (res?.records || [])
  } catch {
    agreementList.value = []
  } finally {
    loading.value = false
  }
}

async function handleSign(row) {
  try {
    await ElMessageBox.confirm(
      `确认签署学生【${row.studentName}】的三方协议？\n\n企业：${row.companyName}\n协议编号：${row.agreementNo}`,
      '学校签署确认',
      { type: 'info', confirmButtonText: '确认签署', cancelButtonText: '取消' }
    )
    signingId.value = row.id
    await teacherApi.signAgreement(row.id)
    ElMessage.success('签署成功')
    await loadAgreements()
    await loadStats()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  } finally {
    signingId.value = null
  }
}

function resetFilters() {
  filters.status = ''
  filters.keyword = ''
}

onMounted(() => {
  loadAgreements()
  loadStats()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
</style>
