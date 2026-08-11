<template>
  <div class="page-container">
    <h2>我的收藏</h2>

    <el-card style="margin-top: 16px;">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="favorites.length === 0" class="empty-state">
        <el-empty description="暂无收藏职位" :image-size="80">
          <el-button type="primary" @click="$router.push('/student/job-search')">去搜索职位</el-button>
        </el-empty>
      </div>

      <div v-else class="job-list">
        <div v-for="item in favorites" :key="item.id" class="job-card">
          <div class="job-main">
            <div class="job-title-row">
              <span class="job-name" @click="$router.push(`/student/job-detail/${item.jobId}`)">
                {{ item.jobName }}
              </span>
              <el-tag v-if="item.status === 'published'" type="success" size="small">招聘中</el-tag>
              <el-tag v-else type="info" size="small">已下架</el-tag>
            </div>
            <div class="company-name">{{ item.companyName }}</div>
            <div class="job-tags">
              <span class="tag salary" v-if="item.salaryMin && item.salaryMax">
                {{ formatSalaryK(item.salaryMin) }}～{{ formatSalaryK(item.salaryMax) }}元/月
              </span>
              <span class="tag" v-if="item.workCity">{{ item.workCity }}</span>
              <span class="tag" v-if="item.deadline">截止: {{ item.deadline?.substring(0, 10) }}</span>
            </div>
          </div>
          <div class="job-side">
            <el-button type="primary" size="small" @click="$router.push(`/student/job-detail/${item.jobId}`)">查看详情</el-button>
            <el-button type="danger" size="small" plain @click="handleUnfavorite(item)">取消收藏</el-button>
          </div>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="params.page"
            v-model:page-size="params.size"
            :page-sizes="[10, 20, 30]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadFavorites"
            @current-change="loadFavorites"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { studentApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const favorites = ref([])
const total = ref(0)
const params = reactive({ page: 1, size: 10 })

function loadFavorites() {
  loading.value = true
  studentApi.getFavorites(params).then(res => {
    favorites.value = res?.records || []
    total.value = res?.total || 0
  }).catch(() => {
    favorites.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function handleUnfavorite(item) {
  studentApi.unfavoriteJob(item.jobId).then(() => {
    ElMessage.success('已取消收藏')
    loadFavorites()
  }).catch(err => {
    ElMessage.error(err.message || '操作失败')
  })
}

function formatSalaryK(v) {
  const n = parseInt(v)
  if (isNaN(n)) return v
  if (n >= 10000) return (n / 1000) + 'K'
  return v
}

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #999; gap: 12px; }
.job-list { display: flex; flex-direction: column; }
.job-card { display: flex; justify-content: space-between; align-items: center; padding: 16px 0; border-bottom: 1px solid #f0f0f0; gap: 16px; }
.job-card:last-child { border-bottom: none; }
.job-main { flex: 1; min-width: 0; }
.job-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.job-name { font-size: 16px; font-weight: 600; color: #333; cursor: pointer; }
.job-name:hover { color: #409eff; }
.company-name { font-size: 14px; color: #666; margin-bottom: 8px; }
.job-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.tag { font-size: 12px; color: #666; background: #f5f5f5; padding: 2px 8px; border-radius: 4px; }
.tag.salary { color: #f56c6c; font-weight: 600; background: #fef0f0; }
.job-side { display: flex; gap: 8px; flex-shrink: 0; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
