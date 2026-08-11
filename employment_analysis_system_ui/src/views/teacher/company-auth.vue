<template>
  <div class="page-container">
    <div class="page-header">
      <h2>企业入驻审核</h2>
    </div>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>待审核企业资质认证</span>
          <el-tag type="warning" size="small">{{ pendingList.length }} 条待审核</el-tag>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-empty v-else-if="pendingList.length === 0" description="暂无待审核的企业入驻资质" />

      <el-table v-else :data="pendingList" stripe border>
        <el-table-column prop="companyName" label="企业名称" width="180" show-overflow-tooltip />
        <el-table-column prop="deptName" label="入驻学院" width="120" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="authType" label="认证类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ getAuthTypeName(row.authType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="authName" label="文件名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="filePath" label="认证材料" width="100">
          <template #default="{ row }">
            <el-button v-if="row.filePath" type="primary" link size="small" @click="previewFile(row.filePath)">
              查看材料
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" size="small" @click="handleReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="rejectVisible" title="驳回原因" width="400px">
      <el-input v-model="rejectRemark" type="textarea" :rows="3" placeholder="请输入驳回原因（可选）" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="材料预览" width="800px" destroy-on-close>
      <div v-if="previewUrl" class="preview-container">
        <img v-if="isImage" :src="previewUrl" alt="预览图片" class="preview-image" />
        <iframe v-else :src="previewUrl" class="preview-file"></iframe>
      </div>
      <div v-else class="preview-empty">
        <el-empty description="暂无预览内容" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { teacherApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const pendingList = ref([])
const rejectVisible = ref(false)
const rejectRemark = ref('')
const pendingRow = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const isImage = ref(false)

function getAuthTypeName(type) {
  const map = {
    'business_license': '营业执照',
    'tax_cert': '税务登记证',
    'org_cert': '组织机构代码证',
    'other': '其他资质'
  }
  return map[type] || type || '其他'
}

async function loadData() {
  loading.value = true
  try {
    const res = await teacherApi.getPendingCompanyAuths()
    pendingList.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.error('加载待审核列表失败', e)
    pendingList.value = []
  } finally {
    loading.value = false
  }
}

function handleApprove(row) {
  ElMessageBox.confirm(`确认通过企业「${row.companyName}」的资质认证？`, '审核确认', {
    confirmButtonText: '确认通过',
    cancelButtonText: '取消',
    type: 'success'
  }).then(() => {
    teacherApi.auditCompanyAuth(row.id, 'approve').then(() => {
      ElMessage.success('审核通过')
      loadData()
    }).catch(() => {
      ElMessage.error('操作失败')
    })
  }).catch(() => {})
}

function handleReject(row) {
  pendingRow.value = row
  rejectRemark.value = ''
  rejectVisible.value = true
}

function confirmReject() {
  teacherApi.auditCompanyAuth(pendingRow.value.id, 'reject', rejectRemark.value).then(() => {
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadData()
  }).catch(() => {
    ElMessage.error('操作失败')
  })
}

function previewFile(filePath) {
  if (!filePath) return
  // 构建完整的预览URL
  const baseUrl = window.location.origin
  previewUrl.value = filePath.startsWith('http') ? filePath : `${baseUrl}/api/file/preview?path=${encodeURIComponent(filePath)}`
  // 检查是否是图片
  isImage.value = /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(filePath)
  previewVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }

.preview-container { width: 100%; height: 500px; }
.preview-image { max-width: 100%; max-height: 100%; display: block; margin: 0 auto; }
.preview-file { width: 100%; height: 100%; border: none; }
.preview-empty { display: flex; justify-content: center; padding: 60px; }
</style>
