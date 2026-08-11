<template>
  <div class="page-container">
    <div class="page-header">
      <h2>注册码管理</h2>
    </div>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>注册码列表</span>
          <div class="header-actions">
            <el-button type="primary" size="small" @click="handleGenerate" :loading="generating">
              <el-icon><Plus /></el-icon> 生成注册码
            </el-button>
            <el-button size="small" @click="loadData">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-empty v-else-if="records.length === 0" description="暂无注册码，请点击上方按钮生成" />

      <el-table v-else :data="records" stripe border>
        <el-table-column prop="code" label="注册码" width="140">
          <template #default="{ row }">
            <span :class="{ 'code-used': row.status === 'used' }">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'unused' ? 'success' : 'info'" size="small">
              {{ row.status === 'unused' ? '未使用' : '已使用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="usedUsername" label="注册人" width="120">
          <template #default="{ row }">
            <span v-if="row.usedUsername" style="color:#409eff;">{{ row.usedUsername }}</span>
            <span v-else style="color:#c0c4cc;">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="usedTime" label="注册时间" width="160">
          <template #default="{ row }">
            <span v-if="row.usedTime">{{ formatTime(row.usedTime) }}</span>
            <span v-else style="color:#c0c4cc;">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="expiresTime" label="过期时间" width="160">
          <template #default="{ row }">
            <span :style="{ color: isExpired(row) ? '#f56c6c' : '#606266' }">
              {{ row.expiresTime ? formatTime(row.expiresTime) : '永久有效' }}
            </span>
            <el-tag v-if="isExpired(row)" type="danger" size="small" style="margin-left:6px;">已过期</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'unused' && !isExpired(row)"
              type="danger"
              size="small"
              link
              @click="handleDelete(row)"
            >删除</el-button>
            <span v-else style="color:#c0c4cc;font-size:12px;">--</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 生成注册码对话框 -->
    <el-dialog v-model="generateVisible" title="生成注册码" width="500px">
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="生成数量">
          <el-input-number v-model="generateForm.count" :min="1" :max="50" />
          <span style="margin-left:12px;color:#909399;font-size:13px;">个</span>
        </el-form-item>
        <el-form-item label="有效期">
          <el-select v-model="generateForm.expireDays" style="width:200px;">
            <el-option label="1个月" :value="30" />
            <el-option label="3个月" :value="90" />
            <el-option label="6个月" :value="180" />
            <el-option label="1年" :value="365" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="generateForm.remark" type="textarea" :rows="2" placeholder="可选，填写备注信息" />
        </el-form-item>
        <el-alert
          v-if="generatedCodes.length > 0"
          type="success"
          :closable="false"
          style="margin-top:8px;"
        >
          <template #title>
            <div>已生成 {{ generatedCodes.length }} 个注册码：</div>
            <div style="margin-top:6px; font-size:13px; font-family:monospace; word-break:break-all;">
              {{ generatedCodes.join('  |  ') }}
            </div>
          </template>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false; generatedCodes = []">关闭</el-button>
        <el-button type="primary" @click="confirmGenerate" :loading="generating">确认生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Loading, Plus, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const records = ref([])
const generating = ref(false)
const generateVisible = ref(false)
const generatedCodes = ref([])
const generateForm = reactive({
  count: 1,
  expireDays: 90,
  remark: ''
})

function formatTime(time) {
  if (!time) return '-'
  try {
    const str = time.toString()
    return str.substring(0, 19).replace('T', ' ')
  } catch {
    return time
  }
}

function isExpired(row) {
  if (!row.expiresTime) return false
  const now = new Date()
  const expire = new Date(row.expiresTime)
  return expire < now
}

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/admin/invitation-codes/list')
    records.value = Array.isArray(res) ? res : []
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  generateForm.count = 1
  generateForm.expireDays = 90
  generateForm.remark = ''
  generatedCodes.value = []
  generateVisible.value = true
}

async function confirmGenerate() {
  generating.value = true
  try {
    const res = await request.post('/admin/invitation-codes/generate', null, {
      params: {
        count: generateForm.count,
        expireDays: generateForm.expireDays,
        remark: generateForm.remark
      }
    })
    generatedCodes.value = Array.isArray(res) ? res : []
    if (generatedCodes.value.length === 0) {
      ElMessage.warning('生成结果为空')
    }
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '生成失败')
  } finally {
    generating.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除注册码「${row.code}」吗？`, '提示', { type: 'warning' })
    await request.delete(`/admin/invitation-codes/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
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
.header-actions { display: flex; gap: 10px; }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.code-used { color: #c0c4cc; }
</style>
