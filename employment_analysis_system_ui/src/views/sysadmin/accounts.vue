<template>
  <div class="page-container">
    <h2>账号管理</h2>
    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <div class="filter-bar">
            <el-input v-model="keyword" placeholder="搜索用户名/姓名" clearable style="width: 200px; margin-right: 8px;" @input="handleSearch" />
            <el-select v-model="roleFilter" placeholder="筛选角色" clearable style="width: 140px; margin-right: 8px;" @change="handleSearch">
              <el-option v-for="r in roleOptions" :key="r.roleKey" :label="r.roleName" :value="r.roleKey" />
            </el-select>
            <el-select v-model="deptFilter" placeholder="筛选院系" clearable style="width: 140px; margin-right: 8px;" @change="handleSearch">
              <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
            </el-select>
          </div>
          <el-button type="primary" size="small" @click="openCreateDialog">创建账号</el-button>
        </div>
      </template>

      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="roleName" label="角色" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ row.roleName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column label="院系" width="120">
          <template #default="{ row }">
            {{ getDeptName(row.deptId) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openRoleDialog(row)">分配角色</el-button>
            <el-button type="info" link size="small" @click="resetPassword(row)">重置密码</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
    </el-card>

    <!-- 创建账号弹窗 -->
    <el-dialog v-model="createVisible" title="创建账号" width="500px" destroy-on-close>
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="createForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="createForm.role" placeholder="请选择角色" style="width: 100%;" @change="onCreateRoleChange">
            <el-option v-for="r in roleOptions" :key="r.roleKey" :label="r.roleName" :value="r.roleKey" />
          </el-select>
        </el-form-item>
        <el-form-item label="院系" v-if="createForm.role !== 'company'">
          <el-select v-model="createForm.deptId" placeholder="选择院系" style="width: 100%;" @change="onCreateDeptChange">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业" v-if="createForm.role === 'student' || createForm.role === 'class_teacher'">
          <el-select v-model="createForm.majorId" placeholder="选择专业" style="width: 100%;" :disabled="!createForm.deptId" @change="onCreateMajorChange">
            <el-option v-for="m in majorOptions" :key="m.id" :label="m.majorName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" v-if="createForm.role === 'student'">
          <el-select v-model="createForm.classId" placeholder="选择班级" style="width: 100%;" :disabled="!createForm.majorId">
            <el-option v-for="c in classOptions" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" v-if="createForm.role === 'class_teacher'">
          <el-select v-model="createForm.classId" placeholder="选择班级" style="width: 100%;" :disabled="!createForm.majorId">
            <el-option v-for="c in classOptions" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学号" v-if="createForm.role === 'student'">
          <el-input v-model="createForm.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑账号弹窗 -->
    <el-dialog v-model="editVisible" title="编辑账号" width="480px" destroy-on-close>
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
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="roleVisible" title="分配角色" width="400px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <span>{{ currentRow?.username }}</span>
        </el-form-item>
        <el-form-item label="当前角色">
          <el-tag size="small">{{ currentRow?.roleName || '未分配' }}</el-tag>
        </el-form-item>
        <el-form-item label="新角色" required>
          <el-select v-model="newRole" placeholder="请选择角色" style="width: 100%;">
            <el-option v-for="r in roleOptions" :key="r.roleKey" :label="r.roleName" :value="r.roleKey" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAssignRole">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const total = ref(0)
const roleOptions = ref([])
const keyword = ref('')
const roleFilter = ref('')
const deptFilter = ref('')
const params = reactive({ page: 1, size: 10 })

const createVisible = ref(false)
const editVisible = ref(false)
const roleVisible = ref(false)
const currentRow = ref(null)
const newRole = ref('')

const createForm = reactive({ username: '', realName: '', role: '', phone: '', email: '', deptId: null, majorId: null, classId: null, studentNo: '' })
const editForm = reactive({ id: null, username: '', realName: '', phone: '', email: '', status: '0' })
const deptOptions = ref([])
const majorOptions = ref([])
const classOptions = ref([])

function loadData() {
  loading.value = true
  adminApi.account.list({ page: params.page, size: params.size, keyword: keyword.value || undefined, role: roleFilter.value || undefined, deptId: deptFilter.value || undefined }).then(res => {
    records.value = res?.records || res?.list || []
    total.value = res?.total || records.value.length
  }).catch(() => { records.value = []; total.value = 0 })
    .finally(() => { loading.value = false })
}

function loadRoles() {
  adminApi.account.roles().then(res => {
    roleOptions.value = Array.isArray(res) ? res : []
  }).catch(() => { roleOptions.value = [] })
}

function loadDepts() {
  adminApi.dept.list().then(res => {
    deptOptions.value = Array.isArray(res) ? res : []
  }).catch(() => { deptOptions.value = [] })
}

function onCreateRoleChange() {
  createForm.deptId = null
  createForm.majorId = null
  createForm.classId = null
  createForm.studentNo = ''
  majorOptions.value = []
  classOptions.value = []
}

function onCreateDeptChange(deptId) {
  createForm.majorId = null
  createForm.classId = null
  classOptions.value = []
  if (deptId) {
    adminApi.major.all().then(res => {
      const all = Array.isArray(res) ? res : []
      majorOptions.value = all.filter(m => m.deptId === deptId)
    }).catch(() => { majorOptions.value = [] })
  } else {
    majorOptions.value = []
  }
}

function onCreateMajorChange(majorId) {
  createForm.classId = null
  if (majorId) {
    adminApi.sysClass.list({ majorId }).then(res => {
      const list = res?.records || res?.list || []
      classOptions.value = Array.isArray(list) ? list : []
    }).catch(() => { classOptions.value = [] })
  } else {
    classOptions.value = []
  }
}

function getDeptName(deptId) {
  if (!deptId) return '-'
  const d = deptOptions.value.find(x => x.id == deptId || x.id === deptId)
  return d ? d.deptName : '-'
}

let searchTimer = null
function handleSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    params.page = 1
    loadData()
  }, 300)
}

