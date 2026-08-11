<template>
  <div class="page-container">
    <h2>用户管理</h2>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <div class="filter-bar">
            <el-input v-model="keyword" placeholder="用户名/姓名" clearable style="width: 160px; margin-right: 8px;" @input="handleSearch" />
            <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width: 130px; margin-right: 8px;" @change="handleSearch">
              <el-option v-for="(label, key) in roleMap" :key="key" :label="label" :value="key" />
            </el-select>
            <el-select v-model="deptFilter" placeholder="院系筛选" clearable style="width: 130px; margin-right: 8px;" @change="handleSearch">
              <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
            </el-select>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="records.length === 0" class="empty-state">
        <el-empty description="暂无用户数据" />
      </div>

      <div v-else>
        <el-table :data="records" stripe style="width: 100%">
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="role" label="角色" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ roleText(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deptName" label="院系" width="140" />
          <el-table-column prop="className" label="班级" width="120" />
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                {{ row.status === '0' ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button
                :type="row.status === '0' ? 'danger' : 'success'"
                link size="small"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === '0' ? '禁用' : '启用' }}
              </el-button>
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

    <el-dialog v-model="dialogVisible" title="编辑用户" width="550px" destroy-on-close>
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const records = ref([])
const total = ref(0)
const keyword = ref('')
const roleFilter = ref('')
const deptFilter = ref('')
const deptOptions = ref([])
const params = reactive({ page: 1, size: 10 })
const editForm = reactive({ id: null, username: '', realName: '', phone: '', email: '', status: '0' })

const roleMap = {
  admin: '校级管理员',
  employment_staff: '数据分析师',
  class_teacher: '班级老师',
  dept_teacher: '院级老师',
  student: '学生',
  company: '企业'
}
const roleText = (key) => roleMap[key] || key || '-'

function loadData() {
  loading.value = true
  const p = { page: params.page, size: params.size }
  if (keyword.value) p.keyword = keyword.value
  if (roleFilter.value) p.role = roleFilter.value
  if (deptFilter.value) p.deptId = deptFilter.value
  request.get('/admin/accounts/list', { params: p }).then(res => {
    records.value = res?.records || res?.list || []
    total.value = res?.total || records.value.length
  }).catch(() => {
    records.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function loadDepts() {
  request.get('/admin/depts/list').then(res => {
    deptOptions.value = Array.isArray(res) ? res : []
  }).catch(() => { deptOptions.value = [] })
}

let searchTimer = null
function handleSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    params.page = 1
    loadData()
  }, 300)
}

function handleEdit(row) {
  Object.assign(editForm, {
    id: row.id,
    username: row.username || '',
    realName: row.realName || '',
    phone: row.phone || '',
    email: row.email || '',
    status: row.status || '0'
  })
  dialogVisible.value = true
}

function handleSave() {
  submitting.value = true
  request.put(`/admin/accounts/${editForm.id}`, editForm).then(() => {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  }).catch(err => {
    ElMessage.error(err.message || '保存失败')
  }).finally(() => {
    submitting.value = false
  })
}

function handleToggleStatus(row) {
  const action = row.status === '0' ? '禁用' : '启用'
  ElMessageBox.confirm(`确定${action}用户 "${row.username}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.put(`/admin/accounts/${row.id}`, { status: row.status === '0' ? '1' : '0' }).then(() => {
      ElMessage.success(`${action}成功`)
      loadData()
    }).catch(err => {
      ElMessage.error(err.message || '操作失败')
    })
  }).catch(() => {})
}

onMounted(() => {
  loadData()
  loadDepts()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
