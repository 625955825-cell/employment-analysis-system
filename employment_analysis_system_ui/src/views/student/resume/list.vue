<template>
  <div class="resume-list-container">
    <div class="page-header">
      <h2>我的简历</h2>
      <el-button type="primary" @click="$router.push('/student/resume/create')">
        <el-icon><Plus /></el-icon> 创建简历
      </el-button>
    </div>

    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>

    <div v-else-if="resumes.length === 0" class="empty-container">
      <el-empty description="暂无简历，请创建第一份简历">
        <el-button type="primary" @click="$router.push('/student/resume/create')">创建简历</el-button>
      </el-empty>
    </div>

    <div v-else class="resume-grid">
      <div v-for="resume in resumes" :key="resume.id" class="resume-card">
        <div class="resume-card-header">
          <h3>{{ resume.resumeName || '未命名简历' }}</h3>
          <el-tag v-if="resume.isDefault === '1'" type="success" size="small">默认</el-tag>
        </div>
        <div class="resume-card-body">
          <div class="resume-meta">
            <span><el-icon><Calendar /></el-icon> {{ resume.createTime || '未知' }}</span>
          </div>
          <div class="resume-preview">
            <p v-if="resume.personalSummary">{{ resume.personalSummary.substring(0, 80) }}{{ resume.personalSummary.length > 80 ? '...' : '' }}</p>
            <p v-else class="no-content">暂无简介内容</p>
          </div>
        </div>
        <div class="resume-card-footer">
          <el-button size="small" type="primary" @click="$router.push(`/student/resume/edit/${resume.id}`)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button size="small" @click="handleExport(resume.id)">
            <el-icon><Download /></el-icon> 导出PDF
          </el-button>
          <el-dropdown trigger="click">
            <el-button size="small">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="resume.isDefault !== '1'" @click="handleSetDefault(resume.id)">
                  <el-icon><Star /></el-icon> 设为默认
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleDelete(resume.id)" style="color: #f56c6c;">
                  <el-icon><Delete /></el-icon> 删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { studentApi } from '@/api'
import { Plus, Edit, Download, Delete, Star, Calendar, MoreFilled, Loading } from '@element-plus/icons-vue'

const resumes = ref([])
const loading = ref(true)

async function loadResumes() {
  loading.value = true
  try {
    resumes.value = await studentApi.getResumes()
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleSetDefault(id) {
  try {
    await studentApi.setDefaultResume(id)
    ElMessage.success('设置默认简历成功')
    loadResumes()
  } catch (e) {
    // error handled by interceptor
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除这份简历吗？', '提示', { type: 'warning' })
    await studentApi.deleteResume(id)
    ElMessage.success('删除成功')
    loadResumes()
  } catch (e) {
    if (e !== 'cancel') {
      // error handled by interceptor
    }
  }
}

function handleExport(id) {
  const token = localStorage.getItem('token')
  const url = `${window.location.origin}/api/student/resume/${id}/export`
  const link = document.createElement('a')
  link.href = url
  link.download = ''
  link.style.display = 'none'
  const header = new Headers()
  header.append('Authorization', `Bearer ${token}`)
  fetch(url, { headers: header })
    .then(response => response.blob())
    .then(blob => {
      const url = window.URL.createObjectURL(blob)
      link.href = url
      link.download = `简历_${id}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    })
    .catch(() => {
      ElMessage.error('导出失败，请重试')
    })
}

onMounted(() => {
  loadResumes()
})
</script>

<style scoped>
.resume-list-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.loading-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  font-size: 24px;
}

.resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.resume-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  transition: box-shadow 0.3s;
}

.resume-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.resume-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.resume-card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.resume-card-body {
  margin-bottom: 16px;
}

.resume-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.resume-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.resume-preview p {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.resume-preview .no-content {
  color: #c0c4cc;
  font-style: italic;
}

.resume-card-footer {
  display: flex;
  gap: 8px;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}
</style>