function openCreateDialog() {
  Object.assign(createForm, { username: '', realName: '', role: '', phone: '', email: '', deptId: null, majorId: null, classId: null, studentNo: '' })
  majorOptions.value = []
  classOptions.value = []
  createVisible.value = true
}

function handleCreate() {
  if (!createForm.username || !createForm.role) {
    ElMessage.warning('用户名和角色不能为空')
    return
  }
  if ((createForm.role === 'student' || createForm.role === 'class_teacher') && !createForm.deptId) {
    ElMessage.warning('请选择院系')
    return
  }
  if (createForm.role === 'student' && !createForm.studentNo) {
    ElMessage.warning('请输入学号')
    return
  }
  submitting.value = true
  const submitData = { ...createForm }
  adminApi.account.create(submitData).then(res => {
    const pwd = res?.password || res?.data?.password || '123456'
    ElMessageBox.alert(`账号创建成功！<br>用户名：${createForm.username}<br>默认密码：${pwd}`, '创建成功', {
      confirmButtonText: '知道了',
      dangerouslyUseHTMLString: true
    })
    createVisible.value = false
    loadData()
  }).catch(err => {
    ElMessage.error(err.message || '创建失败')
  }).finally(() => { submitting.value = false })
}

function openEditDialog(row) {
  Object.assign(editForm, {
    id: row.id, username: row.username || '', realName: row.realName || '',
    phone: row.phone || '', email: row.email || '', status: row.status || '0'
  })
  editVisible.value = true
}

function handleUpdate() {
  submitting.value = true
  adminApi.account.update(editForm.id, editForm).then(() => {
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  }).catch(err => { ElMessage.error(err.message || '保存失败') })
    .finally(() => { submitting.value = false })
}

function openRoleDialog(row) {
  currentRow.value = row
  newRole.value = row.role || ''
  roleVisible.value = true
}

function handleAssignRole() {
  if (!newRole.value) {
    ElMessage.warning('请选择角色')
    return
  }
  submitting.value = true
  adminApi.account.assignRole(currentRow.value.id, newRole.value).then(() => {
    ElMessage.success('角色分配成功')
    roleVisible.value = false
    loadData()
  }).catch(err => { ElMessage.error(err.message || '分配失败') })
    .finally(() => { submitting.value = false })
}

function resetPassword(row) {
  ElMessageBox.confirm(`确定重置用户 "${row.username}" 的密码吗？重置后密码将更新为新密码。`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    adminApi.account.resetPassword(row.id).then(res => {
      const pwd = res?.data || res
      ElMessageBox.alert(`密码重置成功！<br>新密码：<strong>${pwd}</strong>`, '重置成功', {
        confirmButtonText: '知道了',
        dangerouslyUseHTMLString: true
      })
    }).catch(err => { ElMessage.error(err.message || '重置失败') })
  }).catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除账号 "${row.username}" 吗？此操作不可恢复。`, '删除账号', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    adminApi.account.delete(row.id).then(() => {
      ElMessage.success('删除成功')
      loadData()
    }).catch(err => { ElMessage.error(err.message || '删除失败') })
  }).catch(() => {})
}

onMounted(() => {
  loadData()
  loadRoles()
  loadDepts()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
