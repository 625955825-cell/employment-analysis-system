<template>
  <div class="page-container">
    <h2>个人信息</h2>
    <el-card style="margin-top: 20px;" v-loading="loading">
      <div class="avatar-section">
        <el-avatar :size="100" :src="avatarUrl" class="avatar-img">
          <el-icon :size="40"><User /></el-icon>
        </el-avatar>
        <div class="avatar-actions">
          <el-upload
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            accept="image/*"
          >
            <el-button type="primary" plain size="small" :loading="avatarUploading">
              <el-icon><Upload /></el-icon> 上传头像
            </el-button>
          </el-upload>
          <span class="avatar-tip">支持 JPG、PNG 格式，建议 1MB 以内</span>
        </div>
      </div>
      <el-divider />
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <div style="display:flex;gap:8px;">
            <el-button type="warning" plain size="small" @click="openPwdDialog">修改密码</el-button>
            <el-button type="success" plain size="small" @click="openUsernameDialog">修改用户名</el-button>
            <el-button type="primary" @click="openEditDialog">编辑信息</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="学号">{{ profile.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ profile.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ profile.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ profile.majorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ profile.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="毕业年份">{{ profile.graduationYear ? profile.graduationYear + '届' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ profile.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ profile.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学籍状态">{{ formatStatus(profile.status) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="450px" destroy-on-close>
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd" :loading="pwdSubmitting">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 修改用户名对话框 -->
    <el-dialog v-model="usernameDialogVisible" title="修改用户名" width="450px" destroy-on-close>
      <el-form :model="usernameForm" :rules="usernameRules" ref="usernameFormRef" label-width="100px">
        <el-form-item label="当前用户名">
          <el-input :model-value="profile.username" disabled />
        </el-form-item>
        <el-form-item label="新用户名" prop="username">
          <el-input v-model="usernameForm.username" placeholder="请输入新用户名（4-30个字符）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="usernameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangeUsername" :loading="usernameSubmitting">确认修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="编辑个人信息" width="600px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="editForm.realName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { studentApi, fileApi, profileApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Upload, User } from '@element-plus/icons-vue'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const avatarUploading = ref(false)
const profile = ref({})
const editForm = reactive({ realName: '', phone: '', email: '' })

// 修改密码
const pwdDialogVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 修改用户名
const usernameDialogVisible = ref(false)
const usernameSubmitting = ref(false)
const usernameFormRef = ref()
const usernameForm = reactive({ username: '' })
const usernameRules = {
  username: [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { min: 4, max: 30, message: '用户名长度需在4-30个字符之间', trigger: 'blur' }
  ]
}

const avatarUrl = computed(() => {
  const avatar = profile.value.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http') || avatar.startsWith('/uploads')) {
    return BASE_URL + avatar
  }
  return BASE_URL + '/' + avatar
})

async function fetchProfile() {
  loading.value = true
  try {
    profile.value = await studentApi.getProfile()
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

async function handleAvatarUpload({ file }) {
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await fileApi.upload(formData)
    const avatarPath = res || ''
    profile.value.avatar = avatarPath
    // 同时更新后端用户信息
    await studentApi.updateProfile({ avatar: avatarPath })
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error('上传失败，请重试')
  } finally {
    avatarUploading.value = false
  }
}

function openEditDialog() {
  Object.assign(editForm, {
    realName: profile.value.realName || '',
    phone: profile.value.phone || '',
    email: profile.value.email || ''
  })
  dialogVisible.value = true
}

async function handleSave() {
  submitting.value = true
  try {
    const updated = await studentApi.updateProfile(editForm)
    profile.value = updated
    dialogVisible.value = false
    ElMessage.success('保存成功')
  } catch (e) {} finally {
    submitting.value = false
  }
}

function openPwdDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

async function handleChangePwd() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdSubmitting.value = true
  try {
    await profileApi.changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    pwdDialogVisible.value = false
    ElMessage.success('密码修改成功，请重新登录')
  } catch (e) {} finally {
    pwdSubmitting.value = false
  }
}

function openUsernameDialog() {
  usernameForm.username = ''
  usernameDialogVisible.value = true
}

async function handleChangeUsername() {
  const valid = await usernameFormRef.value.validate().catch(() => false)
  if (!valid) return
  usernameSubmitting.value = true
  try {
    await profileApi.changeUsername(usernameForm.username)
    usernameDialogVisible.value = false
    ElMessage.success('用户名修改成功')
    fetchProfile()
  } catch (e) {} finally {
    usernameSubmitting.value = false
  }
}

function formatStatus(status) {
  const map = {
    studying: '在读',
    graduated: '已毕业',
    suspended: '休学'
  }
  return map[status] || status || '-'
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 10px 0 0;
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.avatar-tip {
  font-size: 12px;
  color: #909399;
}
</style>
