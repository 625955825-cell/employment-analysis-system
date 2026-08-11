<template>
  <div class="page-container">
    <h2>日志审计</h2>
    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <div class="filter-bar">
            <el-input v-model="keyword" placeholder="搜索用户名/描述" clearable style="width: 200px; margin-right: 8px;" @input="handleSearch" />
            <el-select v-model="logType" placeholder="日志类型" clearable style="width: 140px; margin-right: 8px;" @change="handleSearch">
              <el-option v-for="t in logTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 240px; margin-right: 8px;"
              @change="handleSearch"
            />
          </div>
          <div class="action-bar">
            <el-button type="danger" size="small" @click="handleClear" :loading="clearing">清空日志</el-button>
          </div>
        </div>
      </template>

      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="createTime" label="操作时间" width="170" />
        <el-table-column prop="username" label="操作用户" width="120" />
        <el-table-column prop="logType" label="日志类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.logType)" size="small">{{ typeText(row.logType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="method" label="请求方式" width="90" align="center" />
        <el-table-column prop="url" label="请求路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="costTime" label="耗时(ms)" width="90" align="center">
          <template #default="{ row }">
            <span :class="{ 'slow-log': row.costTime && row.costTime > 3000 }">{{ row.costTime || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="params.page"
          v-model:page-size="params.size"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const loading = ref(false)
const clearing = ref(false)
const records = ref([])
const total = ref(0)
const keyword = ref('')
const logType = ref('')
const dateRange = ref([])
const params = reactive({ page: 1, size: 20 })
const logTypes = ref([])

function loadData() {
  loading.value = true
  const payload = {
    page: params.page,
    size: params.size,
    keyword: keyword.value || undefined,
    logType: logType.value || undefined
  }
  adminApi.log.list(payload).then(res => {
    records.value = res?.records || res?.list || []
    total.value = res?.total || records.value.length
  }).catch(() => { records.value = []; total.value = 0 })
    .finally(() => { loading.value = false })
}

function loadTypes() {
  adminApi.log.types().then(res => {
    logTypes.value = Array.isArray(res) ? res : []
  }).catch(() => { logTypes.value = [] })
}

let searchTimer = null
function handleSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    params.page = 1
    loadData()
  }, 300)
}

function typeText(type) {
  const map = { login: '登录', logout: '登出', operation: '操作', error: '异常', auth: '认证' }
  return map[type] || type || '-'
}

function typeTag(type) {
  const map = { login: 'primary', logout: 'info', operation: 'success', error: 'danger', auth: 'warning' }
  return map[type] || 'info'
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除这条日志吗？`, '删除日志', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    adminApi.log.delete(row.id).then(() => {
      ElMessage.success('删除成功')
      loadData()
    }).catch(err => { ElMessage.error(err.message || '删除失败') })
  }).catch(() => {})
}

function handleClear() {
  ElMessageBox.confirm('确定清空所有日志吗？此操作不可恢复！', '清空日志', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    clearing.value = true
    adminApi.log.clear().then(() => {
      ElMessage.success('日志已清空')
      loadData()
    }).catch(err => { ElMessage.error(err.message || '清空失败') })
      .finally(() => { clearing.value = false })
  }).catch(() => {})
}

onMounted(() => {
  loadData()
  loadTypes()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 8px; }
.filter-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.action-bar { display: flex; gap: 8px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.slow-log { color: #f56c6c; font-weight: 600; }
</style>
