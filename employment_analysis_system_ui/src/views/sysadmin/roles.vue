<template>
  <div class="page-container">
    <h2>角色权限管理</h2>
    <el-row :gutter="20" style="margin-top: 16px;">
      <!-- 左侧：角色列表 -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>角色列表</span>
              <el-button type="primary" size="small" @click="openCreateDialog">新建角色</el-button>
            </div>
          </template>
          <el-table :data="roles" stripe v-loading="loading" highlight-current-row @row-click="selectRole" :row-class-name="currentRowClass">
            <el-table-column prop="roleName" label="角色名称" />
            <el-table-column prop="roleKey" label="角色标识" />
            <el-table-column prop="userCount" label="用户数" width="70" align="center" />
            <el-table-column prop="status" label="状态" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
                  {{ row.status === '0' ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click.stop="openEditDialog(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click.stop="handleDelete(row)" :disabled="row.roleKey === 'admin'">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：权限配置 -->
      <el-col :span="14">
        <el-card v-if="selectedRole">
          <template #header>
            <div class="card-header">
              <span>权限配置 — {{ selectedRole.roleName }}</span>
              <el-button type="primary" size="small" @click="savePermissions">保存配置</el-button>
            </div>
          </template>
          <el-alert v-if="allPermissions.length === 0" title="暂无权限数据，请先在数据库中初始化权限数据" type="info" :closable="false" style="margin-bottom: 16px;" />
          <div v-else class="permission-tree">
            <el-tree
              ref="permTreeRef"
              :data="allPermissions"
              :props="{ children: 'children', label: 'permissionName' }"
              node-key="id"
              show-checkbox
              default-expand-all
              check-strictly
            />
          </div>
        </el-card>
        <el-card v-else>
          <template #header><span>权限配置</span></template>
          <el-empty description="请先选择一个角色" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建/编辑角色弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新建角色'" width="480px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" :required="!isEdit">
          <el-input v-model="form.roleKey" placeholder="如：admin" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.roleSort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
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
const roles = ref([])
const allPermissions = ref([])
const selectedRole = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const permTreeRef = ref(null)
const form = reactive({ id: null, roleName: '', roleKey: '', roleSort: 0, status: '0', remark: '' })

function loadRoles() {
  loading.value = true
  adminApi.role.list().then(res => {
    roles.value = Array.isArray(res) ? res : []
  }).catch(() => { roles.value = [] })
    .finally(() => { loading.value = false })
}

function loadPermissions() {
  adminApi.role.permissions().then(res => {
    allPermissions.value = Array.isArray(res) ? res : []
  }).catch(() => { allPermissions.value = [] })
}

function selectRole(row) {
  selectedRole.value = row
  loadRolePermissions(row.id)
}

function loadRolePermissions(roleId) {
  adminApi.role.get(roleId).then(res => {
    if (res && res.permissionIds) {
      nextTick(() => {
        permTreeRef.value?.setCheckedKeys(res.permissionIds || [])
      })
    } else {
      permTreeRef.value?.setCheckedKeys([])
    }
  }).catch(() => {})
}

import { nextTick } from 'vue'

function savePermissions() {
  if (!selectedRole.value) return
  submitting.value = true
  const checkedKeys = permTreeRef.value?.getCheckedKeys(true) || []
  adminApi.role.assignPermissions(selectedRole.value.id, checkedKeys).then(() => {
    ElMessage.success('权限配置保存成功')
  }).catch(err => { ElMessage.error(err.message || '保存失败') })
    .finally(() => { submitting.value = false })
}

function openCreateDialog() {
  isEdit.value = false
  Object.assign(form, { id: null, roleName: '', roleKey: '', roleSort: 0, status: '0', remark: '' })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    roleName: row.roleName || '',
    roleKey: row.roleKey || '',
    roleSort: row.roleSort || 0,
    status: row.status || '0',
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

function handleSave() {
  if (!form.roleName) { ElMessage.warning('角色名称不能为空'); return }
  if (!isEdit.value && !form.roleKey) { ElMessage.warning('角色标识不能为空'); return }
  submitting.value = true
  const promise = isEdit.value
    ? adminApi.role.update(form.id, form)
    : adminApi.role.create(form)
  promise.then(() => {
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadRoles()
  }).catch(err => { ElMessage.error(err.message || '操作失败') })
    .finally(() => { submitting.value = false })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除角色 "${row.roleName}" 吗？`, '删除角色', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    adminApi.role.delete(row.id).then(() => {
      ElMessage.success('删除成功')
      if (selectedRole.value?.id === row.id) selectedRole.value = null
      loadRoles()
    }).catch(err => { ElMessage.error(err.message || '删除失败') })
  }).catch(() => {})
}

function currentRowClass({ row }) {
  return selectedRole.value?.id === row.id ? 'current-row' : ''
}

onMounted(() => {
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.permission-tree { max-height: 500px; overflow-y: auto; }
</style>
