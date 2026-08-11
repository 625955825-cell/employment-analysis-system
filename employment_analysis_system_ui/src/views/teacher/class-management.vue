<template>
  <div class="page-container">
    <div class="page-header">
      <h2>班级就业管理</h2>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：班级列表 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>班级列表</span>
              <el-tag type="info" size="small">{{ classList.length }} 个班级</el-tag>
            </div>
          </template>

          <div v-if="loadingClasses" class="loading-state">
            <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          </div>

          <div v-else class="class-list">
            <div
              v-for="cls in classList"
              :key="cls.id"
              class="class-item"
              :class="{ active: selectedClass?.id === cls.id }"
              @click="selectClass(cls)"
            >
              <div class="class-info">
                <div class="class-name">{{ cls.className }}</div>
                <div class="class-meta">
                  <span>班主任：{{ cls.advisor || '未分配' }}</span>
                  <span>学生：{{ cls.studentCount }}人</span>
                </div>
              </div>
              <div class="class-stats">
                <div class="employment-rate" :class="getRateClass(cls.employmentRate)">
                  {{ cls.employmentRate || '0' }}%
                </div>
                <div class="rate-label">就业率</div>
              </div>
            </div>

            <el-empty v-if="classList.length === 0" description="暂无班级数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：班级详情和操作 -->
      <el-col :span="16">
        <div v-if="selectedClass" class="detail-section">
          <!-- 班级就业概况 -->
          <el-card shadow="hover" class="stats-card">
            <template #header>
              <div class="card-header">
                <span>「{{ selectedClass.className }}」就业概况</span>
                <el-button type="primary" size="small" @click="showReminderDialog = true">
                  <el-icon><Bell /></el-icon> 发送提醒
                </el-button>
              </div>
            </template>

            <el-row :gutter="16">
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ selectedClass.studentCount || 0 }}</div>
                  <div class="stat-label">班级总人数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value success">{{ selectedClass.employedCount || 0 }}</div>
                  <div class="stat-label">已就业</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value danger">{{ selectedClass.unemployedCount || 0 }}</div>
                  <div class="stat-label">未就业</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value primary">{{ selectedClass.employmentRate || '0' }}%</div>
                  <div class="stat-label">就业率</div>
                </div>
              </el-col>
            </el-row>

            <el-divider />

            <div class="employment-alert" v-if="getAlertLevel()">
              <el-alert
                :title="getAlertMessage()"
                :type="getAlertLevel()"
                :closable="false"
                show-icon
              />
            </div>
          </el-card>

          <!-- 班主任信息 -->
          <el-card shadow="hover" class="teacher-card">
            <template #header>
              <div class="card-header">
                <span>班主任信息</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="姓名">{{ selectedClass.advisor || '未分配' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ selectedClass.advisorPhone || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 发送的提醒记录 -->
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>提醒记录</span>
              </div>
            </template>

            <el-table :data="reminderHistory" stripe>
              <el-table-column prop="createTime" label="发送时间" width="160" />
              <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
              <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
              <el-table-column prop="employmentRate" label="当时就业率" width="100" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.isRead === '1' ? 'success' : 'warning'" size="small">
                    {{ row.isRead === '1' ? '已读' : '未读' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-if="reminderHistory.length === 0" description="暂无提醒记录" />
          </el-card>
        </div>

        <el-card v-else shadow="hover">
          <el-empty description="请从左侧选择班级查看详情">
            <template #image>
              <el-icon :size="60" color="#c0c4cc"><Monitor /></el-icon>
            </template>
          </el-empty>
        </el-card>
      </el-col>
    </el-row>

    <!-- 发送提醒对话框 -->
    <el-dialog v-model="showReminderDialog" title="发送就业提醒" width="500px" destroy-on-close>
      <el-form :model="reminderForm" label-width="100px">
        <el-form-item label="班级">
          <el-input v-model="selectedClass.className" disabled />
        </el-form-item>
        <el-form-item label="班主任">
          <el-input v-model="selectedClass.advisor" disabled />
        </el-form-item>
        <el-form-item label="当前就业率">
          <el-input :value="selectedClass.employmentRate + '%'" disabled />
        </el-form-item>
        <el-form-item label="提醒标题" required>
          <el-input v-model="reminderForm.title" placeholder="请输入提醒标题" />
        </el-form-item>
        <el-form-item label="提醒内容" required>
          <el-input v-model="reminderForm.content" type="textarea" :rows="4" placeholder="请输入提醒内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReminderDialog = false">取消</el-button>
        <el-button type="primary" @click="sendReminder" :loading="sending">发送提醒</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Loading, Bell, Monitor } from '@element-plus/icons-vue'
import { teacherApi } from '@/api'
import { ElMessage } from 'element-plus'

const loadingClasses = ref(false)
const classList = ref([])
const selectedClass = ref(null)
const showReminderDialog = ref(false)
const sending = ref(false)
const reminderHistory = ref([])
const reminderForm = reactive({
  title: '',
  content: ''
})

async function loadClasses() {
  loadingClasses.value = true
  try {
    const res = await teacherApi.getClasses()
    classList.value = Array.isArray(res) ? res : []
    // 为每个班级计算就业率
    for (const cls of classList.value) {
      try {
        const stats = await teacherApi.getClassEmploymentStats(cls.id)
        cls.employedCount = stats.employed || 0
        cls.unemployedCount = stats.unemployed || 0
        cls.employmentRate = stats.employmentRate || '0'
      } catch {
        cls.employedCount = 0
        cls.unemployedCount = cls.studentCount || 0
        cls.employmentRate = '0'
      }
    }
  } catch (e) {
    console.error('加载班级列表失败', e)
  } finally {
    loadingClasses.value = false
  }
}

async function selectClass(cls) {
  selectedClass.value = cls
  // 加载该班级的提醒记录
  try {
    const res = await teacherApi.getEmploymentReminders()
    reminderHistory.value = (res || []).filter(r => r.classId === cls.id)
  } catch {
    reminderHistory.value = []
  }
}

function getRateClass(rate) {
  const r = parseFloat(rate) || 0
  if (r >= 80) return 'rate-high'
  if (r >= 60) return 'rate-medium'
  return 'rate-low'
}

function getAlertLevel() {
  if (!selectedClass.value) return null
  const rate = parseFloat(selectedClass.value.employmentRate) || 0
  if (rate < 50) return 'error'
  if (rate < 70) return 'warning'
  return 'success'
}

function getAlertMessage() {
  if (!selectedClass.value) return ''
  const rate = parseFloat(selectedClass.value.employmentRate) || 0
  if (rate < 50) return `【紧急】${selectedClass.value.className} 就业率偏低（${rate}%），请重点关注！`
  if (rate < 70) return `【提示】${selectedClass.value.className} 就业率偏低（${rate}%），建议关注。`
  return `${selectedClass.value.className} 就业情况良好，继续保持！`
}

async function sendReminder() {
  if (!reminderForm.title.trim()) {
    ElMessage.warning('请输入提醒标题')
    return
  }
  if (!reminderForm.content.trim()) {
    ElMessage.warning('请输入提醒内容')
    return
  }

  sending.value = true
  try {
    await teacherApi.sendEmploymentReminder({
      classId: selectedClass.value.id,
      receiverId: selectedClass.value.advisorUserId,
      title: reminderForm.title,
      content: reminderForm.content,
      employmentRate: selectedClass.value.employmentRate,
      totalStudents: selectedClass.value.studentCount,
      employedStudents: selectedClass.value.employedCount,
      unemployedStudents: selectedClass.value.unemployedCount
    })
    ElMessage.success('提醒已发送')
    showReminderDialog.value = false
    reminderForm.title = ''
    reminderForm.content = ''
    // 刷新提醒记录
    selectClass(selectedClass.value)
  } catch (e) {
    console.error('发送提醒失败', e)
    ElMessage.error('发送失败，请重试')
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  loadClasses()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.card-header { display: flex; justify-content: space-between; align-items: center; }

.class-list { max-height: 600px; overflow-y: auto; }
.class-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}
.class-item:hover { background: #f5f7fa; }
.class-item.active { border-color: #409eff; background: #ecf5ff; }

.class-info { flex: 1; }
.class-name { font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.class-meta { font-size: 12px; color: #909399; display: flex; gap: 12px; }

.class-stats { text-align: center; margin-left: 16px; }
.employment-rate { font-size: 20px; font-weight: 700; }
.rate-high { color: #67c23a; }
.rate-medium { color: #e6a23c; }
.rate-low { color: #f56c6c; }
.rate-label { font-size: 12px; color: #909399; }

.loading-state { display: flex; justify-content: center; padding: 40px; }

.detail-section { display: flex; flex-direction: column; gap: 16px; }

.stats-card .stat-item { text-align: center; padding: 16px 0; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
.stat-value.success { color: #67c23a; }
.stat-value.danger { color: #f56c6c; }
.stat-value.primary { color: #409eff; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }

.employment-alert { margin-top: 16px; }

.teacher-card { margin-top: 0; }
</style>
