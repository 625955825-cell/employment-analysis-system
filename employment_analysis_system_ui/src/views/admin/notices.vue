<template>
  <div class="page-container">
    <h2>公告管理</h2>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>公告列表</span>
          <el-button type="primary" @click="openDialog()">发布公告</el-button>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="records.length === 0" class="empty-state">
        <el-empty description="暂无公告" />
      </div>

      <div v-else>
        <el-table :data="records" stripe style="width: 100%">
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="noticeType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ typeText(row.noticeType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetRoles" label="可见范围" width="160">
            <template #default="{ row }">
              <template v-if="!row.targetRoles || row.targetRoles === 'all'">
                <el-tag type="success" size="small">全部用户</el-tag>
              </template>
              <template v-else>
                <el-tag
                  v-for="r in row.targetRoles.split(',')"
                  :key="r"
                  size="small"
                  style="margin-right: 4px;"
                >
                  {{ roleText(r.trim()) }}
                </el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="publisherName" label="发布人" width="100" />
          <el-table-column prop="publishTime" label="发布时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.publishTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="topStatus" label="置顶" width="80">
            <template #default="{ row }">
              <el-tag :type="row.topStatus === '1' ? 'warning' : 'info'" size="small">
                {{ row.topStatus === '1' ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'published' ? 'success' : 'info'" size="small">
                {{ row.status === 'published' ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览量" width="80" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
              <el-button :type="row.topStatus === '1' ? 'info' : 'warning'" link size="small" @click="handleTop(row)">
                {{ row.topStatus === '1' ? '取消置顶' : '置顶' }}
              </el-button>
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
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '发布公告'" width="680px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告类型">
          <el-select v-model="form.noticeType" placeholder="请选择" style="width: 100%;">
            <el-option label="系统通知" value="system" />
            <el-option label="活动通知" value="activity" />
            <el-option label="招聘信息" value="recruit" />
            <el-option label="重要公告" value="important" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见范围">
          <div class="target-roles-wrapper">
            <el-checkbox
              v-model="selectAllRoles"
              @change="handleSelectAllChange"
              style="margin-bottom: 8px;"
            >全选 / 取消全选</el-checkbox>
            <el-checkbox-group v-model="form.targetRoles">
              <el-checkbox value="student">学生</el-checkbox>
              <el-checkbox value="class_teacher">班主任</el-checkbox>
              <el-checkbox value="dept_teacher">院级管理员</el-checkbox>
              <el-checkbox value="admin">校级管理员</el-checkbox>
              <el-checkbox value="company">企业</el-checkbox>
              <el-checkbox value="employment_staff">就业专员</el-checkbox>
            </el-checkbox-group>
            <div class="target-roles-tip">
              <span v-if="form.targetRoles.length === 0 || selectAllRoles" style="color: #67c23a;">
                未选择时默认为全部用户可见
              </span>
              <span v-else style="color: #909399;">
                将仅推送给所选角色，当前选择：{{ form.targetRoles.map(r => roleText(r)).join('、') }}
              </span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="附件图片">
          <div class="image-upload-area" :class="{ 'is-dragover': isDragover }"
            @dragover.prevent="isDragover = true"
            @dragleave.prevent="isDragover = false"
            @drop.prevent="handleDrop"
            @click="triggerFileInput"
          >
            <input ref="fileInputRef" type="file" accept="image/*" multiple style="display: none;" @change="handleFileChange" />
            <div class="upload-hint">
              <el-icon :size="28" color="#c0d0e8"><Plus /></el-icon>
              <span>拖拽图片到这里，或点击上传</span>
              <span class="upload-tip">支持 JPG、PNG、GIF，建议尺寸 800×600，最多 9 张</span>
            </div>
          </div>
          <div v-if="form.images.length > 0" class="image-preview-list">
            <div v-for="(img, idx) in form.images" :key="idx" class="image-preview-item">
              <el-image :src="img.url" fit="cover" class="preview-img" :preview-src-list="form.images.map(i => i.url)" />
              <div class="preview-overlay">
                <el-icon @click.stop="removeImage(idx)"><Delete /></el-icon>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.topStatus" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-radio-group v-model="form.status">
            <el-radio value="published">立即发布</el-radio>
            <el-radio value="draft">存为草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ form.status === 'draft' ? '保存草稿' : '发布' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Loading, Plus, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const records = ref([])
const total = ref(0)
const params = reactive({ page: 1, size: 10 })

const allRoles = ['student', 'class_teacher', 'dept_teacher', 'admin', 'company', 'employment_staff']
const selectAllRoles = ref(true)
const isDragover = ref(false)
const fileInputRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  content: '',
  noticeType: 'system',
  topStatus: '0',
  status: 'published',
  targetRoles: [],
  images: []
})

const typeMap = { system: '系统通知', activity: '活动通知', recruit: '招聘信息', important: '重要公告' }
const roleMap = {
  student: '学生',
  class_teacher: '班主任',
  dept_teacher: '院级管理员',
  admin: '校级管理员',
  company: '企业',
  employment_staff: '就业专员'
}

const typeText = (t) => typeMap[t] || t || '-'
const roleText = (r) => roleMap[r] || r || '-'

function handleSelectAllChange(val) {
  form.targetRoles = val ? [...allRoles] : []
}

function formatDateTime(str) {
  if (!str) return '-'
  try { return str.substring(0, 16).replace('T', ' ') } catch { return str }
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleDrop(e) {
  isDragover.value = false
  const files = Array.from(e.dataTransfer.files).filter(f => f.type.startsWith('image/'))
  uploadFiles(files)
}

function handleFileChange(e) {
  const files = Array.from(e.target.files)
  uploadFiles(files)
  e.target.value = ''
}

function uploadFiles(files) {
  const remaining = 9 - form.images.length
  if (remaining <= 0) { ElMessage.warning('最多上传 9 张图片'); return }
  const toUpload = files.slice(0, remaining)
  toUpload.forEach(file => {
    const formData = new FormData()
    formData.append('file', file)
    request.post('/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then(res => {
        const url = typeof res === 'string' ? res : res?.url || res?.path || res?.data
        if (url) form.images.push({ url, name: file.name })
      })
      .catch(() => ElMessage.error(`图片 ${file.name} 上传失败`))
  })
}

function removeImage(idx) {
  form.images.splice(idx, 1)
}

function loadData() {
  loading.value = true
  request.get('/notice/list', { params }).then(res => {
    records.value = res?.records || res?.list || []
    total.value = res?.total || records.value.length
  }).catch(() => { records.value = []; total.value = 0 })
    .finally(() => { loading.value = false })
}

function openDialog(row = null) {
  isEdit.value = !!row
  if (row) {
    selectAllRoles.value = false
    const roles = row.targetRoles
    form.id = row.id
    form.title = row.title
    form.content = row.content
    form.noticeType = row.noticeType || 'system'
    form.topStatus = row.topStatus || '0'
    form.status = row.status || 'published'
    form.targetRoles = (roles && roles !== 'all') ? roles.split(',').map(r => r.trim()) : [...allRoles]
    form.images = (row.images && Array.isArray(row.images))
      ? row.images.map(img => (typeof img === 'string' ? { url: img, name: '' } : img))
      : []
  } else {
    selectAllRoles.value = true
    Object.assign(form, {
      id: null,
      title: '',
      content: '',
      noticeType: 'system',
      topStatus: '0',
      status: 'published',
      targetRoles: [...allRoles],
      images: []
    })
  }
  dialogVisible.value = true
}

function handleSubmit() {
  if (!form.title || !form.content) { ElMessage.warning('请填写标题和内容'); return }
  submitting.value = true

  const payload = {
    title: form.title,
    content: form.content,
    noticeType: form.noticeType,
    topStatus: form.topStatus,
    status: form.status,
    targetRoles: form.targetRoles.length === allRoles.length ? 'all' : form.targetRoles.join(','),
    images: form.images.map(img => img.url)
  }

  const api = form.id
    ? request.put(`/notice/${form.id}`, payload)
    : request.post('/notice', payload)

  api.then(() => {
    ElMessage.success(form.id ? '更新成功' : '发布成功')
    dialogVisible.value = false
    loadData()
  }).catch(err => ElMessage.error(err.message || '操作失败'))
    .finally(() => { submitting.value = false })
}

function handleTop(row) {
  request.put(`/notice/${row.id}/top`, { topStatus: row.topStatus === '1' ? '0' : '1' })
    .then(() => { ElMessage.success('操作成功'); loadData() })
    .catch(err => ElMessage.error(err.message || '操作失败'))
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除公告 "${row.title}" 吗？`, '提示', { type: 'warning' })
    .then(() => request.delete(`/notice/${row.id}`)
      .then(() => { ElMessage.success('删除成功'); loadData() })
      .catch(err => ElMessage.error(err.message || '删除失败')))
    .catch(() => {})
}

onMounted(() => { loadData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.target-roles-wrapper { padding: 8px 0; }
.target-roles-tip { font-size: 12px; margin-top: 8px; }

.image-upload-area {
  border: 2px dashed #dcdfe6;
  border-radius: 10px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  background: #fafafa;
}
.image-upload-area:hover, .image-upload-area.is-dragover {
  border-color: #2f6bff;
  background: #f0f5ff;
}
.upload-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}
.upload-tip { font-size: 11px; color: #c0d0e8; }

.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
.image-preview-item {
  position: relative;
  width: 90px;
  height: 90px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5eaf3;
}
.preview-img { width: 100%; height: 100%; display: block; }
.preview-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
  font-size: 20px;
}
.image-preview-item:hover .preview-overlay { opacity: 1; }
</style>
