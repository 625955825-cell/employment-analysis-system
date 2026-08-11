<template>
  <div class="page-container">
    <div class="page-header">
      <h2>专业关键词爬虫管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="loadTasks">
          <el-icon><Refresh /></el-icon> 刷新状态
        </el-button>
      </div>
    </div>

    <!-- ==================== 专业关键词配置 ==================== -->
    <el-card shadow="hover" class="config-card">
      <template #header>
        <div class="card-header">
          <span>学院专业关键词配置</span>
          <div class="card-header-right">
            <el-tag type="success" size="small">共 {{ majorKeywords.length }} 个专业 · {{ totalKeywords }} 个关键词</el-tag>
            <el-button size="small" text type="primary" @click="toggleAll" style="margin-left: 8px;">
              {{ isAllExpanded ? '全部收起' : '全部展开' }}
            </el-button>
          </div>
        </div>
      </template>
      <el-collapse v-model="activeMajorCollapse">
        <el-collapse-item v-for="item in displayedMajors" :key="item.major" :name="item.major">
          <template #title>
            <div class="major-title">
              <span class="major-name">{{ item.major }}</span>
              <el-tag size="small" type="info">{{ item.count }} 个关键词</el-tag>
            </div>
          </template>
          <div class="keyword-section">
            <!-- 关键词标签展示 -->
            <div class="keyword-tags">
              <el-tag
                v-for="kw in item.keywords"
                :key="kw"
                size="small"
                style="margin: 4px;"
                closable
                @close="handleDeleteKeyword(item.major, kw)"
              >{{ kw }}</el-tag>
            </div>
            <!-- 新增关键词输入 -->
            <div class="keyword-add-row">
              <el-input
                v-model="keywordInputMap[item.major]"
                :placeholder="`添加「${item.major}」关键词`"
                size="small"
                style="width: 200px; margin-right: 8px;"
                @keyup.enter="handleAddKeyword(item.major)"
              />
              <el-button size="small" type="primary" plain @click="handleAddKeyword(item.major)" :loading="keywordLoadingMap[item.major]">
                添加
              </el-button>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
      <div v-if="majorKeywords.length > 5" style="text-align: center; padding: 10px 0 4px;">
        <el-button size="small" text type="primary" @click="showAllMajors = !showAllMajors">
          {{ showAllMajors ? '收起更多 ∧' : `展开剩余 ${majorKeywords.length - 5} 个专业 ∨` }}
        </el-button>
      </div>
    </el-card>

    <!-- ==================== 爬虫任务控制 ==================== -->
    <el-card shadow="hover" class="control-card">
      <template #header>
        <div class="card-header">
          <span>爬虫任务</span>
          <div class="header-actions">
            <el-select v-model="selectedSource" placeholder="选择数据源" style="width: 150px; margin-right: 8px;">
              <el-option
                v-for="s in dataSources"
                :key="s.code"
                :label="s.name"
                :value="s.code"
              />
            </el-select>
            <el-select v-model="crawlDepth" placeholder="爬取深度" style="width: 120px; margin-right: 8px;">
              <el-option label="爬1页" :value="1" />
              <el-option label="爬2页" :value="2" />
              <el-option label="爬3页" :value="3" />
              <el-option label="爬5页" :value="5" />
              <el-option label="爬10页" :value="10" />
              <el-option label="爬20页" :value="20" />
              <el-option label="爬50页" :value="50" />
            </el-select>
            <el-button
              type="info"
              plain
              @click="majorDialogVisible = true"
              style="margin-right: 8px;"
            >
              <el-icon><Document /></el-icon>
              {{ selectedMajors.length > 0 ? `已选 ${selectedMajors.length} 个专业` : '选择专业' }}
            </el-button>
            <el-button
              v-if="!isRunning"
              type="success"
              @click="startCrawl"
              :loading="starting"
              :disabled="!selectedSource"
            >
              <el-icon><VideoPlay /></el-icon> 开启爬虫
            </el-button>
            <el-button
              v-else
              type="danger"
              @click="safeStopCrawl"
              :loading="stopping"
            >
              <el-icon><VideoPause /></el-icon> 安全停止
            </el-button>
          </div>
        </div>
      </template>

        <!-- 实时进度 -->
      <div v-if="isRunning || progressData.progress > 0" class="progress-section">
        <div class="progress-header">
          <span class="progress-label">
            <el-icon class="is-loading" v-if="isRunning"><Loading /></el-icon>
            {{ isRunning ? '爬取中...' : '爬取完成' }}
          </span>
          <div class="progress-header-right">
            <span class="progress-percent">{{ progressData.progress || 0 }}%</span>
            <el-button v-if="isRunning" size="small" type="primary" plain @click="showProgressDialog = true">
              <el-icon><Monitor /></el-icon> 详情
            </el-button>
          </div>
        </div>
        <el-progress
          :percentage="progressData.progress || 0"
          :stroke-width="14"
          :color="progressColor"
          :status="progressData.progress >= 100 ? 'success' : undefined"
        >
          <span class="progress-text">{{ progressData.collectedCount || 0 }} 条数据</span>
        </el-progress>
        <div class="progress-info">
          <span>数据源: {{ getSourceName(selectedSource) }}</span>
          <span>爬取深度: {{ crawlDepth }} 页/专业</span>
          <span>爬取专业: {{ totalKeywords }} 个</span>
        </div>
        <!-- 详细进度信息 -->
        <div v-if="isRunning && progressData.currentMajor" class="progress-detail">
          <div class="detail-item">
            <el-icon><Document /></el-icon>
            <span class="detail-label">当前专业:</span>
            <span class="detail-value">{{ progressData.currentMajor }}</span>
            <span class="detail-index">({{ progressData.majorIndex || 0 }}/{{ progressData.totalMajors || 0 }})</span>
          </div>
          <div class="detail-item">
            <el-icon><Search /></el-icon>
            <span class="detail-label">当前关键词:</span>
            <span class="detail-value">{{ progressData.currentKeyword }}</span>
            <span class="detail-index">({{ progressData.keywordIndex || 0 }}/{{ progressData.majorKeywordsCount || 0 }})</span>
          </div>
          <div class="detail-item">
            <el-icon><DocumentCopy /></el-icon>
            <span class="detail-label">当前页数:</span>
            <span class="detail-value">第 {{ progressData.currentPage || 0 }} 页</span>
            <span class="detail-index">/ 共 {{ progressData.maxPages || 0 }} 页</span>
          </div>
        </div>
      </div>

      <!-- 安全停止说明 -->
      <div v-if="isRunning" class="stop-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>点击"安全停止"后，系统将等待当前页爬取完成后退出，不会污染训练数据池</span>
      </div>

      <!-- 任务统计 -->
      <div class="task-stats">
        <div class="stat-item">
          <span class="stat-value">{{ taskStats.total }}</span>
          <span class="stat-label">任务总数</span>
        </div>
        <div class="stat-item">
          <span class="stat-value" style="color:#67c23a">{{ taskStats.running }}</span>
          <span class="stat-label">运行中</span>
        </div>
        <div class="stat-item">
          <span class="stat-value" style="color:#409eff">{{ taskStats.success }}</span>
          <span class="stat-label">已完成</span>
        </div>
        <div class="stat-item">
          <span class="stat-value" style="color:#e6a23c">{{ taskStats.pending }}</span>
          <span class="stat-label">等待中</span>
        </div>
        <div class="stat-item">
          <span class="stat-value" style="color:#f56c6c">{{ taskStats.failed }}</span>
          <span class="stat-label">失败</span>
        </div>
      </div>
    </el-card>

    <!-- ==================== 数据清洗与训练池 ==================== -->
    <el-row :gutter="16" class="stats-row">
      <!-- ETL控制 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>数据清洗 (ETL)</span>
              <el-button type="primary" size="small" @click="executeEtl" :loading="etlLoading">
                <el-icon><Refresh /></el-icon> 执行清洗
              </el-button>
            </div>
          </template>
          <div class="etl-desc">
            <p>ETL 将过滤以下脏数据，防止污染推荐训练池：</p>
            <ul>
              <li>薪资字段为空的记录（最重要）</li>
              <li>职位名称为空或过短的记录</li>
              <li>薪资字段异常超长的数据</li>
              <li>公司名称不完整的数据</li>
              <li>原始数据包含"验证码/反爬"字样的记录</li>
              <li>与训练池或职位表中重复的记录</li>
            </ul>
          </div>
          <div v-if="etlResult" class="etl-result">
            <el-divider content-position="left">清洗结果</el-divider>
            <div class="etl-stats">
              <span>处理总数: <strong>{{ etlResult.totalProcessed || 0 }}</strong></span>
              <span>有效数据: <strong style="color:#67c23a">{{ etlResult.cleanedCount || 0 }}</strong></span>
              <span>重复记录: <strong style="color:#e6a23c">{{ etlResult.duplicateCount || 0 }}</strong></span>
              <span>脏数据: <strong style="color:#f56c6c">{{ etlResult.pollutedCount || 0 }}</strong></span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 训练池统计 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>训练池统计</span>
              <el-button type="success" size="small" @click="syncAllToPool" :loading="syncing">
                <el-icon><Refresh /></el-icon> 一键同步全部
              </el-button>
            </div>
          </template>
          <div class="pool-stats">
            <div class="pool-stat-item">
              <div class="pool-stat-icon" style="background:#409eff;"><el-icon><DataLine /></el-icon></div>
              <div class="pool-stat-info">
                <p class="pool-stat-label">待清洗数据</p>
                <p class="pool-stat-value">{{ poolStats.pendingCount || 0 }}</p>
              </div>
            </div>
            <div class="pool-stat-item">
              <div class="pool-stat-icon" style="background:#67c23a;"><el-icon><CircleCheck /></el-icon></div>
              <div class="pool-stat-info">
                <p class="pool-stat-label">训练池数据</p>
                <p class="pool-stat-value">{{ poolStats.validCount || 0 }}</p>
              </div>
            </div>
            <div class="pool-stat-item">
              <div class="pool-stat-icon" style="background:#e6a23c;"><el-icon><TrendCharts /></el-icon></div>
              <div class="pool-stat-info">
                <p class="pool-stat-label">已同步职位</p>
                <p class="pool-stat-value">{{ poolStats.syncedToJobCount || 0 }}</p>
              </div>
            </div>
          </div>
          <!-- 训练池分布 -->
          <div v-if="poolStats.industryDistribution" class="pool-dist">
            <p class="dist-title">行业分布 TOP8</p>
            <div v-for="(item, idx) in (poolStats.industryDistribution || []).slice(0, 8)" :key="idx" class="dist-item">
              <span class="dist-label">{{ item.industry }}</span>
              <el-progress :percentage="Math.round((item.count / maxPoolCount) * 100)" :stroke-width="8" :show-text="false" style="flex:1; margin: 0 8px;" />
              <span class="dist-value">{{ item.count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 数据质量分析（基于爬取+ETL清洗数据） ==================== -->
    <el-card shadow="hover" class="national-card">
      <template #header>
        <div class="card-header">
          <span>数据质量分析（基于爬取+ETL清洗数据）</span>
          <el-button size="small" @click="loadNationalStats" :loading="statsLoading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <!-- 整体统计卡片 -->
      <el-row :gutter="12" style="margin-bottom: 16px;">
        <el-col :span="6">
          <div class="stat-mini-card">
            <div class="stat-mini-value" style="color:#409eff;">{{ (nationalStats.overall || {}).totalCollected || 0 }}</div>
            <div class="stat-mini-label">总采集量</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-mini-card">
            <div class="stat-mini-value" style="color:#67c23a;">{{ (nationalStats.overall || {}).totalValid || 0 }}</div>
            <div class="stat-mini-label">有效数据（已清洗）</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-mini-card">
            <div class="stat-mini-value" style="color:#e6a23c;">{{ (nationalStats.overall || {}).totalPending || 0 }}</div>
            <div class="stat-mini-label">待清洗</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-mini-card">
            <div class="stat-mini-value" style="color:#909399;">{{ (nationalStats.overall || {}).validRate || 0 }}%</div>
            <div class="stat-mini-label">有效率</div>
          </div>
        </el-col>
      </el-row>

      <!-- 专业数据质量表格 -->
      <el-divider content-position="left">各专业数据质量</el-divider>
      <el-table :data="(nationalStats.majorStats || [])" stripe size="small" max-height="300">
        <el-table-column prop="majorName" label="专业名称" min-width="160" />
        <el-table-column prop="collected" label="采集量" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.collected }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="valid" label="有效数据" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.valid > 0 ? 'success' : 'info'" size="small">{{ row.valid }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pending" label="待清洗" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small">{{ row.pending }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="validPercent" label="占比%" width="80" align="center">
          <template #default="{ row }">
            <span style="color:#909399;font-size:12px;">{{ row.validPercent }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="数据状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === '充足' ? 'success' : row.status === '一般' ? 'warning' : row.status === '不足' ? 'danger' : 'info'"
              size="small"
            >{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建议" min-width="140">
          <template #default="{ row }">
            <span v-if="row.status === '空白'" style="color:#f56c6c;font-size:12px;">⚠ 该专业完全没有有效数据，请立即爬取</span>
            <span v-else-if="row.status === '不足'" style="color:#e6a23c;font-size:12px;">⚠ 数据较少，建议补充爬取</span>
            <span v-else-if="row.status === '一般'" style="color:#909399;font-size:12px;">✓ 数据量尚可，可继续爬取</span>
            <span v-else style="color:#67c23a;font-size:12px;">✓ 数据充足</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!(nationalStats.majorStats && nationalStats.majorStats.length > 0) && !statsLoading" style="text-align:center;padding:30px;color:#909399;">
        暂无专业数据，请先执行爬取任务
      </div>

      <!-- 城市分布 + 行业分布 -->
      <el-divider content-position="left">数据分布</el-divider>
      <el-row :gutter="16" class="position-row">
        <el-col :span="12">
          <div class="position-chart" ref="positionChartRef"></div>
        </el-col>
        <el-col :span="12">
          <div class="position-chart" ref="industryChartRef"></div>
        </el-col>
      </el-row>

      <!-- 学历分布 + 每日趋势 -->
      <el-divider content-position="left">学历要求 &amp; 采集趋势</el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <div class="position-chart" ref="eduChartRef"></div>
        </el-col>
        <el-col :span="12">
          <div class="position-chart" ref="trendChartRef"></div>
        </el-col>
      </el-row>
    </el-card>

    <!-- ==================== 采集数据管理 ==================== -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>已采集数据</span>
          <div class="header-actions">
            <el-select v-model="filterSourceCode" placeholder="数据源" clearable size="small" style="width: 130px; margin-right: 8px;" @change="loadCollectedData">
              <el-option v-for="s in dataSources" :key="s.code" :label="s.name" :value="s.code" />
            </el-select>
            <el-select v-model="filterMajor" placeholder="关联专业" clearable size="small" style="width: 150px; margin-right: 8px;" @change="loadCollectedData">
              <el-option v-for="m in majorKeywords" :key="m.major" :label="m.major" :value="m.major" />
            </el-select>
            <el-button
              v-if="selectedDataIds.length > 0"
              type="danger"
              size="small"
              @click="batchDeleteData"
              style="margin-right: 8px;"
            >
              <el-icon><Delete /></el-icon> 删除已选 ({{ selectedDataIds.length }})
            </el-button>
            <el-button type="danger" size="small" @click="clearAllData" :loading="clearingData">
              <el-icon><Delete /></el-icon> 一键清空
            </el-button>
            <el-button type="primary" size="small" @click="loadCollectedData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="collectedDataList" stripe border max-height="400" v-loading="loadingData" @selection-change="onDataSelectionChange">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="jobName" label="职位名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="companyName" label="公司" min-width="130" show-overflow-tooltip />
        <el-table-column prop="salary" label="薪资" width="100" />
        <el-table-column prop="city" label="城市" width="80" />
        <el-table-column prop="majorName" label="关联专业" min-width="130" show-overflow-tooltip />
        <el-table-column prop="responsibility" label="岗位职责" min-width="200" show-overflow-tooltip />
        <el-table-column prop="education" label="学历" width="80" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isSynced === '1' ? 'success' : 'info'" size="small">
              {{ row.isSynced === '1' ? '已入训练池' : '待清洗' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="collectTime" label="采集时间" width="145" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="viewDataDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 12px; display: flex; justify-content: center;">
        <el-pagination
          v-model:current-page="dataPage"
          v-model:page-size="dataPageSize"
          :page-sizes="[10, 20, 50]"
          :total="dataTotal"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- 进度详情对话框 -->
    <el-dialog v-model="showProgressDialog" title="爬取进度详情" width="650px" destroy-on-close>
      <div v-if="progressData">
        <div class="dialog-progress">
          <el-progress :percentage="progressData.progress || 0" :stroke-width="20" :color="progressColor" />
        </div>
        <el-descriptions :column="2" border size="small" style="margin-top: 16px;">
          <el-descriptions-item label="当前状态">
            <el-tag :type="isRunning ? 'primary' : 'success'" size="small">{{ isRunning ? '爬取中' : '已完成' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="数据源">{{ getSourceName(selectedSource) }}</el-descriptions-item>
          <el-descriptions-item label="爬取深度">{{ crawlDepth }} 页/专业</el-descriptions-item>
          <el-descriptions-item label="已采集数据">{{ progressData.collectedCount || 0 }} 条</el-descriptions-item>
        </el-descriptions>
        <!-- 详细进度信息 -->
        <el-card shadow="never" style="margin-top: 16px; background: #f5f7fa;">
          <template #header>
            <span style="font-weight: 600; color: #303133;">实时进度详情</span>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="当前专业">
              <span style="font-weight: 600; color: #409eff;">{{ progressData.currentMajor || '初始化中...' }}</span>
              <el-tag size="small" style="margin-left: 8px;">{{ progressData.majorIndex || 0 }} / {{ progressData.totalMajors || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前关键词">
              <span style="font-weight: 600;">{{ progressData.currentKeyword || '初始化中...' }}</span>
              <el-tag size="small" type="info" style="margin-left: 8px;">{{ progressData.keywordIndex || 0 }} / {{ progressData.majorKeywordsCount || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前页数">
              <span>第 {{ progressData.currentPage || 0 }} 页 / 共 {{ progressData.maxPages || 0 }} 页</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
        <!-- 进度提示 -->
        <div v-if="isRunning" class="crawl-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>爬取进行中，请勿关闭页面。每3秒自动刷新进度...</span>
        </div>
      </div>
    </el-dialog>

    <!-- 数据详情对话框 -->
    <el-dialog v-model="showDataDetailDialog" title="数据详情" width="620px" destroy-on-close>
      <el-descriptions v-if="currentData" :column="1" border size="small">
        <el-descriptions-item label="职位名称">{{ currentData.jobName }}</el-descriptions-item>
        <el-descriptions-item label="公司名称">{{ currentData.companyName }}</el-descriptions-item>
        <el-descriptions-item label="薪资">{{ currentData.salary }}</el-descriptions-item>
        <el-descriptions-item label="城市">{{ currentData.city }}</el-descriptions-item>
        <el-descriptions-item label="学历要求">{{ currentData.education }}</el-descriptions-item>
        <el-descriptions-item label="关联专业">{{ currentData.majorName }}</el-descriptions-item>
        <el-descriptions-item label="数据来源">{{ getSourceName(currentData.sourceCode) }}</el-descriptions-item>
        <el-descriptions-item label="采集时间">{{ currentData.collectTime }}</el-descriptions-item>
        <el-descriptions-item label="训练池状态">
          <el-tag :type="currentData.isSynced === '1' ? 'success' : 'info'" size="small">
            {{ currentData.isSynced === '1' ? '已加入训练池' : '待清洗/待同步' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="职位信息">{{ currentData.responsibility }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 专业多选弹窗 -->
    <el-dialog v-model="majorDialogVisible" title="选择爬取专业" width="640px" destroy-on-close>
      <div style="margin-bottom: 12px;">
        <el-checkbox
          v-model="selectAll"
          :indeterminate="selectedMajors.length > 0 && selectedMajors.length < allMajors.length"
          @change="toggleSelectAll"
        >全选 / 全不选</el-checkbox>
        <span style="margin-left: 12px; color: #909399; font-size: 12px;">
          已选 {{ selectedMajors.length }} / {{ allMajors.length }} 个专业
          <span v-if="selectedMajors.length === 0" style="color: #e6a23c;">（空 = 爬取全部专业）</span>
        </span>
      </div>
      <el-checkbox-group v-model="selectedMajors">
        <el-row :gutter="8">
          <el-col v-for="major in allMajors" :key="major" :span="12" style="margin-bottom: 6px;">
            <el-checkbox :label="major" :value="major" style="width: 100%;">{{ major }}</el-checkbox>
          </el-col>
        </el-row>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="selectedMajors = []">清空选择</el-button>
        <el-button type="primary" @click="majorDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, VideoPlay, VideoPause, Loading, DataLine, CircleCheck,
  TrendCharts, InfoFilled, Document, Search, DocumentCopy, Monitor, Delete
} from '@element-plus/icons-vue'
import { spiderApi } from '@/api'

// ==================== 状态 ====================
const activeMajorCollapse = ref([])
const showAllMajors = ref(false)
const selectedSource = ref('51job')
const crawlDepth = ref(2)
const selectedMajors = ref([])
const allMajors = ref([])
const majorDialogVisible = ref(false)
const selectedDataIds = ref([])
const filterSourceCode = ref('')
const filterMajor = ref('')
const starting = ref(false)
const stopping = ref(false)
const etlLoading = ref(false)
const syncing = ref(false)
const clearingData = ref(false)
const loadingData = ref(false)
const statsLoading = ref(false)
const showProgressDialog = ref(false)
const showDataDetailDialog = ref(false)
const currentData = ref(null)
const currentPhase = ref('')
const progressPollingTimer = ref(null)

// ==================== 数据 ====================
const dataSources = ref([
  { code: '51job', name: '前程无忧', color: '#ff8b00' },
  { code: 'gov', name: '人社部公共招聘', color: '#409eff' },
  { code: 'moe', name: '教育部就业平台', color: '#67c23a' },
  { code: 'yingjiesheng', name: '应届生求职网', color: '#e6a23c' }
])
const majorKeywords = ref([])
const totalKeywords = computed(() => majorKeywords.value.reduce((sum, m) => sum + (m.count || 0), 0))
const keywordInputMap = ref({})    // { majorName: inputValue }
const keywordLoadingMap = ref({})  // { majorName: loading }
const taskStats = reactive({ total: 0, running: 0, success: 0, pending: 0, failed: 0 })
const progressData = ref({})
const etlResult = ref(null)
const poolStats = ref({})
const nationalStats = ref({})
const collectedDataList = ref([])
const dataPage = ref(1)
const dataPageSize = ref(10)
const dataTotal = ref(0)

// ==================== 图表 ====================
const positionChartRef = ref(null)
const industryChartRef = ref(null)
const eduChartRef = ref(null)
const trendChartRef = ref(null)
let positionChart = null
let industryChart = null
let eduChart = null
let trendChart = null

// ==================== 计算属性 ====================
const isRunning = computed(() =>
  (progressData.value?.isRunning === true || starting.value) && taskStats.running > 0
)

const selectAll = computed({
  get: () => allMajors.value.length > 0 && selectedMajors.value.length === allMajors.value.length,
  set: () => {}
})

function toggleSelectAll(val) {
  selectedMajors.value = val ? [...allMajors.value] : []
}

const progressColor = computed(() => {
  const p = progressData.value?.progress || 0
  if (p < 30) return '#f56c6c'
  if (p < 70) return '#e6a23c'
  return '#67c23a'
})

const maxPoolCount = computed(() => {
  const dist = poolStats.value?.industryDistribution || []
  return Math.max(...dist.map(d => d.count || 0), 1)
})

const isAllExpanded = computed(() => {
  const displayed = displayedMajors.value
  return displayed.length > 0 && activeMajorCollapse.value.length === displayed.length
})

const displayedMajors = computed(() =>
  showAllMajors.value ? majorKeywords.value : majorKeywords.value.slice(0, 5)
)

function toggleAll() {
  const current = showAllMajors.value ? majorKeywords.value : majorKeywords.value.slice(0, 5)
  if (activeMajorCollapse.value.length === current.length) {
    activeMajorCollapse.value = []
  } else {
    activeMajorCollapse.value = current.map(m => m.major)
  }
}

// ==================== 加载专业关键词 ====================
async function loadMajorKeywords() {
  try {
    const [keywordsRes, majorsRes] = await Promise.all([
      spiderApi.getMajorKeywords(),
      spiderApi.getAllMajors()
    ])
    majorKeywords.value = Array.isArray(keywordsRes) ? keywordsRes : (keywordsRes?.data || [])
    allMajors.value = Array.isArray(majorsRes) ? majorsRes : (majorsRes?.data || [])
  } catch {
    majorKeywords.value = []
    allMajors.value = []
  }
}

// ==================== 关键词增删 ====================
async function handleAddKeyword(majorName) {
  const input = keywordInputMap.value[majorName]
  if (!input || !input.trim()) {
    ElMessage.warning('请输入关键词')
    return
  }
  const keywords = input.split(/[,，;；\n]/)
    .map(k => k.trim())
    .filter(k => k.length > 0)
  if (keywords.length === 0) return

  keywordLoadingMap.value[majorName] = true
  try {
    if (keywords.length === 1) {
      await spiderApi.addKeyword(majorName, keywords[0])
      ElMessage.success('关键词添加成功')
    } else {
      const res = await spiderApi.addKeywords(majorName, keywords)
      ElMessage.success(res?.message || `成功添加 ${keywords.length} 个关键词`)
    }
    keywordInputMap.value[majorName] = ''
    await loadMajorKeywords()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || '添加失败')
  } finally {
    keywordLoadingMap.value[majorName] = false
  }
}

async function handleDeleteKeyword(majorName, keyword) {
  try {
    await ElMessageBox.confirm(`确定删除关键词「${keyword}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await spiderApi.deleteKeyword(majorName, keyword)
    ElMessage.success('关键词已删除')
    await loadMajorKeywords()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '删除失败')
  }
}

// ==================== 加载任务列表 ====================
async function loadTasks() {
  // 先重置进度数据，避免残留旧的脏数据（如之前任务遗留的 260% 进度）
  progressData.value = {}

  try {
    const res = await spiderApi.getTasks({ page: 1, pageSize: 100 })
    const list = res?.list || res?.data?.list || []
    taskStats.total = res?.total || list.length
    taskStats.running = list.filter(t => t.status === 'running').length
    taskStats.success = list.filter(t => t.status === 'success').length
    taskStats.pending = list.filter(t => t.status === 'pending').length
    taskStats.failed = list.filter(t => t.status === 'failed').length

    // 检查是否有正在运行的任务（仅用于恢复进度，不覆盖用户选择）
    const runningTask = list.find(t => t.status === 'running')
    if (runningTask) {
      startProgressPolling(runningTask.id)
    }
  } catch (e) {
    console.error('加载任务失败', e)
  }
}

// ==================== 开启爬虫 ====================
async function startCrawl() {
  if (!selectedSource.value) {
    ElMessage.warning('请先选择数据源')
    return
  }
  starting.value = true
  try {
    // 创建任务
    const taskRes = await spiderApi.createTask({
      taskName: `${getSourceName(selectedSource.value)} - ${new Date().toLocaleDateString()} 专业关键词爬取`,
      sourceCode: selectedSource.value,
      sourceName: getSourceName(selectedSource.value),
      depth: crawlDepth.value,
      selectedMajors: selectedMajors.value.join(','),
      dataTypes: 'job',
      status: 'pending'
    })

    const taskId = taskRes?.id || taskRes?.data?.id
    if (!taskId) {
      ElMessage.error('任务创建失败')
      return
    }

    // 启动爬虫
    await spiderApi.runTask(taskId)
    ElMessage.success('爬虫已启动，正在按专业关键词爬取数据...')
    taskStats.running++

    // 开始轮询进度
    startProgressPolling(taskId)
  } catch (e) {
    console.error('启动失败', e)
    ElMessage.error('启动失败: ' + (e.message || e))
  } finally {
    starting.value = false
  }
}

// ==================== 安全停止爬虫 ====================
async function safeStopCrawl() {
  const runningTasks = []
  try {
    const res = await spiderApi.getTasks({ page: 1, pageSize: 100 })
    const list = res?.list || res?.data?.list || []
    list.filter(t => t.status === 'running').forEach(t => runningTasks.push(t.id))
  } catch {}

  if (runningTasks.length === 0) {
    ElMessage.warning('当前没有运行中的任务')
    return
  }

  stopping.value = true
  ElMessage.info('正在安全停止，请等待当前页爬取完成...')

  try {
    for (const taskId of runningTasks) {
      await spiderApi.stopTask(taskId)
    }
    ElMessage.success('爬虫已安全停止，当前页数据已完整保存')
    stopProgressPolling()
    progressData.value = {}
    loadTasks()
  } catch (e) {
    ElMessage.error('停止失败: ' + (e.message || e))
  } finally {
    stopping.value = false
  }
}

// ==================== 进度轮询 ====================
function startProgressPolling(taskId) {
  stopProgressPolling()
  pollProgress(taskId)
  progressPollingTimer.value = setInterval(() => pollProgress(taskId), 3000)
}

function stopProgressPolling() {
  if (progressPollingTimer.value) {
    clearInterval(progressPollingTimer.value)
    progressPollingTimer.value = null
  }
}

async function pollProgress(taskId) {
  if (!taskId) return
  try {
    const res = await spiderApi.getProgress(taskId)
    progressData.value = res || {}
    if (res?.status === 'running') {
      const major = res.currentMajor || '未知'
      const keyword = res.currentKeyword || '未知'
      const page = res.currentPage || 0
      const majorIdx = res.majorIndex || 0
      const totalMajors = res.totalMajors || 0
      currentPhase.value = `正在爬取专业: ${major} (${majorIdx}/${totalMajors}) | 关键词: ${keyword} | 第${page}页`
    } else if (res?.status === 'success' || !res?.isRunning) {
      currentPhase.value = '爬取已完成'
      stopProgressPolling()
      loadTasks()
      loadCollectedData()
    }
  } catch {
    // 静默处理
  }
}

// ==================== ETL数据清洗 ====================
async function executeEtl() {
  etlLoading.value = true
  try {
    const res = await spiderApi.executeEtl()
    etlResult.value = res || {}
    const cleaned = res?.cleanedCount || 0
    const polluted = res?.pollutedCount || 0
    const duplicate = res?.duplicateCount || 0
    ElMessage.success(`ETL完成：有效 ${cleaned} 条，重复 ${duplicate} 条，脏数据 ${polluted} 条`)
    loadPoolStats()
  } catch (e) {
    ElMessage.error('ETL执行失败: ' + (e.message || e))
    etlResult.value = null
  } finally {
    etlLoading.value = false
  }
}

// ==================== 训练池 ====================
async function loadPoolStats() {
  try {
    const res = await spiderApi.getTrainingPool()
    poolStats.value = res || {}
  } catch {
    poolStats.value = {}
  }
}

async function syncAllToPool() {
  syncing.value = true
  try {
    const res = await spiderApi.syncAllData()
    ElMessage.success(res?.message || '同步成功')
    loadPoolStats()
    loadCollectedData()
  } catch (e) {
    ElMessage.error('同步失败: ' + (e.message || e))
  } finally {
    syncing.value = false
  }
}

// ==================== 全国统计数据 ====================
async function loadNationalStats() {
  statsLoading.value = true
  try {
    const res = await spiderApi.getNationalStats()
    nationalStats.value = res || {}
    await nextTick()
    renderNationalCharts()
  } catch {
    nationalStats.value = {}
  } finally {
    statsLoading.value = false
  }
}

function renderNationalCharts() {
  if (!positionChartRef.value || !industryChartRef.value) return

  if (positionChart) positionChart.dispose()
  if (industryChart) industryChart.dispose()
  if (eduChart) eduChart.dispose()
  if (trendChart) trendChart.dispose()

  // 城市分布柱状图
  const cityData = (nationalStats.value.cityStats || []).slice(0, 12)
  positionChart = echarts.init(positionChartRef.value)
  positionChart.setOption({
    title: { text: '城市分布 TOP12', textStyle: { fontSize: 13, fontWeight: 600 }, left: 'center' },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '25%', containLabel: true },
    xAxis: { type: 'category', data: cityData.map(d => d.city).reverse(), axisLabel: { fontSize: 10, rotate: 30 } },
    yAxis: { type: 'value', name: '职位数', axisLabel: { fontSize: 10 } },
    color: ['#5470c6'],
    series: [{
      type: 'bar',
      data: cityData.map(d => d.count).reverse(),
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', fontSize: 9 }
    }]
  })

  // 行业分布饼图
  const indData = nationalStats.value.industryStats || []
  industryChart = echarts.init(industryChartRef.value)
  industryChart.setOption({
    title: { text: '行业分布 TOP10', textStyle: { fontSize: 13, fontWeight: 600 }, left: 'center' },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 10 } },
    color: ['#67c23a', '#409eff', '#e6a23c', '#f56c6c', '#909399', '#1f7ae0', '#00b4b5', '#5ae624', '#ff6a00', '#9c27b0'],
    series: [{
      type: 'pie',
      radius: ['30%', '60%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
      data: indData.map(d => ({ name: d.industry, value: d.count }))
    }]
  })

  // 学历要求分布
  if (eduChartRef.value) {
    const eduData = nationalStats.value.educationStats || []
    eduChart = echarts.init(eduChartRef.value)
    eduChart.setOption({
      title: { text: '学历要求分布', textStyle: { fontSize: 13, fontWeight: 600 }, left: 'center' },
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de'],
      series: [{
        type: 'pie',
        radius: ['30%', '60%'],
        center: ['50%', '45%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: eduData.map(d => ({ name: d.education, value: d.count }))
      }]
    })
  }

  // 每日采集趋势
  if (trendChartRef.value) {
    const trendData = nationalStats.value.dailyTrend || []
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      title: { text: '近7天采集趋势', textStyle: { fontSize: 13, fontWeight: 600 }, left: 'center' },
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '25%', containLabel: true },
      xAxis: { type: 'category', data: trendData.map(d => d.date).reverse(), axisLabel: { fontSize: 10, rotate: 30 } },
      yAxis: { type: 'value', name: '有效数据', axisLabel: { fontSize: 10 } },
      color: ['#67c23a'],
      series: [{
        type: 'line',
        data: trendData.map(d => d.count).reverse(),
        smooth: true,
        areaStyle: { color: 'rgba(103, 194, 58, 0.2)' },
        itemStyle: { color: '#67c23a' },
        label: { show: true, position: 'top', fontSize: 9 }
      }]
    })
  }
}

// ==================== 采集数据 ====================
let loadDataTimer = null
async function loadCollectedData() {
  loadingData.value = true
  selectedDataIds.value = []
  try {
    const params = {
      page: dataPage.value,
      pageSize: dataPageSize.value
    }
    if (filterSourceCode.value) params.sourceCode = filterSourceCode.value
    if (filterMajor.value) params.majorName = filterMajor.value
    const res = await spiderApi.getCollectedData(params)
    collectedDataList.value = res?.list || res?.data?.list || []
    dataTotal.value = res?.total || res?.data?.total || 0
  } catch {
    collectedDataList.value = []
  } finally {
    loadingData.value = false
  }
}

function onDataSelectionChange(rows) {
  selectedDataIds.value = rows.map(r => r.id)
}

async function batchDeleteData() {
  if (selectedDataIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedDataIds.value.length} 条数据？`, '批量删除', { type: 'warning' })
    await spiderApi.deleteData(selectedDataIds.value)
    ElMessage.success('删除成功')
    selectedDataIds.value = []
    loadCollectedData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 监听分页参数变化，自动刷新数据
watch([dataPage, dataPageSize], () => {
  if (loadDataTimer) clearTimeout(loadDataTimer)
  loadDataTimer = setTimeout(() => loadCollectedData(), 150)
})

async function clearAllData() {
  if (dataTotal.value === 0) {
    ElMessage.warning('当前没有可清空的数据')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要清空所有已采集数据吗？此操作将删除 ${dataTotal.value} 条数据，且不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    clearingData.value = true
    const res = await spiderApi.clearAllData()
    ElMessage.success(res?.message || '数据已清空')
    collectedDataList.value = []
    dataTotal.value = 0
    loadPoolStats()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('清空失败: ' + (e?.message || e))
    }
  } finally {
    clearingData.value = false
  }
}

function viewDataDetail(row) {
  currentData.value = row
  showDataDetailDialog.value = true
}

function getSourceName(code) {
  return dataSources.value.find(s => s.code === code)?.name || code
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await Promise.all([
    loadMajorKeywords(),
    loadTasks(),
    loadPoolStats(),
    loadNationalStats(),
    loadCollectedData()
  ])
})

onUnmounted(() => {
  stopProgressPolling()
  if (positionChart) positionChart.dispose()
  if (industryChart) industryChart.dispose()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; }
.card-header-right { display: flex; align-items: center; }

.config-card { margin-bottom: 16px; }
.major-title { display: flex; align-items: center; gap: 10px; }
.major-name { font-weight: 600; color: #303133; }
.keyword-tags { display: flex; flex-wrap: wrap; }

.control-card { margin-bottom: 16px; }

.progress-section { margin-bottom: 16px; padding: 16px; background: #f5f7fa; border-radius: 8px; }
.progress-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.progress-header-right { display: flex; align-items: center; gap: 12px; }
.progress-label { font-weight: 600; color: #303133; display: flex; align-items: center; gap: 6px; }
.progress-percent { font-size: 18px; font-weight: 700; color: #409eff; }
.progress-text { font-size: 12px; color: #909399; }
.progress-info { display: flex; gap: 20px; margin-top: 8px; font-size: 12px; color: #909399; }

/* 详细进度信息 */
.progress-detail {
  margin-top: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 8px;
  border: 1px solid #667eea30;
}
.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  color: #606266;
}
.detail-item:not(:last-child) {
  border-bottom: 1px dashed #667eea20;
  padding-bottom: 8px;
  margin-bottom: 4px;
}
.detail-item .el-icon {
  color: #667eea;
  font-size: 16px;
}
.detail-label {
  color: #909399;
  min-width: 80px;
}
.detail-value {
  color: #303133;
  font-weight: 600;
}
.detail-index {
  color: #909399;
  font-size: 12px;
}

.stop-hint { display: flex; align-items: center; gap: 8px; padding: 10px 12px; background: #fef0f0; border-radius: 6px; color: #f56c6c; font-size: 13px; margin-bottom: 16px; }

.task-stats { display: flex; gap: 32px; padding-top: 12px; border-top: 1px solid #f0f0f0; }
.stat-item { text-align: center; }
.stat-value { display: block; font-size: 24px; font-weight: 700; color: #303133; }
.stat-label { font-size: 12px; color: #909399; }

.stats-row { margin-bottom: 16px; }

.etl-desc { font-size: 13px; color: #606266; }
.etl-desc ul { padding-left: 20px; margin: 8px 0; }
.etl-desc li { margin-bottom: 4px; }
.etl-result { margin-top: 12px; }
.etl-stats { display: flex; gap: 20px; font-size: 13px; }

.pool-stats { display: flex; gap: 16px; }
.pool-stat-item { display: flex; align-items: center; gap: 10px; flex: 1; }
.pool-stat-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; }
.pool-stat-info { flex: 1; }
.pool-stat-label { font-size: 12px; color: #909399; margin-bottom: 2px; }
.pool-stat-value { font-size: 18px; font-weight: 700; color: #303133; }
.pool-dist { margin-top: 16px; }
.dist-title { font-size: 12px; color: #909399; margin-bottom: 8px; }
.dist-item { display: flex; align-items: center; margin-bottom: 6px; }
.dist-label { width: 120px; font-size: 12px; color: #606266; }
.dist-value { font-size: 12px; color: #303133; font-weight: 600; min-width: 40px; text-align: right; }

.national-card { margin-bottom: 16px; }
.stat-mini-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px 12px;
  text-align: center;
}
.stat-mini-value { font-size: 24px; font-weight: 700; }
.stat-mini-label { font-size: 12px; color: #909399; margin-top: 4px; }
.position-row { margin-top: 12px; }
.position-chart { height: 280px; width: 100%; }

.dialog-progress { margin-bottom: 12px; }
.progress-phase { margin-top: 12px; padding: 12px; background: #ecf5ff; border-radius: 6px; }
.phase-title { font-size: 13px; color: #409eff; font-weight: 600; margin-bottom: 4px; }
.phase-content { font-size: 13px; color: #606266; }
.crawl-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #e6f7ff;
  border-radius: 6px;
  color: #1890ff;
  font-size: 13px;
  margin-top: 12px;
}
</style>
