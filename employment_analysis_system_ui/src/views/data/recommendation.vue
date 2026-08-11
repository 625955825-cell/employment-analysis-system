<template>
  <div class="page-container">
    <h2>智能推荐算法管理</h2>

    <el-tabs v-model="activeTab" class="algo-tabs">

      <!-- ===== Tab1: 推荐管理 ===== -->
      <el-tab-pane label="推荐管理" name="manage">

        <!-- 统计卡片 -->
        <el-row :gutter="20" class="stats-row">
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background:#409eff;"><el-icon><Collection /></el-icon></div>
                <div class="stat-info">
                  <p class="stat-label">爬虫数据总量</p>
                  <p class="stat-value">{{ overview.totalSpiderData || 0 }}</p>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background:#67c23a;"><el-icon><User /></el-icon></div>
                <div class="stat-info">
                  <p class="stat-label">学生总数</p>
                  <p class="stat-value">{{ overview.totalStudents || 0 }}</p>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background:#e6a23c;"><el-icon><Cpu /></el-icon></div>
                <div class="stat-info">
                  <p class="stat-label">算法开启专业</p>
                  <p class="stat-value">{{ enabledMajorCount }} / {{ overview.majors?.length || 0 }}</p>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background:#f56c6c;"><el-icon><Finished /></el-icon></div>
                <div class="stat-info">
                  <p class="stat-label">推荐结果总数</p>
                  <p class="stat-value">{{ recommendStats.totalRecommendations || 0 }}</p>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 反馈统计 -->
        <el-row :gutter="20" class="stats-row">
          <el-col :span="8">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background:#009688;"><el-icon><View /></el-icon></div>
                <div class="stat-info">
                  <p class="stat-label">学生已查看</p>
                  <p class="stat-value">{{ recommendStats.viewedCount || 0 }}</p>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background:#67c23a;"><el-icon><CircleCheck /></el-icon></div>
                <div class="stat-info">
                  <p class="stat-label">正面反馈</p>
                  <p class="stat-value">{{ recommendStats.positiveFeedback || 0 }}</p>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background:#909399;"><el-icon><CloseBold /></el-icon></div>
                <div class="stat-info">
                  <p class="stat-label">负面反馈</p>
                  <p class="stat-value">{{ recommendStats.negativeFeedback || 0 }}</p>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 模型评估入口 -->
        <div style="margin: 16px 0; display: flex; justify-content: flex-end; gap: 8px;">
          <el-button type="success" @click="enableAllTrainedModels" :loading="enableAllLoading">
            <el-icon><Check /></el-icon> 一键启动全部已训练模型
          </el-button>
          <el-button type="danger" @click="openBatchTrainDialog" :loading="batchTrainLoading">
            <el-icon><MagicStick /></el-icon> 一键批量训练全部
          </el-button>
          <el-button type="warning" @click="openEvaluateDialog" :loading="evaluateLoading">
            <el-icon><Cpu /></el-icon> 模型评估
          </el-button>
        </div>

        <!-- 专业推荐管理表格 -->
        <el-card style="margin-top: 16px;">
          <template #header>
            <div class="card-header">
              <span>专业推荐管理</span>
              <el-button type="primary" size="small" @click="loadOverview" :loading="loading">
                <el-icon><Refresh /></el-icon> 刷新数据
              </el-button>
            </div>
          </template>

          <el-table :data="majors" stripe v-loading="loading">
            <el-table-column prop="majorName" label="专业名称" min-width="160">
              <template #default="{ row }">
                <span style="font-weight: 600;">{{ row.majorName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="shortName" label="简称" width="100" align="center" />
            <el-table-column prop="studentCount" label="学生数" width="80" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.studentCount || 0 }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="spiderDataCount" label="爬虫数据" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.spiderDataCount > 0 ? 'success' : 'info'" size="small">{{ row.spiderDataCount || 0 }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="modelTrained" label="模型状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="row.modelTrained === 'trained' ? 'success' : 'warning'" size="small">
                  {{ row.modelTrained === 'trained' ? '已训练' : '未训练' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="recommendEnabled" label="算法状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.recommendEnabled === '1' ? 'success' : 'danger'" size="small">
                  {{ row.recommendEnabled === '1' ? '已开启' : '已关闭' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="320" align="center" fixed="right">
              <template #default="{ row }">
                <div style="display: flex; gap: 4px; flex-wrap: wrap; justify-content: center;">
                  <!-- 训练推荐按钮：爬虫数据>0且有学生才可训练 -->
                  <el-button
                    type="primary"
                    size="small"
                    @click="openTrainDialog(row)"
                    :disabled="row.modelTrained === 'trained' || row.studentCount === 0 || row.spiderDataCount === 0"
                    :loading="trainingMajorId === row.id"
                  >
                    {{ row.modelTrained === 'trained' ? '已训练' : '训练模型' }}
                  </el-button>
                  <!-- 开启/关闭算法：必须训练完成后才可操作 -->
                  <el-button
                    size="small"
                    :type="row.recommendEnabled === '1' ? 'warning' : 'success'"
                    @click="toggleRecommend(row)"
                    :disabled="row.modelTrained !== 'trained' || row.spiderDataCount === 0"
                  >
                    {{ row.recommendEnabled === '1' ? '关闭算法' : '开启算法' }}
                  </el-button>
                  <el-button
                    type="info"
                    size="small"
                    @click="viewMajorDetail(row)"
                  >
                    查看详情
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 专业详情弹窗 -->
        <el-dialog v-model="detailDialogVisible" title="专业推荐详情" width="800px" destroy-on-close>
          <div v-if="detailData.major">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="专业名称">{{ detailData.major.majorName }}</el-descriptions-item>
              <el-descriptions-item label="专业简称">{{ detailData.major.shortName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="算法状态">
                <el-tag :type="detailData.major.recommendEnabled === '1' ? 'success' : 'danger'" size="small">
                  {{ detailData.major.recommendEnabled === '1' ? '已开启' : '已关闭' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="学生数量">{{ detailData.studentCount }}</el-descriptions-item>
              <el-descriptions-item label="爬虫数据">{{ detailData.spiderDataCount }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div style="margin-top: 16px;">
            <h4 style="margin-bottom: 8px; color: #303133;">最近推荐记录</h4>
            <el-table :data="detailData.recentRecommendations" stripe size="small" max-height="300">
              <el-table-column prop="userId" label="用户ID" width="80" />
              <el-table-column prop="targetName" label="推荐职位" min-width="150" />
              <el-table-column prop="matchScore" label="匹配分" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getScoreTagType(row.matchScore)" size="small">{{ row.matchScore }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="city" label="城市" width="90" align="center" />
              <el-table-column prop="feedback" label="反馈" width="80" align="center">
                <template #default="{ row }">
                  {{ row.feedback === 'positive' ? '👍 感兴趣' : row.feedback === 'negative' ? '👎 不合适' : '未反馈' }}
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="推荐时间" width="170">
                <template #default="{ row }">
                  {{ row.createTime ? row.createTime.replace('T', ' ').slice(0, 19) : '—' }}
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!detailData.recentRecommendations || detailData.recentRecommendations.length === 0" style="text-align: center; color: #999; padding: 20px;">
              暂无推荐记录
            </div>
          </div>
        </el-dialog>

        <!-- 训练推荐模型弹窗 -->
        <el-dialog v-model="trainDialogVisible" title="训练推荐模型" width="520px" destroy-on-close>
          <div v-if="trainLoading" style="text-align: center; padding: 40px;">
            <el-icon class="is-loading" :size="48"><Loading /></el-icon>
            <p style="color: #409eff; margin-top: 16px; font-size: 15px;">模型训练中，请稍候...</p>
            <p style="color: #909399; font-size: 13px; margin-top: 8px;">这可能需要数秒至数十秒</p>
          </div>
          <div v-else>
            <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
              <template #title>
                即将为专业 <strong>{{ trainTargetMajor?.majorName }}</strong> 训练推荐模型
              </template>
            </el-alert>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="专业名称">{{ trainTargetMajor?.majorName }}</el-descriptions-item>
              <el-descriptions-item label="学生数量">{{ trainTargetMajor?.studentCount }} 人</el-descriptions-item>
              <el-descriptions-item label="爬虫数据">{{ trainTargetMajor?.spiderDataCount }} 条</el-descriptions-item>
              <el-descriptions-item label="当前状态">
                <el-tag :type="trainTargetMajor?.modelTrained === 'trained' ? 'success' : 'warning'" size="small">
                  {{ trainTargetMajor?.modelTrained === 'trained' ? '已训练' : '未训练' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
            <div style="margin-top: 16px;">
              <div style="margin-bottom: 8px; font-weight: 600; color: #303133;">选择训练模型</div>
              <el-radio-group v-model="trainAlgorithmType">
                <el-radio label="tfidf">TF-IDF 文本相似度</el-radio>
                <el-radio label="multi_factor">规则权重打分</el-radio>
              </el-radio-group>
              <div style="margin-top: 8px; color: #909399; font-size: 12px; line-height: 1.6;">
                选择 TF-IDF 时将尝试调用 Python 服务训练；若服务不可用，会在本页面提示当前实际使用规则权重打分。
              </div>
            </div>
            <el-alert type="warning" :closable="false" style="margin-top: 16px;">
              <template #title>
                训练完成后，需手动「开启算法」后学生端才能使用推荐功能
              </template>
            </el-alert>
          </div>
          <template #footer>
            <el-button @click="trainDialogVisible = false" :disabled="trainLoading">取消</el-button>
            <el-button type="primary" @click="doTrainModel" :loading="trainLoading">
              开始训练
            </el-button>
          </template>
        </el-dialog>

        <!-- 批量训练全部专业弹窗 -->
        <el-dialog v-model="batchTrainDialogVisible" title="一键批量训练全部专业" width="600px" destroy-on-close>
          <div v-if="batchTrainLoading" style="text-align: center; padding: 40px;">
            <el-icon class="is-loading" :size="48"><Loading /></el-icon>
            <p style="color: #409eff; margin-top: 16px; font-size: 15px;">正在批量训练，请稍候...</p>
            <p style="color: #909399; font-size: 13px; margin-top: 8px;">将遍历所有有爬虫数据和学生的专业</p>
          </div>
          <div v-else-if="batchTrainResult">
            <el-alert :type="batchTrainResult.trainedCount > 0 ? 'success' : 'info'" :closable="false" style="margin-bottom: 16px;">
              <template #title>{{ batchTrainResult.message }}</template>
            </el-alert>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="总专业数">{{ batchTrainResult.totalMajors }}</el-descriptions-item>
              <el-descriptions-item label="已训练">{{ batchTrainResult.trainedCount }} 个专业</el-descriptions-item>
              <el-descriptions-item label="已跳过">{{ batchTrainResult.skippedCount }} 个</el-descriptions-item>
              <el-descriptions-item label="耗时">{{ batchTrainResult.costTime || '—' }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="batchTrainResult.successList?.length > 0" style="margin-top: 16px;">
              <p style="font-weight: 600; margin-bottom: 8px;">训练成功的专业：</p>
              <div
                v-for="item in batchTrainResult.successList"
                :key="item.majorId"
                style="margin-bottom: 10px; padding: 10px 12px; border: 1px solid #ebeef5; border-radius: 8px; background: #fafafa;"
              >
                <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                  <el-tag :type="item.fallbackUsed ? 'warning' : 'success'" size="small">
                    {{ item.majorName }}
                  </el-tag>
                  <el-tag size="small" type="info">{{ item.spiderDataCount }}条数据</el-tag>
                  <el-tag size="small" :type="item.actualAlgorithm === 'tfidf' ? 'success' : 'warning'">
                    实际算法：{{ item.actualAlgorithm === 'tfidf' ? 'TF-IDF' : '规则权重打分' }}
                  </el-tag>
                </div>
                <div v-if="item.message" style="margin-top: 6px; color: #606266; font-size: 13px; line-height: 1.6;">
                  {{ item.message }}
                </div>
                <div v-if="item.failureReason" style="margin-top: 6px; color: #f56c6c; font-size: 12px; line-height: 1.6; word-break: break-all;">
                  失败原因：{{ item.failureReason }}
                </div>
              </div>
            </div>
            <div v-if="batchTrainResult.skipList?.length > 0" style="margin-top: 12px;">
              <p style="font-weight: 600; margin-bottom: 8px; color: #909399;">跳过的专业：</p>
              <el-tag v-for="item in batchTrainResult.skipList" :key="item.majorId" type="info" size="small" style="margin: 4px;">
                {{ item.majorName }} ({{ item.reason }})
              </el-tag>
            </div>
          </div>
          <div v-else>
            <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
              <template #title>即将对所有有爬虫数据且有学生的专业进行批量训练</template>
            </el-alert>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="总专业数">{{ trainingStats.totalMajors || 0 }}</el-descriptions-item>
              <el-descriptions-item label="有爬虫数据">{{ trainingStats.hasSpiderDataCount || 0 }} 个</el-descriptions-item>
              <el-descriptions-item label="有学生数据">{{ trainingStats.hasStudentsCount || 0 }} 个</el-descriptions-item>
              <el-descriptions-item label="有效爬虫总数">{{ trainingStats.validSpiderDataTotal || 0 }} 条</el-descriptions-item>
            </el-descriptions>
            <div style="margin-top: 16px;">
              <div style="margin-bottom: 8px; font-weight: 600; color: #303133;">选择训练模型</div>
              <el-radio-group v-model="batchTrainAlgorithmType">
                <el-radio label="tfidf">TF-IDF 文本相似度</el-radio>
                <el-radio label="multi_factor">规则权重打分</el-radio>
              </el-radio-group>
              <div style="margin-top: 8px; color: #909399; font-size: 12px; line-height: 1.6;">
                批量训练时若 TF-IDF 不可用，会按专业返回回退提示，数据分析员端可直接看到当前实际使用的算法。
              </div>
            </div>
            <el-alert type="warning" :closable="false" style="margin-top: 16px;">
              <template #title>训练完成后，需手动「开启算法」后学生端才能使用推荐功能</template>
            </el-alert>
          </div>
          <template #footer>
            <el-button @click="batchTrainDialogVisible = false" :disabled="batchTrainLoading">关闭</el-button>
            <el-button v-if="!batchTrainResult" type="danger" @click="doBatchTrain" :loading="batchTrainLoading">
              开始批量训练
            </el-button>
            <el-button v-else type="primary" @click="batchTrainDialogVisible = false; batchTrainResult = null">
              确定
            </el-button>
          </template>
        </el-dialog>

        <!-- 模型评估弹窗 -->
        <el-dialog v-model="evaluateDialogVisible" title="推荐效果双打分评估" width="560px" destroy-on-close>
          <div v-if="evaluateLoading" style="text-align: center; padding: 40px;">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            <p style="color: #999; margin-top: 12px;">评估中，请稍候...</p>
          </div>
          <div v-else>
            <el-alert
              :type="evaluateResult.feedbackSampleCount > 0 ? 'success' : 'warning'"
              :closable="false"
              style="margin-bottom: 20px;"
            >
              <template #title>
                {{ evaluateResult.feedbackSampleCount > 0
                  ? '评估完成，以下为离线评分与实时反馈评分的综合结果'
                  : '当前暂无真实反馈样本，系统先展示离线评分结果' }}
              </template>
            </el-alert>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="评估范围">{{ evaluateResult.majorName || '全部专业' }}</el-descriptions-item>
              <el-descriptions-item label="实际算法">{{ evaluateResult.actualAlgorithm === 'tfidf' ? 'TF-IDF 文本相似度' : '规则权重打分' }}</el-descriptions-item>
              <el-descriptions-item label="推荐记录数">{{ evaluateResult.totalRecommendations || 0 }}</el-descriptions-item>
              <el-descriptions-item label="已查看数">{{ evaluateResult.viewedCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="反馈样本数">{{ evaluateResult.feedbackSampleCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="评分公式">{{ evaluateResult.scoreFormula || '0.7 × 离线评分 + 0.3 × 实时反馈评分' }}</el-descriptions-item>
            </el-descriptions>
            <div style="margin-top: 20px;">
              <h4 style="margin: 0 0 12px; font-size: 14px; color: #303133;">双打分结果</h4>
              <el-row :gutter="12">
                <el-col :span="8">
                  <div class="eval-metric">
                    <span class="eval-label">离线评分</span>
                    <span class="eval-value">{{ formatPercent(evaluateResult.offlineScore) }}</span>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="eval-metric">
                    <span class="eval-label">实时反馈评分</span>
                    <span class="eval-value">{{ formatPercent(evaluateResult.realtimeScore) }}</span>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="eval-metric">
                    <span class="eval-label">综合评分</span>
                    <span class="eval-value">{{ formatPercent(evaluateResult.finalScore) }}</span>
                  </div>
                </el-col>
              </el-row>
            </div>
            <div style="margin-top: 20px;">
              <h4 style="margin: 0 0 12px; font-size: 14px; color: #303133;">实时反馈统计</h4>
              <el-row :gutter="12">
                <el-col :span="12">
                  <div class="eval-metric">
                    <span class="eval-label">正反馈数</span>
                    <span class="eval-value">{{ evaluateResult.positiveCount || 0 }}</span>
                  </div>
                </el-col>
                <el-col :span="12">
                  <div class="eval-metric">
                    <span class="eval-label">负反馈数</span>
                    <span class="eval-value">{{ evaluateResult.negativeCount || 0 }}</span>
                  </div>
                </el-col>
              </el-row>
            </div>
            <div v-if="evaluateResult.note" style="margin-top: 16px;">
              <el-alert type="info" :closable="false" show-icon>
                <template #title>{{ evaluateResult.note }}</template>
              </el-alert>
            </div>
          </div>
          <template #footer>
            <el-button @click="evaluateDialogVisible = false">关闭</el-button>
          </template>
        </el-dialog>

      </el-tab-pane>

      <!-- ===== Tab2: 权重配置 ===== -->
      <el-tab-pane label="权重配置" name="weight">
        <el-card style="margin-top: 16px;">
          <template #header>
            <div class="card-header">
              <span>推荐算法评分权重配置</span>
              <div style="display: flex; gap: 8px; align-items: center;">
                <el-button type="warning" size="small" @click="resetWeights" :loading="weightLoading">
                  <el-icon><RefreshRight /></el-icon> 恢复默认
                </el-button>
                <el-button type="primary" size="small" @click="saveWeights" :disabled="weightTotal !== 100" :loading="weightLoading">
                  <el-icon><Check /></el-icon> 保存配置
                </el-button>
              </div>
            </div>
          </template>

          <el-alert
            v-if="weightTotal !== 100"
            type="error"
            :closable="false"
            style="margin-bottom: 16px;"
          >
            <template #title>权重总分必须等于 100 分，当前为 <strong>{{ weightTotal }}</strong> 分，请调整滑块</template>
          </el-alert>
          <el-alert
            v-else
            type="success"
            :closable="false"
            style="margin-bottom: 16px;"
          >
            <template #title>权重配置正确，总分刚好 100 分，可点击「保存配置」生效</template>
          </el-alert>

          <div class="weight-list">
            <div v-for="item in weightList" :key="item.weightKey" class="weight-item">
              <div class="weight-item-header">
                <span class="weight-name">{{ item.weightName }}</span>
                <span class="weight-value-display">{{ item.weightValue }} 分</span>
              </div>
              <div class="weight-slider-row">
                <el-slider
                  v-model="item.weightValue"
                  :min="0"
                  :max="50"
                  :step="1"
                  :show-tooltip="true"
                  :format-tooltip="val => val + ' 分'"
                />
              </div>
              <div class="weight-desc">{{ item.description }}</div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

    </el-tabs>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshRight, Check, Loading, Collection, User, Cpu, Finished, View, CircleCheck, CloseBold, MagicStick } from '@element-plus/icons-vue'
import { recommendApi } from '@/api'

const activeTab = ref('manage')
const loading = ref(false)
const weightLoading = ref(false)
const evaluateLoading = ref(false)
const evaluateDialogVisible = ref(false)
const evaluateResult = ref({})
const overview = ref({})
const recommendStats = ref({})
const detailDialogVisible = ref(false)
const trainDialogVisible = ref(false)
const trainTargetMajor = ref(null)
const trainAlgorithmType = ref('tfidf')
const batchTrainAlgorithmType = ref('tfidf')
const trainLoading = ref(false)
const trainingMajorId = ref(null)
const detailData = ref({})
const weightList = ref([])
const batchTrainDialogVisible = ref(false)
const batchTrainLoading = ref(false)
const batchTrainResult = ref(null)
const enableAllLoading = ref(false)
const trainingStats = ref({})

const majors = computed(() => overview.value.majors || [])
const enabledMajorCount = computed(() =>
  (majors.value.filter(m => m.recommendEnabled === '1')).length
)
const weightTotal = computed(() =>
  weightList.value.reduce((sum, item) => sum + item.weightValue, 0)
)

function getScoreTagType(score) {
  if (!score) return 'info'
  if (score >= 70) return 'success'
  if (score >= 50) return 'warning'
  return 'info'
}

function formatPercent(value) {
  const num = Number(value || 0)
  return `${(num * 100).toFixed(1)}%`
}

function loadOverview() {
  loading.value = true
  Promise.all([
    recommendApi.getMajorOverview(),
    recommendApi.getRecommendStats()
  ]).then(([overviewData, statsData]) => {
    overview.value = overviewData || {}
    recommendStats.value = statsData || {}
  }).catch(() => {
    ElMessage.error('加载数据失败')
  }).finally(() => {
    loading.value = false
  })
}

function loadWeights() {
  weightLoading.value = true
  recommendApi.getWeightList().then(res => {
    weightList.value = res || []
  }).catch(() => {
    ElMessage.error('加载权重配置失败')
  }).finally(() => {
    weightLoading.value = false
  })
}

async function saveWeights() {
  if (weightTotal.value !== 100) {
    ElMessage.warning('权重总分必须等于 100 分')
    return
  }
  weightLoading.value = true
  try {
    await recommendApi.updateWeights(weightList.value)
    ElMessage.success('权重配置已保存')
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '保存失败')
  } finally {
    weightLoading.value = false
  }
}

async function resetWeights() {
  try {
    await ElMessageBox.confirm('确定恢复默认权重吗？当前所有调整将丢失。', '恢复确认', { type: 'warning' })
  } catch {
    return
  }
  weightLoading.value = true
  recommendApi.resetWeights().then(() => {
    ElMessage.success('已恢复默认权重')
    loadWeights()
  }).catch(err => {
    ElMessage.error(err?.response?.data?.message || '恢复失败')
  }).finally(() => {
    weightLoading.value = false
  })
}

async function toggleRecommend(row) {
  const action = row.recommendEnabled === '1' ? '关闭' : '开启'
  const confirmMsg = row.recommendEnabled === '1'
    ? `确定关闭专业「${row.majorName}」的推荐算法吗？关闭后学生端将无法使用该专业推荐功能。`
    : `确定开启专业「${row.majorName}」的推荐算法吗？开启后学生可以选择简历并开始使用推荐功能。`

  try {
    await ElMessageBox.confirm(confirmMsg, `${action}确认`, { type: 'warning' })
  } catch {
    return
  }

  const newValue = row.recommendEnabled === '1' ? '0' : '1'
  recommendApi.toggleRecommendEnabled(row.id, newValue).then(() => {
    ElMessage.success(`推荐算法已${action}`)
    loadOverview()
  }).catch(err => {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  })
}

function openTrainDialog(row) {
  if (row.modelTrained === 'trained') return
  if (row.spiderDataCount === 0) {
    ElMessage.warning('该专业暂无爬虫清洗数据，无法训练模型')
    return
  }
  if (row.studentCount === 0) {
    ElMessage.warning('该专业下暂无学生，无法训练模型')
    return
  }
  trainTargetMajor.value = row
  trainAlgorithmType.value = 'tfidf'
  trainLoading.value = false
  trainDialogVisible.value = true
}

async function doTrainModel() {
  if (!trainTargetMajor.value) return
  trainLoading.value = true
  trainingMajorId.value = trainTargetMajor.value.id
  try {
    const res = await recommendApi.trainRecommendModel(trainTargetMajor.value.id, trainAlgorithmType.value)
    const actualAlgorithmText = res?.actualAlgorithm === 'tfidf' ? 'TF-IDF 文本相似度' : '规则权重打分'
    if (res?.fallbackUsed) {
      ElMessage.warning((res?.message || `TF-IDF 当前不可用，现已回退为${actualAlgorithmText}`) + (res?.failureReason ? `；原因：${res.failureReason}` : ''))
    } else {
      ElMessage.success(res?.message || `模型训练完成，当前使用${actualAlgorithmText}`)
    }
    trainDialogVisible.value = false
    loadOverview()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '模型训练失败')
  } finally {
    trainLoading.value = false
    trainingMajorId.value = null
  }
}

function openBatchTrainDialog() {
  batchTrainDialogVisible.value = true
  batchTrainResult.value = null
  recommendApi.getModelTrainingStats().then(res => {
    trainingStats.value = res || {}
  }).catch(() => {
    trainingStats.value = {}
  })
}

async function doBatchTrain() {
  batchTrainLoading.value = true
  try {
    const res = await recommendApi.trainAllMajors(batchTrainAlgorithmType.value)
    batchTrainResult.value = res || {}
    if (res?.fallbackCount > 0) {
      ElMessage.warning(`批量训练完成，其中 ${res.fallbackCount} 个专业因 TF-IDF 不可用已回退为规则权重打分，可在结果列表查看具体失败原因`)
    }
    loadOverview()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '批量训练失败')
  } finally {
    batchTrainLoading.value = false
  }
}

async function enableAllTrainedModels() {
  try {
    await ElMessageBox.confirm('确定一键启动全部已训练模型吗？系统会开启所有训练完成但当前尚未开启的专业推荐功能。', '批量启动确认', { type: 'warning' })
  } catch {
    return
  }

  enableAllLoading.value = true
  try {
    const res = await recommendApi.enableAllTrainedMajors()
    if (res?.enabledCount > 0) {
      ElMessage.success(res?.message || `已开启 ${res.enabledCount} 个已训练专业`)
    } else {
      ElMessage.info(res?.message || '没有可开启的已训练专业')
    }
    loadOverview()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '批量启动失败')
  } finally {
    enableAllLoading.value = false
  }
}

function viewMajorDetail(row) {
  recommendApi.getMajorDetail(row.id).then(res => {
    detailData.value = res || {}
    detailDialogVisible.value = true
  }).catch(err => {
    ElMessage.error(err?.response?.data?.message || '加载详情失败')
  })
}

async function openEvaluateDialog() {
  evaluateDialogVisible.value = true
  evaluateLoading.value = true
  evaluateResult.value = {}
  try {
    const res = await recommendApi.evaluateModel()
    evaluateResult.value = res || {}
  } catch (err) {
    const message = err?.response?.data?.message || err?.message || ''
    ElMessage.error(message || '模型评估失败')
  } finally {
    evaluateLoading.value = false
  }
}

onMounted(() => {
  loadOverview()
  loadWeights()
})
</script>

<style scoped>
.page-container { padding: 20px; }
h2 { margin: 0 0 8px; font-size: 18px; font-weight: 600; color: #303133; }
.stats-row { margin-top: 16px; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; }
.eval-metric { display: flex; flex-direction: column; align-items: center; padding: 12px 8px; background: #f5f7fa; border-radius: 8px; gap: 4px; }
.eval-label { font-size: 12px; color: #909399; }
.eval-value { font-size: 18px; font-weight: 600; color: #409eff; }
.stat-info { flex: 1; }
.stat-label { font-size: 13px; color: #666; margin-bottom: 6px; }
.stat-value { font-size: 22px; font-weight: 600; color: #333; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.weight-list { display: flex; flex-direction: column; gap: 20px; }
.weight-item { background: #f5f7fa; border-radius: 8px; padding: 16px 20px; }
.weight-item-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.weight-name { font-size: 15px; font-weight: 600; color: #303133; }
.weight-value-display { font-size: 18px; font-weight: 700; color: #409eff; min-width: 50px; text-align: right; }
.weight-slider-row { padding: 0 4px; }
.weight-desc { font-size: 12px; color: #909399; margin-top: 6px; line-height: 1.5; }
</style>
