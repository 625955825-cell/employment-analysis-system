<template>
  <div class="page-wrap">
    <!-- 顶部标题区 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">就业率分析</h2>
        <p class="page-subtitle">就业分析与预警工作台 — 发现问题、定位责任、推动处理</p>
      </div>
      <div class="header-actions">
        <el-select v-model="selectedYear" placeholder="选择届" clearable style="width: 140px;" @change="loadAll">
          <el-option v-for="y in years" :key="y" :label="y + '届'" :value="y" />
        </el-select>
        <el-button @click="loadAll" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
        <el-button type="primary" @click="generateReport">
          <el-icon><Document /></el-icon> 导出分析报告
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>正在加载分析数据...</span>
    </div>

    <div v-else class="page-body">

      <!-- ==================== 核心指标卡片 ==================== -->
      <div class="metric-grid">
        <div class="metric-card" :class="overallRate >= 90 ? 'metric-card--green' : overallRate >= 75 ? 'metric-card--orange' : 'metric-card--red'">
          <div class="metric-card__icon"><el-icon><TrendCharts /></el-icon></div>
          <div class="metric-card__body">
            <p class="metric-card__label">总体就业率</p>
            <p class="metric-card__value">{{ overallRate.toFixed(1) }}<span class="metric-card__unit">%</span></p>
            <p class="metric-card__sub">
              <span v-if="overallRate >= 90" style="color:#22c55e">高于目标，就业形势良好</span>
              <span v-else-if="overallRate >= 75" style="color:#f59e0b">处于中等水平，需持续推进</span>
              <span v-else style="color:#ef4444">低于警戒线，需重点干预</span>
            </p>
          </div>
        </div>
        <div class="metric-card" :class="unemployedCount > 0 ? 'metric-card--orange' : 'metric-card--green'">
          <div class="metric-card__icon"><el-icon><Warning /></el-icon></div>
          <div class="metric-card__body">
            <p class="metric-card__label">未就业人数</p>
            <p class="metric-card__value">{{ unemployedCount }}</p>
            <p class="metric-card__sub">{{ unemployedCount > 0 ? '需要持续跟进帮扶' : '已全部就业' }}</p>
          </div>
        </div>
        <div class="metric-card" :class="belowAvgDeptCount > 0 ? 'metric-card--orange' : 'metric-card--green'">
          <div class="metric-card__icon"><el-icon><OfficeBuilding /></el-icon></div>
          <div class="metric-card__body">
            <p class="metric-card__label">低于全校平均院系</p>
            <p class="metric-card__value">{{ belowAvgDeptCount }} <span class="metric-card__unit">个</span></p>
            <p class="metric-card__sub">{{ belowAvgDeptCount > 0 ? '建议重点关注就业推进' : '各院系就业率均达标' }}</p>
          </div>
        </div>
        <div class="metric-card" :class="criticalClassCount > 0 ? 'metric-card--red' : 'metric-card--green'">
          <div class="metric-card__icon"><el-icon><School /></el-icon></div>
          <div class="metric-card__body">
            <p class="metric-card__label">重点帮扶班级</p>
            <p class="metric-card__value">{{ criticalClassCount }} <span class="metric-card__unit">个</span></p>
            <p class="metric-card__sub">{{ criticalClassCount > 0 ? '就业率低于全校 5% 以上' : '暂无重点帮扶班级' }}</p>
          </div>
        </div>
        <div class="metric-card" :class="highRiskCount > 0 ? 'metric-card--red' : 'metric-card--green'">
          <div class="metric-card__icon"><el-icon><User /></el-icon></div>
          <div class="metric-card__body">
            <p class="metric-card__label">高风险未就业学生</p>
            <p class="metric-card__value">{{ unemployedStudents.length }}</p>
            <p class="metric-card__sub">{{ highRiskCount > 0 ? `${highRiskCount} 人未登记或审核未通过` : '未就业学生风险可控' }}</p>
          </div>
        </div>
        <div class="metric-card metric-card--blue">
          <div class="metric-card__icon"><el-icon><Clock /></el-icon></div>
          <div class="metric-card__body">
            <p class="metric-card__label">近 7 天新增就业</p>
            <p class="metric-card__value">{{ recentTrend }}</p>
            <p class="metric-card__sub">近一周就业登记趋势</p>
          </div>
        </div>
      </div>

      <!-- ==================== 就业预警看板 ==================== -->
      <div class="section-title">
        <el-icon><Warning /></el-icon>
        就业预警看板
        <el-tag size="small" type="warning" effect="plain" style="margin-left:8px;">{{ alertSummary }}</el-tag>
      </div>
      <div class="alert-grid">
        <!-- 院系预警 -->
        <div class="alert-card" :class="belowAvgDepts.length > 0 ? 'alert-card--warn' : 'alert-card--ok'">
          <div class="alert-card__header">
            <el-icon><OfficeBuilding /></el-icon>
            <span>院系预警</span>
            <span class="alert-card__count">{{ belowAvgDepts.length }}</span>
          </div>
          <div class="alert-card__body scroll-list" v-if="belowAvgDepts.length > 0">
            <div v-for="d in belowAvgDepts.slice(0, 3)" :key="d.deptName" class="alert-item">
              <span class="alert-item__name">{{ d.deptName }}</span>
              <span class="alert-item__rate">{{ Number(d.employmentRate).toFixed(1) }}%</span>
              <el-tag size="small" type="danger" effect="plain">低</el-tag>
            </div>
          </div>
          <div class="alert-card__body alert-card__body--ok" v-else>
            <span class="ok-text"><el-icon><CircleCheck /></el-icon> 所有院系就业率均达标</span>
          </div>
          <div class="alert-card__action">
            <el-button size="small" text type="primary" @click="activeTab = 'dept'">查看详情</el-button>
          </div>
        </div>

        <!-- 专业预警 -->
        <div class="alert-card" :class="criticalMajorCount > 0 ? 'alert-card--warn' : 'alert-card--ok'">
          <div class="alert-card__header">
            <el-icon><Reading /></el-icon>
            <span>专业预警</span>
            <span class="alert-card__count">{{ criticalMajorCount > 0 ? criticalMajorCount : '—' }}</span>
          </div>
          <div class="alert-card__body scroll-list" v-if="criticalMajorCount > 0 && belowAvgMajors.length > 0">
            <div v-for="m in belowAvgMajors.slice(0, 3)" :key="m.majorName" class="alert-item">
              <span class="alert-item__name">{{ m.majorName }}</span>
              <span class="alert-item__rate">{{ Number(m.employmentRate).toFixed(1) }}%</span>
              <el-tag size="small" :type="m.warningLevel === '重点关注' ? 'danger' : 'warning'" effect="plain">{{ m.warningLevel }}</el-tag>
            </div>
          </div>
          <div class="alert-card__body alert-card__body--ok" v-else>
            <span class="ok-text"><el-icon><CircleCheck /></el-icon> 各专业就业率表现良好</span>
          </div>
          <div class="alert-card__action">
            <el-button size="small" text type="primary" @click="activeTab = 'major'" :disabled="criticalMajorCount === 0">查看名单</el-button>
          </div>
        </div>

        <!-- 班级预警 -->
        <div class="alert-card" :class="criticalClasses.length > 0 ? 'alert-card--danger' : 'alert-card--ok'">
          <div class="alert-card__header">
            <el-icon><School /></el-icon>
            <span>班级预警</span>
            <span class="alert-card__count">{{ criticalClasses.length }}</span>
          </div>
          <div class="alert-card__body scroll-list" v-if="criticalClasses.length > 0">
            <div v-for="c in criticalClasses.slice(0, 3)" :key="c.className" class="alert-item">
              <span class="alert-item__name">{{ c.className }}</span>
              <span class="alert-item__rate">{{ Number(c.employmentRate).toFixed(1) }}%</span>
              <el-tag size="small" type="danger" effect="plain">重点</el-tag>
            </div>
          </div>
          <div class="alert-card__body alert-card__body--ok" v-else>
            <span class="ok-text"><el-icon><CircleCheck /></el-icon> 暂无重点帮扶班级</span>
          </div>
          <div class="alert-card__action">
            <el-button size="small" text type="primary" @click="activeTab = 'class'">查看详情</el-button>
          </div>
        </div>

        <!-- 学生风险预警 -->
        <div class="alert-card" :class="highRiskCount > 0 ? 'alert-card--danger' : 'alert-card--ok'">
          <div class="alert-card__header">
            <el-icon><User /></el-icon>
            <span>学生风险预警</span>
            <span class="alert-card__count">{{ highRiskCount > 0 ? highRiskCount : '—' }}</span>
          </div>
          <div class="alert-card__body" v-if="highRiskCount > 0">
            <p class="alert-desc">
              有 {{ highRiskCount }} 名学生未登记就业或审核未通过，请通知班主任重点跟进帮扶。
            </p>
          </div>
          <div class="alert-card__body alert-card__body--ok" v-else>
            <span class="ok-text"><el-icon><CircleCheck /></el-icon> 未就业学生风险可控</span>
          </div>
          <div class="alert-card__action">
            <el-button size="small" text type="primary" @click="activeTab = 'student'" :disabled="unemployedStudents.length === 0">查看名单</el-button>
          </div>
        </div>
      </div>

      <!-- ==================== 低就业率排名 + 反馈建议 ==================== -->
      <div class="two-col-grid">
        <!-- 排名区 -->
        <div class="card">
          <div class="card__header">
            <div class="card__title">
              <el-icon><TrendCharts /></el-icon>
              低就业率对象排名
            </div>
            <el-tabs v-model="activeTab" size="small" @tab-change="handleTabChange" class="rank-tabs">
              <el-tab-pane label="院系" name="dept" />
              <el-tab-pane label="专业" name="major" />
              <el-tab-pane label="班级" name="class" />
              <el-tab-pane label="学生" name="student" />
            </el-tabs>
          </div>
          <!-- 院系排名 -->
          <div v-if="activeTab === 'dept'" class="scroll-list">
            <div v-if="belowAvgDepts.length === 0" class="empty-state">
              <el-icon :size="32" color="#22c55e"><CircleCheck /></el-icon>
              <span>所有院系就业率均达标</span>
            </div>
            <div v-else>
              <div class="rank-header">
                <span>排名</span><span>院系</span><span>总人数</span><span>已就业</span><span>就业率</span><span>偏差</span><span>建议</span>
              </div>
              <div v-for="(d, idx) in belowAvgDepts" :key="d.deptName" class="rank-row">
                <span class="rank-num" :class="idx < 3 ? 'rank-num--danger' : ''">{{ idx + 1 }}</span>
                <span class="rank-name">{{ d.deptName }}</span>
                <span>{{ d.totalStudents }}</span>
                <span style="color:#22c55e">{{ d.employed }}</span>
                <span class="rate-badge" :class="Number(d.employmentRate) < 60 ? 'rate-badge--red' : Number(d.employmentRate) < 75 ? 'rate-badge--orange' : 'rate-badge--yellow'">
                  {{ Number(d.employmentRate).toFixed(1) }}%
                </span>
                <span style="color:#ef4444">-{{ (overallRate - Number(d.employmentRate)).toFixed(1) }}%</span>
                <el-button size="small" text type="primary" @click="openTargetStudents('dept', d)">查看学生</el-button>
              </div>
            </div>
          </div>
          <!-- 专业排名 -->
          <div v-if="activeTab === 'major'" class="scroll-list">
            <div v-if="belowAvgMajors.length === 0" class="empty-state">
              <el-icon :size="32" color="#22c55e"><CircleCheck /></el-icon>
              <span>所有专业就业率均达标</span>
            </div>
            <div v-else>
              <div class="rank-header">
                <span>排名</span><span>专业</span><span>院系</span><span>总人数</span><span>已就业</span><span>就业率</span><span>偏差</span><span>建议</span>
              </div>
              <div v-for="(m, idx) in belowAvgMajors" :key="m.majorName" class="rank-row">
                <span class="rank-num" :class="idx < 3 ? 'rank-num--danger' : ''">{{ idx + 1 }}</span>
                <span class="rank-name">{{ m.majorName }}</span>
                <span>{{ m.deptName || '—' }}</span>
                <span>{{ m.totalStudents }}</span>
                <span style="color:#22c55e">{{ m.employed }}</span>
                <span class="rate-badge" :class="Number(m.employmentRate) < 60 ? 'rate-badge--red' : Number(m.employmentRate) < 75 ? 'rate-badge--orange' : 'rate-badge--yellow'">
                  {{ Number(m.employmentRate).toFixed(1) }}%
                </span>
                <span style="color:#ef4444">-{{ (overallRate - Number(m.employmentRate)).toFixed(1) }}%</span>
                <el-button size="small" text type="primary" @click="openTargetStudents('major', m)">查看学生</el-button>
              </div>
            </div>
          </div>
          <!-- 班级排名 -->
          <div v-if="activeTab === 'class'" class="scroll-list">
            <div v-if="criticalClasses.length === 0" class="empty-state">
              <el-icon :size="32" color="#22c55e"><CircleCheck /></el-icon>
              <span>暂无重点帮扶班级</span>
            </div>
            <div v-else>
              <div class="rank-header">
                <span>排名</span><span>班级</span><span>院系</span><span>总人数</span><span>就业率</span><span>偏差</span><span>建议</span>
              </div>
              <div v-for="(c, idx) in criticalClasses" :key="c.className" class="rank-row">
                <span class="rank-num" :class="idx < 3 ? 'rank-num--danger' : ''">{{ idx + 1 }}</span>
                <span class="rank-name">{{ c.className }}</span>
                <span>{{ c.deptName || '—' }}</span>
                <span>{{ c.totalStudents }}</span>
                <span class="rate-badge rate-badge--red">{{ Number(c.employmentRate).toFixed(1) }}%</span>
                <span style="color:#ef4444">-{{ (overallRate - Number(c.employmentRate)).toFixed(1) }}%</span>
                <el-button size="small" text type="primary" @click="openTargetStudents('class', c)">查看学生</el-button>
              </div>
            </div>
          </div>
          <!-- 学生排名 -->
          <div v-if="activeTab === 'student'" class="scroll-list">
            <div v-if="unemployedStudents.length === 0" class="empty-state">
              <el-icon :size="32" color="#22c55e"><CircleCheck /></el-icon>
              <span>所有学生已就业</span>
            </div>
            <div v-else>
              <div class="rank-header">
                <span>姓名</span><span>学院</span><span>专业</span><span>班级</span><span>风险等级</span><span>风险原因</span><span>建议</span>
              </div>
              <div v-for="s in unemployedStudents" :key="s.studentId" class="rank-row">
                <span class="rank-name">{{ s.realName || '—' }}</span>
                <span>{{ s.deptName || '—' }}</span>
                <span>{{ s.majorName || '—' }}</span>
                <span>{{ s.className || '—' }}</span>
                <el-tag size="small" :type="s.riskLevel === '高风险' ? 'danger' : s.riskLevel === '中风险' ? 'warning' : 'info'" effect="plain">{{ s.riskLevel || '—' }}</el-tag>
                <span style="color:#f59e0b;font-size:12px;">{{ s.riskReason || '—' }}</span>
                <el-button size="small" text type="primary" @click="openStudentDetail(s)">查看详情</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 反馈建议 -->
        <div class="card">
          <div class="card__header">
            <div class="card__title">
              <el-icon><ChatDotSquare /></el-icon>
              反馈建议
            </div>
            <el-button size="small" type="primary" @click="generateAdvice" :loading="adviceLoading">
              <el-icon><Refresh /></el-icon> 生成建议
            </el-button>
          </div>
          <div class="advice-list">
            <div class="advice-card">
              <div class="advice-card__title">
                <el-icon><UserFilled /></el-icon>
                给校级老师的建议
              </div>
              <div class="advice-card__body" :class="{ 'advice-card__body--truncated': !schoolExpanded }">
                <p v-if="advice.school" class="advice-text">{{ advice.school }}</p>
                <p v-else class="advice-placeholder">点击"生成建议"获取校级分析报告</p>
              </div>
              <div class="advice-card__footer" v-if="advice.school">
                <el-button size="small" text @click="copyAdvice(advice.school)">
                  <el-icon><DocumentCopy /></el-icon> 复制
                </el-button>
                <el-button size="small" text type="primary">
                  <el-icon><Download /></el-icon> 导出
                </el-button>
                <el-button size="small" text @click="schoolExpanded = !schoolExpanded" style="margin-left:auto;">
                  {{ schoolExpanded ? '收起' : '展开' }}
                </el-button>
              </div>
            </div>
            <div class="advice-card">
              <div class="advice-card__title">
                <el-icon><OfficeBuilding /></el-icon>
                给院级老师的建议
              </div>
              <div class="advice-card__body" :class="{ 'advice-card__body--truncated': !deptExpanded }">
                <p v-if="advice.dept" class="advice-text">{{ advice.dept }}</p>
                <p v-else class="advice-placeholder">点击"生成建议"获取院级分析报告</p>
              </div>
              <div class="advice-card__footer" v-if="advice.dept">
                <el-button size="small" text @click="copyAdvice(advice.dept)">
                  <el-icon><DocumentCopy /></el-icon> 复制
                </el-button>
                <el-button size="small" text type="primary">
                  <el-icon><Download /></el-icon> 导出
                </el-button>
                <el-button size="small" text @click="deptExpanded = !deptExpanded" style="margin-left:auto;">
                  {{ deptExpanded ? '收起' : '展开' }}
                </el-button>
              </div>
            </div>
            <div class="advice-card">
              <div class="advice-card__title">
                <el-icon><School /></el-icon>
                给班主任的建议
              </div>
              <div class="advice-card__body" :class="{ 'advice-card__body--truncated': !teacherExpanded }">
                <p v-if="advice.teacher" class="advice-text">{{ advice.teacher }}</p>
                <p v-else class="advice-placeholder">点击"生成建议"获取班主任跟进建议</p>
              </div>
              <div class="advice-card__footer" v-if="advice.teacher">
                <el-button size="small" text @click="copyAdvice(advice.teacher)">
                  <el-icon><DocumentCopy /></el-icon> 复制
                </el-button>
                <el-button size="small" text type="primary">
                  <el-icon><Download /></el-icon> 导出
                </el-button>
                <el-button size="small" text @click="teacherExpanded = !teacherExpanded" style="margin-left:auto;">
                  {{ teacherExpanded ? '收起' : '展开' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 就业结构分析 ==================== -->
      <div class="two-col-grid">
        <!-- 就业类型分布 -->
        <div class="card">
          <div class="card__header">
            <div class="card__title">
              <el-icon><PieChart /></el-icon>
              就业类型分布
            </div>
            <el-tag size="small" type="info" effect="plain">总计 {{ totalEmployed }} 人</el-tag>
          </div>
          <div ref="typeChartRef" class="chart-container" v-if="hasTypeData"></div>
          <div class="empty-state" v-else>
            <el-icon :size="32" color="#c0c4cc"><PieChart /></el-icon>
            <span>暂无就业类型数据</span>
          </div>
          <div class="analysis-conclusion" v-if="hasTypeData">
            <el-icon><InfoFilled /></el-icon>
            <div class="analysis-conclusion__text">
              <strong>就业类型分析：</strong>
              {{ typeConclusion }}
            </div>
          </div>
        </div>

        <!-- 薪资分布 -->
        <div class="card">
          <div class="card__header">
            <div class="card__title">
              <el-icon><Money /></el-icon>
              薪资分布
            </div>
            <el-tag size="small" type="info" effect="plain">有效数据 {{ salaryCount }} 条</el-tag>
          </div>
          <div ref="salaryChartRef" class="chart-container" v-if="hasSalaryData"></div>
          <div class="empty-state" v-else>
            <el-icon :size="32" color="#c0c4cc"><Money /></el-icon>
            <span>暂无薪资数据</span>
          </div>
          <div class="analysis-conclusion" v-if="hasSalaryData">
            <el-icon><InfoFilled /></el-icon>
            <div class="analysis-conclusion__text">
              <strong>薪资分布分析：</strong>
              {{ salaryConclusion }}
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 辅助分析 Tabs ==================== -->
      <div class="card">
        <div class="card__header">
          <div class="card__title">
            <el-icon><PieChart /></el-icon>
            辅助分析
          </div>
        </div>
        <div class="aux-tabs">
          <el-tabs v-model="auxTab" @tab-change="handleAuxTabChange" class="aux-tabs__inner">
            <el-tab-pane label="行业分布" name="industry" />
            <el-tab-pane label="院系就业率对比" name="dept" />
            <el-tab-pane label="班级就业率 TOP10" name="class" />
            <el-tab-pane label="专业就业率" name="major" />
          </el-tabs>

          <!-- 行业分布 -->
          <div v-show="auxTab === 'industry'" class="aux-content">
            <div ref="industryChartRef" class="chart-container" v-if="hasIndustryData"></div>
            <div class="empty-state" v-else>
              <el-icon :size="32" color="#c0c4cc"><Briefcase /></el-icon>
              <span>暂无行业数据</span>
            </div>
            <div class="analysis-conclusion" v-if="hasIndustryData">
              <el-icon><InfoFilled /></el-icon>
              <div class="analysis-conclusion__text">
                <strong>行业分析：</strong>
                {{ industryConclusion }}
              </div>
            </div>
          </div>

          <!-- 院系就业率对比 -->
          <div v-show="auxTab === 'dept'" class="aux-content">
            <div ref="deptChartRef" class="chart-container"></div>
          </div>

          <!-- 班级就业率 TOP10 -->
          <div v-show="auxTab === 'class'" class="aux-content">
            <div ref="classChartRef" class="chart-container"></div>
          </div>

          <!-- 专业就业率 -->
          <div v-show="auxTab === 'major'" class="aux-content">
            <div ref="majorChartRef" class="chart-container"></div>
          </div>
        </div>
      </div>

      <!-- ==================== 就业地区分布地图（全宽重点模块） ==================== -->
      <div class="card card--map">
        <div class="card__header">
          <div class="card__title">
            <el-icon><Location /></el-icon>
            就业地区分布
          </div>
          <el-tag size="small" type="info" effect="plain">共 {{ provinceTotal || 0 }} 人</el-tag>
        </div>
        <div class="map-layout">
          <!-- 地图主体 -->
          <div class="map-main">
            <div ref="mapChartRef" class="map-container" v-if="hasRegionData"></div>
            <div class="map-empty" v-else>
              <el-icon :size="40" color="#c0c4cc"><Location /></el-icon>
              <span>暂无地区分布数据</span>
            </div>
          </div>
          <!-- 右侧 TOP5 列表 -->
          <div class="map-aside" v-if="provinceTop5.length > 0">
            <p class="map-aside__title">TOP{{ provinceTop5.length }} 地区</p>
            <div class="map-aside__list scroll-list">
              <div v-for="(p, idx) in provinceTop5" :key="p.name" class="map-aside__item">
                <span class="map-aside__rank" :class="idx < 3 ? 'map-aside__rank--top' : ''">{{ idx + 1 }}</span>
                <div class="map-aside__info">
                  <span class="map-aside__name">{{ p.name }}</span>
                  <div class="map-aside__bar-wrap">
                    <div class="map-aside__bar" :style="{ width: provinceTotal > 0 ? ((p.value / provinceTotal) * 100).toFixed(1) + '%' : '0%' }"></div>
                  </div>
                </div>
                <div class="map-aside__nums">
                  <span class="map-aside__value">{{ p.value }}</span>
                  <span class="map-aside__pct">{{ provinceTotal > 0 ? ((p.value / provinceTotal) * 100).toFixed(1) : 0 }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="analysis-conclusion" v-if="hasRegionData">
          <el-icon><InfoFilled /></el-icon>
          <div class="analysis-conclusion__text">
            <strong>地区流向分析：</strong>
            <span :class="['analysis-summary', { expanded: regionExpanded }]">{{ regionConclusion }}</span>
            <el-button v-if="regionConclusion && regionConclusion.length > 60" link size="small" @click="regionExpanded = !regionExpanded" style="padding:0;margin-left:4px;">
              {{ regionExpanded ? '收起' : '展开' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- ==================== 重点帮扶学生名单 ==================== -->
      <div class="card card--student">
        <div class="card__header">
          <div class="card__title">
            <el-icon><User /></el-icon>
            重点帮扶学生名单
            <el-tag size="small" type="warning" effect="plain" style="margin-left:6px;">{{ highRiskCount }} 人</el-tag>
          </div>
          <div class="panel-actions">
            <el-button size="small" text type="primary" @click="notifyAll" :disabled="unemployedStudents.length === 0">
              <el-icon><Message /></el-icon> 通知班主任
            </el-button>
            <el-button size="small" text type="primary" @click="exportStudentList" :disabled="unemployedStudents.length === 0">
              <el-icon><Download /></el-icon> 导出名单
            </el-button>
          </div>
        </div>
        <div class="student-table-wrap" v-if="unemployedStudents.length === 0">
          <div class="empty-state">
            <el-icon :size="32" color="#22c55e"><CircleCheck /></el-icon>
            <span>所有学生均已就业</span>
          </div>
        </div>
        <div v-else class="student-table-wrap">
          <div class="student-table-inner scroll-list">
            <div class="student-table-hd">
              <span>姓名</span><span>学号</span><span>学院</span><span>专业</span><span>班级</span><span>风险等级</span><span>风险原因</span><span>建议</span>
            </div>
            <div v-for="s in displayedStudents" :key="s.studentId" class="student-row">
              <span class="cell-name">{{ s.realName || '—' }}</span>
              <span class="cell-ellipsis">{{ s.studentNo || '—' }}</span>
              <span class="cell-ellipsis">{{ s.deptName || '—' }}</span>
              <span class="cell-ellipsis">{{ s.majorName || '—' }}</span>
              <span class="cell-ellipsis">{{ s.className || '—' }}</span>
              <el-tag size="small" :type="s.riskLevel === '高风险' ? 'danger' : s.riskLevel === '中风险' ? 'warning' : 'info'" effect="plain">{{ s.riskLevel || '—' }}</el-tag>
              <span class="cell-risk">{{ s.riskReason || '—' }}</span>
              <el-button size="small" text type="primary" @click="openStudentDetail(s)">查看详情</el-button>
            </div>
          </div>
        </div>
      </div>

    </div><!-- /page-body -->

    <!-- ======= 弹窗：院系/班级/专业学生列表 ======= -->
    <el-dialog v-model="targetDialogVisible" :title="targetDialogTitle" width="900px" :append-to-body="true">
      <div class="dialog-toolbar">
        <el-tag size="small" type="info">共 {{ targetStudents.length }} 人</el-tag>
      </div>
      <el-table :data="targetStudents" stripe border max-height="420" style="width:100%">
        <el-table-column prop="realName" label="姓名" width="90" fixed />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="deptName" label="学院" min-width="120" />
        <el-table-column prop="majorName" label="专业" min-width="120" />
        <el-table-column prop="className" label="班级" min-width="120" />
        <el-table-column label="风险等级" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.riskLevel === '高风险' ? 'danger' : row.riskLevel === '中风险' ? 'warning' : 'info'" effect="plain">{{ row.riskLevel || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskReason" label="风险原因" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openStudentDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- ======= 弹窗：学生详细信息 ======= -->
    <el-dialog v-model="studentDetailVisible" title="学生详细信息" width="700px" :append-to-body="true">
      <div v-if="studentDetail" class="student-detail">
        <div class="detail-section">
          <h4 class="detail-section__title">基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-label">姓名</span><span class="detail-value">{{ studentDetail.realName || '—' }}</span></div>
            <div class="detail-item"><span class="detail-label">学号</span><span class="detail-value">{{ studentDetail.studentNo || '—' }}</span></div>
            <div class="detail-item"><span class="detail-label">性别</span><span class="detail-value">{{ studentDetail.gender || '—' }}</span></div>
            <div class="detail-item"><span class="detail-label">毕业年份</span><span class="detail-value">{{ studentDetail.graduationYear || '—' }}</span></div>
            <div class="detail-item"><span class="detail-label">学院</span><span class="detail-value">{{ studentDetail.deptName || '—' }}</span></div>
            <div class="detail-item"><span class="detail-label">专业</span><span class="detail-value">{{ studentDetail.majorName || '—' }}</span></div>
            <div class="detail-item"><span class="detail-label">班级</span><span class="detail-value">{{ studentDetail.className || '—' }}</span></div>
          </div>
        </div>
        <div class="detail-section">
          <h4 class="detail-section__title">就业风险评估</h4>
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-label">风险等级</span>
              <el-tag size="small" :type="studentDetail.riskLevel === '高风险' ? 'danger' : studentDetail.riskLevel === '中风险' ? 'warning' : 'info'" effect="plain">{{ studentDetail.riskLevel || '—' }}</el-tag>
            </div>
            <div class="detail-item full-width"><span class="detail-label">风险原因</span><span class="detail-value" style="color:#f59e0b;">{{ studentDetail.riskReason || '—' }}</span></div>
          </div>
          <div class="risk-suggestion">
            <el-icon><InfoFilled /></el-icon>
            <div>
              <strong>帮扶建议：</strong>
              <span>{{ getStudentSuggestion(studentDetail) }}</span>
            </div>
          </div>
        </div>
        <div class="detail-section" v-if="studentDetail.employmentType">
          <h4 class="detail-section__title">就业登记信息</h4>
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-label">就业类型</span><span class="detail-value">{{ studentDetail.employmentType }}</span></div>
            <div class="detail-item"><span class="detail-label">登记状态</span>
              <el-tag size="small" :type="studentDetail.recordStatus === 'approved' ? 'success' : studentDetail.recordStatus === 'pending' ? 'warning' : 'danger'" effect="plain">
                {{ studentDetail.recordStatus === 'approved' ? '已审核' : studentDetail.recordStatus === 'pending' ? '待审核' : studentDetail.recordStatus === 'rejected' ? '已驳回' : '未知' }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="detail-section" v-else>
          <div class="no-record">
            <el-icon><Warning /></el-icon>
            <span>该学生尚未登记任何就业信息，请班主任及时跟进。</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- ======= 弹窗：专业详情列表 ======= -->
    <el-dialog v-model="majorDetailVisible" title="专业就业详情" width="900px" :append-to-body="true">
      <div v-if="majorDetail" class="major-detail">
        <div class="major-summary">
          <div class="major-stat">
            <span class="major-stat__value">{{ majorDetail.totalStudents }}</span>
            <span class="major-stat__label">专业总人数</span>
          </div>
          <div class="major-stat">
            <span class="major-stat__value" style="color:#22c55e;">{{ majorDetail.employed }}</span>
            <span class="major-stat__label">已就业人数</span>
          </div>
          <div class="major-stat">
            <span class="major-stat__value" style="color:#ef4444;">{{ majorDetail.unemployed }}</span>
            <span class="major-stat__label">未就业人数</span>
          </div>
          <div class="major-stat">
            <span class="major-stat__value" :style="{ color: Number(majorDetail.employmentRate) < 70 ? '#ef4444' : '#22c55e' }">{{ Number(majorDetail.employmentRate).toFixed(1) }}%</span>
            <span class="major-stat__label">就业率</span>
          </div>
          <div class="major-stat">
            <el-tag size="small" :type="majorDetail.warningLevel === '重点关注' ? 'danger' : 'warning'" effect="plain">{{ majorDetail.warningLevel }}</el-tag>
            <span class="major-stat__label">预警等级</span>
          </div>
        </div>
        <div class="major-chart-wrap">
          <div ref="majorDetailChartRef" style="width:100%;height:200px;"></div>
        </div>
        <el-table :data="majorUnemployedStudents" stripe border max-height="320" style="width:100%;margin-top:12px;">
          <el-table-column prop="realName" label="姓名" width="90" />
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="className" label="班级" min-width="120" />
          <el-table-column label="风险等级" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.riskLevel === '高风险' ? 'danger' : row.riskLevel === '中风险' ? 'warning' : 'info'" effect="plain">{{ row.riskLevel || '—' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="riskReason" label="风险原因" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openStudentDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh, Loading, TrendCharts, Warning, CircleCheck,
  User, Clock, OfficeBuilding, School, Reading,
  Document, PieChart, Money, Briefcase, Location,
  ChatDotSquare, UserFilled, DocumentCopy, Download,
  InfoFilled, Message
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { analyticsApi, teacherApi } from '@/api'
import request from '@/utils/request'

// ==================== 数据状态 ====================
const loading = ref(false)
const adviceLoading = ref(false)
const selectedYear = ref('')
const years = ref([])

const overview = ref({})
const trend = ref({})
const salary = ref({})
const industry = ref({})
const province = ref({})
const deptStats = ref([])
const classStats = ref([])

// 未就业学生数据（来自真实 API）
const unemployedStudentsRes = ref({ list: [], total: 0 })

// 专业预警数据（来自真实 API）
const majorStats = ref([])

// 活跃标签
const activeTab = ref('dept')

// 辅助分析 Tabs
const auxTab = ref('industry')

// 反馈建议
const advice = ref({ school: '', dept: '', teacher: '' })

// 展开收起状态
const regionExpanded = ref(false)
const schoolExpanded = ref(false)
const deptExpanded = ref(false)
const teacherExpanded = ref(false)

// 学生名单分页
const studentPageSize = ref(100000) // 设置足够大，让后端返回全部

// ==================== 弹窗状态 ====================
const targetDialogVisible = ref(false)
const targetDialogTitle = ref('')
const targetStudents = ref([])

const studentDetailVisible = ref(false)
const studentDetail = ref(null)

const majorDetailVisible = ref(false)
const majorDetail = ref(null)
const majorDetailChartRef = ref(null)
const majorUnemployedStudents = computed(() => {
  if (!majorDetail.value) return []
  return unemployedStudents.value.filter(s => s.majorName === majorDetail.value.majorName)
})
let majorDetailChart = null

// ==================== 图表 ref ====================
const typeChartRef = ref(null)
const salaryChartRef = ref(null)
const industryChartRef = ref(null)
const mapChartRef = ref(null)
const deptChartRef = ref(null)
const classChartRef = ref(null)
const majorChartRef = ref(null)

let typeChart = null
let salaryChart = null
let industryChart = null
let mapChart = null
let deptChart = null
let classChart = null
let majorChart = null
let resizeObserver = null

// 省份名称映射
const PROVINCE_NAME_MAP = {
  '北京': '北京市', '天津': '天津市', '上海': '上海市', '重庆': '重庆市',
  '河北': '河北省', '山西': '山西省', '辽宁': '辽宁省', '吉林': '吉林省',
  '黑龙江': '黑龙江省', '江苏': '江苏省', '浙江': '浙江省', '安徽': '安徽省',
  '福建': '福建省', '江西': '江西省', '山东': '山东省', '河南': '河南省',
  '湖北': '湖北省', '湖南': '湖南省', '广东': '广东省', '海南': '海南省',
  '四川': '四川省', '贵州': '贵州省', '云南': '云南省', '陕西': '陕西省',
  '甘肃': '甘肃省', '青海': '青海省', '内蒙古': '内蒙古自治区',
  '广西': '广西壮族自治区', '西藏': '西藏自治区', '宁夏': '宁夏回族自治区',
  '新疆': '新疆维吾尔自治区', '台湾': '台湾省', '香港': '香港特别行政区', '澳门': '澳门特别行政区'
}

function normalizeProvince(name) {
  if (!name) return null
  const trimmed = String(name).trim()
  if (PROVINCE_NAME_MAP[trimmed]) return PROVINCE_NAME_MAP[trimmed]
  if (trimmed.endsWith('省') || trimmed.endsWith('市') || trimmed.endsWith('自治区') ||
      trimmed.endsWith('特别行政区')) return trimmed
  return PROVINCE_NAME_MAP[trimmed + '省'] || trimmed
}

const COLOR_PALETTE = ['#2f6bff', '#22c55e', '#f59e0b', '#ef4444', '#009688', '#9c27b0', '#3b82f6', '#f97316']

// ==================== 计算属性 ====================
const overallRate = computed(() => Number(overview.value.employmentRate) || 0)
const unemployedCount = computed(() => Number(overview.value.unemployed) || 0)
const totalEmployed = computed(() => Number(overview.value.employed) || 0)
const totalStudents = computed(() => Number(overview.value.totalStudents) || 0)

const recentTrend = computed(() => Number(overview.value.recent7DaysCount) || 0)

const belowAvgDepts = computed(() => {
  if (!overallRate.value) return []
  return deptStats.value
    .filter(d => Number(d.employmentRate) < overallRate.value)
    .sort((a, b) => Number(a.employmentRate) - Number(b.employmentRate))
})

const belowAvgDeptCount = computed(() => belowAvgDepts.value.length)

const criticalClasses = computed(() => {
  if (!overallRate.value) return []
  const threshold = overallRate.value - 5
  return classStats.value
    .filter(c => Number(c.employmentRate) < threshold)
    .sort((a, b) => Number(a.employmentRate) - Number(b.employmentRate))
})

const criticalClassCount = computed(() => criticalClasses.value.length)

const belowAvgMajors = computed(() => {
  if (!overallRate.value || !majorStats.value.length) return []
  return majorStats.value
    .filter(m => Number(m.employmentRate) < overallRate.value)
    .sort((a, b) => Number(a.employmentRate) - Number(b.employmentRate))
})

const criticalMajorCount = computed(() => belowAvgMajors.value.length)

const unemployedStudents = computed(() => unemployedStudentsRes.value.list || [])

const highRiskCount = computed(() => Number(unemployedStudentsRes.value.total) || 0)

const displayedStudents = computed(() => unemployedStudents.value)

const alertSummary = computed(() => {
  const parts = []
  if (belowAvgDeptCount.value > 0) parts.push(`${belowAvgDeptCount.value} 个院系预警`)
  if (criticalMajorCount.value > 0) parts.push(`${criticalMajorCount.value} 个专业预警`)
  if (criticalClassCount.value > 0) parts.push(`${criticalClassCount.value} 个班级需帮扶`)
  if (highRiskCount.value > 0) parts.push(`${highRiskCount.value} 名高风险学生`)
  return parts.length > 0 ? parts.join(' | ') : '暂无预警'
})

const provinceTop5 = computed(() => {
  const dist = province.value.distribution || []
  return Array.isArray(dist)
    ? [...dist]
        .sort((a, b) => Number(b.value || 0) - Number(a.value || 0))
        .slice(0, 5)
        .map(d => ({ name: normalizeProvince(d.name || d) || '未知', value: Number(d.value || 0) }))
        .filter(d => d.name)
    : []
})

const provinceTotal = computed(() => provinceTop5.value.reduce((s, p) => s + p.value, 0))

const hasTypeData = computed(() => Object.keys(trend.value.typeCount || {}).length > 0)
const hasSalaryData = computed(() => Object.keys(salary.value.distribution || {}).length > 0)
const hasIndustryData = computed(() => Array.isArray(industry.value.distribution) && industry.value.distribution.length > 0)
const hasRegionData = computed(() => Array.isArray(province.value.distribution) && province.value.distribution.length > 0)
const salaryCount = computed(() => {
  const dist = salary.value.distribution || {}
  return Object.values(dist).reduce((a, b) => a + Number(b), 0)
})

// ==================== 分析结论 ====================
const typeConclusion = computed(() => {
  const typeCount = trend.value.typeCount || {}
  const entries = Object.entries(typeCount).sort((a, b) => b[1] - a[1])
  if (entries.length === 0) return '暂无数据'
  const total = entries.reduce((a, b) => a + Number(b[1]), 0)
  const top = entries[0]
  const topPercent = total > 0 ? (Number(top[1]) / total * 100).toFixed(1) : 0
  let conclusion = `${top[0]}占比最高（${topPercent}%），`
  if (top[0].includes('签约') || top[0].includes('就业')) {
    conclusion += '主流就业方式为签约就业，整体结构合理。'
  } else if (top[0].includes('升学') || top[0].includes('深造')) {
    conclusion += '升学深造比例较高，说明学生学历提升意愿强，建议关注就业市场与深造专业的衔接。'
  } else if (top[0].includes('创业')) {
    conclusion += '自主创业比例良好，创业氛围较浓。'
  } else {
    conclusion += '建议关注各类就业类型占比合理性。'
  }
  const otherCount = entries.filter(e => e[0] !== top[0]).length
  if (otherCount > 3) {
    conclusion += ` 另有 ${otherCount} 种其他就业类型，建议关注各类占比均衡性。`
  }
  return conclusion
})

const salaryConclusion = computed(() => {
  const dist = salary.value.distribution || {}
  const labels = ['5k以下', '5k-8k', '8k-12k', '12k-20k', '20k以上']
  const values = labels.map(l => Number(dist[l]) || 0)
  const total = values.reduce((a, b) => a + b, 0)
  if (total === 0) return '暂无数据'
  const lowSalary = values[0]
  const midSalary = values[1] + values[2]
  const highSalary = values[3] + values[4]
  const lowPct = (lowSalary / total * 100).toFixed(1)
  const midPct = (midSalary / total * 100).toFixed(1)
  const highPct = (highSalary / total * 100).toFixed(1)
  if (Number(lowPct) > 30) {
    return `低薪区间（5k以下）占 ${lowPct}%，比例偏高，建议重点关注低薪岗位质量，引导学生提升求职竞争力。`
  } else if (Number(highPct) > 30) {
    return `中高薪区间（12k以上）占 ${highPct}%，整体薪资质量较好，建议继续保持高质量企业资源对接。`
  } else {
    return `中等薪资（5k-12k）占 ${midPct}%，薪资结构合理；低薪 ${lowPct}%，高薪 ${highPct}%，整体呈橄榄型分布。`
  }
})

const industryConclusion = computed(() => {
  const dist = industry.value.distribution || []
  const top5 = Array.isArray(dist) ? dist.slice(0, 5) : []
  if (top5.length === 0) return '暂无数据'
  const top3Names = top5.slice(0, 3).map(d => d.name || d).join('、')
  const count5 = top5.reduce((sum, d) => sum + Number(d.value || 0), 0)
  const total = Array.isArray(dist) ? dist.reduce((sum, d) => sum + Number(d.value || 0), 0) : count5
  const top5Pct = total > 0 ? (count5 / total * 100).toFixed(1) : 0
  return `TOP5 行业为 ${top3Names}，占全部 ${top5Pct}%。建议优先与头部行业企业加强合作，拓展实习和就业渠道。`
})

const regionConclusion = computed(() => {
  if (provinceTop5.value.length === 0) return '暂无数据'
  const top3Names = provinceTop5.value.slice(0, 3).map(p => p.name).join('、')
  const top5Pct = provinceTotal.value > 0
    ? ((provinceTop5.value.reduce((s, p) => s + p.value, 0) / Math.max(provinceTotal.value, 1)) * 100).toFixed(1)
    : '0'
  return `学生主要流向 ${top3Names} 等地区，前 5 省占 ${top5Pct}%。建议关注本地就业资源饱和度，适当拓展目标城市企业合作。`
})

// ==================== 反馈建议 ====================
function generateAdvice() {
  adviceLoading.value = true
  setTimeout(() => {
    const rate = overallRate.value
    const unemp = unemployedCount.value
    const deptCount = belowAvgDeptCount.value
    const classCount = criticalClassCount.value
    const riskCount = highRiskCount.value
    const lowestDept = belowAvgDepts.value[0]
    const lowestClass = criticalClasses.value[0]

    advice.value.school = `当前全校就业率为 ${rate.toFixed(1)}%，${unemp > 0 ? `尚有 ${unemp} 人未就业。` : '已实现全员就业。'} ` +
      `${deptCount > 0 ? `${deptCount} 个院系就业率低于全校平均，${lowestDept ? `其中 ${lowestDept.deptName} 最低（${Number(lowestDept.employmentRate).toFixed(1)}%），` : ''}` : ''}` +
      `${classCount > 0 ? `${classCount} 个班级需要重点帮扶，${lowestClass ? `${lowestClass.className} 就业率最低（${Number(lowestClass.employmentRate).toFixed(1)}%）。` : ''}` : ''}` +
      `${riskCount > 0 ? `${riskCount} 名学生未登记就业或审核未通过，存在高风险。` : ''}` +
      `建议近期组织召开全校就业推进会，针对性解决低就业率院系和班级的实际问题。`

    advice.value.dept = deptCount > 0
      ? `${deptCount} 个院系就业率低于全校平均 ${rate.toFixed(1)}%。` +
        `建议院级老师重点关注 ${belowAvgDepts.value.slice(0, 3).map(d => d.deptName).join('、')} 等院系，` +
        `督促相关班主任制定未就业学生帮扶计划，定期跟踪进展。` +
        `${unemp > 0 ? `全校尚有 ${unemp} 名未就业学生，建议加强就业指导和岗位推荐力度。` : ''}`
      : `当前各院系就业率均达标或接近目标，请继续保持推进力度，重点关注未就业学生的精准帮扶。`

    advice.value.teacher = classCount > 0
      ? `${classCount} 个班级就业率低于全校平均 5% 以上。` +
        `${criticalClasses.value.slice(0, 3).map(c => `${c.className}（${Number(c.employmentRate).toFixed(1)}%）`).join('、')} 等班级需要重点跟进。` +
        `建议班主任逐一摸排未就业学生情况，了解求职意向，一对一推荐合适岗位，帮助完善简历和面试准备。`
      : `各班级就业率表现良好，请继续保持对未就业学生的关注，定期跟进并及时更新就业去向登记。`

    adviceLoading.value = false
  }, 600)
}

function copyAdvice(text) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('建议已复制到剪贴板')
  }).catch(() => {
    ElMessage.warning('复制失败，请手动复制')
  })
}

// ==================== 导出报告 ====================
function generateReport() {
  if (loading.value) {
    ElMessage.warning('数据加载中，请稍候')
    return
  }

  const year = selectedYear.value || '全部届'
  const now = new Date().toLocaleString('zh-CN')

  const rate = overallRate.value || 0
  const unemp = unemployedCount.value || 0
  const highRisk = highRiskCount.value || 0
  const deptCount = belowAvgDeptCount.value || 0
  const majorCount = criticalMajorCount.value || 0
  const classCount = criticalClassCount.value || 0

  // 就业类型饼图数据
  const typeEntries = Object.entries(trend.value.typeCount || {}).sort((a, b) => b[1] - a[1])
  const typeRows = typeEntries.map(([name, val]) =>
    `            <tr><td>${name}</td><td style="text-align:right">${Number(val).toLocaleString()}</td><td style="text-align:right">${typeEntries.reduce((s, v) => s + Number(v[1]), 0) > 0 ? (Number(val) / typeEntries.reduce((s, v) => s + Number(v[1]), 0) * 100).toFixed(1) : 0}%</td></tr>`
  ).join('\n')

  // 薪资分布
  const salDist = salary.value.distribution || {}
  const salLabels = ['5k以下', '5k-8k', '8k-12k', '12k-20k', '20k以上']
  const salTotal = salLabels.reduce((s, k) => s + Number(salDist[k] || 0), 0)
  const salRows = salLabels.map(l => {
    const v = Number(salDist[l] || 0)
    return `            <tr><td>${l}</td><td style="text-align:right">${v.toLocaleString()}</td><td style="text-align:right">${salTotal > 0 ? (v / salTotal * 100).toFixed(1) : 0}%</td></tr>`
  }).join('\n')

  // 行业分布
  const indDist = Array.isArray(industry.value.distribution) ? industry.value.distribution.slice(0, 10) : []
  const indRows = indDist.map(d => {
    const name = d.name || '未知'
    const val = Number(d.value || 0)
    const total = indDist.reduce((s, x) => s + Number(x.value || 0), 0)
    return `            <tr><td>${name}</td><td style="text-align:right">${val.toLocaleString()}</td><td style="text-align:right">${total > 0 ? (val / total * 100).toFixed(1) : 0}%</td></tr>`
  }).join('\n')

  // 院系排名
  const deptRows = (belowAvgDepts.value || []).map((d, i) =>
    `            <tr><td style="text-align:center">${i + 1}</td><td>${d.deptName || '未知'}</td><td style="text-align:right">${Number(d.totalStudents || 0).toLocaleString()}</td><td style="text-align:right">${Number(d.employed || 0).toLocaleString()}</td><td style="text-align:right;color:#ef4444">${Number(d.employmentRate || 0).toFixed(1)}%</td><td style="text-align:right;color:#ef4444">-${(rate - Number(d.employmentRate || 0)).toFixed(1)}%</td></tr>`
  ).join('\n')

  // 专业排名
  const majorRows = (belowAvgMajors.value || []).map((m, i) =>
    `            <tr><td style="text-align:center">${i + 1}</td><td>${m.majorName || '未知'}</td><td>${m.deptName || '未知'}</td><td style="text-align:right">${Number(m.totalStudents || 0).toLocaleString()}</td><td style="text-align:right">${Number(m.employed || 0).toLocaleString()}</td><td style="text-align:right;color:#ef4444">${Number(m.employmentRate || 0).toFixed(1)}%</td><td style="text-align:right;color:#ef4444">-${(rate - Number(m.employmentRate || 0)).toFixed(1)}%</td></tr>`
  ).join('\n')

  // 班级排名
  const classRows = (criticalClasses.value || []).slice(0, 50).map((c, i) =>
    `            <tr><td style="text-align:center">${i + 1}</td><td>${c.className || '未知'}</td><td>${c.deptName || '未知'}</td><td style="text-align:right">${Number(c.totalStudents || 0).toLocaleString()}</td><td style="text-align:right;color:#ef4444">${Number(c.employmentRate || 0).toFixed(1)}%</td><td style="text-align:right;color:#ef4444">-${(rate - Number(c.employmentRate || 0)).toFixed(1)}%</td></tr>`
  ).join('\n')

  // 风险学生名单
  const riskRows = (unemployedStudents.value || []).slice(0, 100).map(s =>
    `            <tr><td>${s.realName || '—'}</td><td>${s.studentNo || '—'}</td><td>${s.deptName || '—'}</td><td>${s.majorName || '—'}</td><td>${s.className || '—'}</td><td><span style="color:${s.riskLevel === '高风险' ? '#ef4444' : '#f59e0b'}">${s.riskLevel || '—'}</span></td><td>${s.riskReason || '—'}</td></tr>`
  ).join('\n')

  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>就业率分析报告 - ${year}</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; font-size: 13px; color: #333; background: #fff; padding: 40px; max-width: 1200px; margin: 0 auto; }
  .cover { text-align: center; padding: 60px 20px; border-bottom: 3px solid #2f6bff; margin-bottom: 40px; }
  .cover h1 { font-size: 28px; color: #1a1a1a; margin-bottom: 12px; }
  .cover .subtitle { font-size: 14px; color: #666; margin-bottom: 20px; }
  .cover .meta { font-size: 12px; color: #999; }
  .section { margin-bottom: 36px; }
  .section-title { font-size: 16px; font-weight: bold; color: #2f6bff; border-left: 4px solid #2f6bff; padding-left: 10px; margin-bottom: 14px; }
  .summary-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
  .summary-card { background: #f8f9ff; border: 1px solid #e0e8ff; border-radius: 8px; padding: 16px; text-align: center; }
  .summary-card .num { font-size: 24px; font-weight: bold; color: #2f6bff; }
  .summary-card .num.red { color: #ef4444; }
  .summary-card .num.orange { color: #f59e0b; }
  .summary-card .label { font-size: 12px; color: #666; margin-top: 4px; }
  table { width: 100%; border-collapse: collapse; margin-bottom: 12px; font-size: 12px; }
  th { background: #2f6bff; color: #fff; padding: 8px 10px; text-align: left; font-weight: normal; }
  td { padding: 7px 10px; border-bottom: 1px solid #eee; }
  tr:nth-child(even) td { background: #f8f9ff; }
  tr:hover td { background: #eef2ff; }
  .warn-box { background: #fff7ed; border: 1px solid #fed7aa; border-radius: 6px; padding: 14px 18px; margin-bottom: 20px; }
  .warn-box p { font-size: 13px; color: #c2410c; line-height: 1.8; }
  .advice-box { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 6px; padding: 14px 18px; }
  .advice-box p { font-size: 13px; color: #166534; line-height: 1.8; }
  .footer { text-align: center; font-size: 11px; color: #aaa; margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; }
  .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
  @media print {
    body { padding: 20px; }
    .no-print { display: none; }
    .summary-grid { grid-template-columns: repeat(4, 1fr); }
    .two-col { grid-template-columns: 1fr 1fr; }
    table { page-break-inside: auto; }
    tr { page-break-inside: avoid; }
  }
</style>
</head>
<body>

<!-- 封面 -->
<div class="cover">
  <h1>高校毕业生就业率分析报告</h1>
  <p class="subtitle">基于真实就业登记数据的统计分析 · 多维度就业预警报告</p>
  <p class="meta">统计届别：${year} &nbsp;|&nbsp; 生成时间：${now}</p>
</div>

<!-- 关键指标 -->
<div class="section">
  <div class="section-title">一、关键指标概览</div>
  <div class="summary-grid">
    <div class="summary-card">
      <div class="num">${Number(overview.value.totalStudents || 0).toLocaleString()}</div>
      <div class="label">毕业生总数</div>
    </div>
    <div class="summary-card">
      <div class="num">${Number(overview.value.employed || 0).toLocaleString()}</div>
      <div class="label">已就业人数</div>
    </div>
    <div class="summary-card">
      <div class="num red">${rate.toFixed(1)}%</div>
      <div class="label">总体就业率</div>
    </div>
    <div class="summary-card">
      <div class="num orange">${unemp.toLocaleString()}</div>
      <div class="label">未就业人数</div>
    </div>
  </div>
  <div class="summary-grid">
    <div class="summary-card">
      <div class="num orange">${highRisk.toLocaleString()}</div>
      <div class="label">高风险学生</div>
    </div>
    <div class="summary-card">
      <div class="num orange">${deptCount}</div>
      <div class="label">低于平均院系</div>
    </div>
    <div class="summary-card">
      <div class="num orange">${majorCount}</div>
      <div class="label">低于平均专业</div>
    </div>
    <div class="summary-card">
      <div class="num red">${classCount}</div>
      <div class="label">重点帮扶班级</div>
    </div>
  </div>
</div>

<!-- 预警提示 -->
<div class="section">
  <div class="section-title">二、预警提示</div>
  <div class="warn-box">
    <p>${deptCount > 0 ? `⚠  ${deptCount} 个院系就业率低于全校平均 ${rate.toFixed(1)}%，建议重点关注。` : '✓ 当前各院系就业率均达标或接近目标。'}</p>
    <p>${majorCount > 0 ? `⚠  ${majorCount} 个专业就业率低于全校平均，需加强专业就业指导。` : ''}</p>
    <p>${classCount > 0 ? `⚠  ${classCount} 个班级就业率低于全校平均 5% 以上，列入重点帮扶名单。` : ''}</p>
    <p>${highRisk > 0 ? `⚠  ${highRisk} 名学生未登记就业信息或审核未通过，存在较高风险，需一对一跟进。` : ''}</p>
  </div>
</div>

<!-- 建议 -->
<div class="section">
  <div class="section-title">三、改进建议</div>
  <div class="advice-box">
    <p>${advice.value.school || '建议近期组织召开全校就业推进会，针对性解决低就业率院系和班级的实际问题。'}</p>
    <p>${advice.value.dept || '建议院级老师重点关注低就业率院系，督促相关班主任制定未就业学生帮扶计划。'}</p>
    <p>${advice.value.teacher || '建议班主任逐一摸排未就业学生情况，了解求职意向，一对一推荐合适岗位。'}</p>
  </div>
</div>

<!-- 就业类型 + 薪资分布 -->
<div class="section">
  <div class="section-title">四、就业类型分布</div>
  <table>
    <thead><tr><th>就业类型</th><th style="text-align:right">人数</th><th style="text-align:right">占比</th></tr></thead>
    <tbody>
${typeRows}
    </tbody>
  </table>
</div>

<div class="section">
  <div class="section-title">五、薪资分布</div>
  <table>
    <thead><tr><th>薪资区间</th><th style="text-align:right">人数</th><th style="text-align:right">占比</th></tr></thead>
    <tbody>
${salRows}
    </tbody>
  </table>
</div>

<!-- 行业分布 -->
<div class="section">
  <div class="section-title">六、行业分布 TOP10</div>
  <table>
    <thead><tr><th>行业</th><th style="text-align:right">人数</th><th style="text-align:right">占比</th></tr></thead>
    <tbody>
${indRows}
    </tbody>
  </table>
</div>

<!-- 院系排名 -->
<div class="section">
  <div class="section-title">七、低于全校平均的院系排名（共 ${deptCount} 个）</div>
  ${deptCount > 0 ? `
  <table>
    <thead><tr><th style="width:40px;text-align:center">#</th><th>院系</th><th style="text-align:right">总人数</th><th style="text-align:right">已就业</th><th style="text-align:right">就业率</th><th style="text-align:right">偏差</th></tr></thead>
    <tbody>
${deptRows}
    </tbody>
  </table>` : '<p style="color:#22c55e">✓ 所有院系就业率均达标</p>'}
</div>

<!-- 专业排名 -->
<div class="section">
  <div class="section-title">八、低于全校平均的专业排名（共 ${majorCount} 个）</div>
  ${majorCount > 0 ? `
  <table>
    <thead><tr><th style="width:40px;text-align:center">#</th><th>专业</th><th>所属院系</th><th style="text-align:right">总人数</th><th style="text-align:right">已就业</th><th style="text-align:right">就业率</th><th style="text-align:right">偏差</th></tr></thead>
    <tbody>
${majorRows}
    </tbody>
  </table>` : '<p style="color:#22c55e">✓ 所有专业就业率均达标</p>'}
</div>

<!-- 班级排名 -->
<div class="section">
  <div class="section-title">九、重点帮扶班级排名（前 50 个，共 ${classCount} 个）</div>
  ${classCount > 0 ? `
  <table>
    <thead><tr><th style="width:40px;text-align:center">#</th><th>班级</th><th>所属院系</th><th style="text-align:right">总人数</th><th style="text-align:right">就业率</th><th style="text-align:right">偏差</th></tr></thead>
    <tbody>
${classRows}
    </tbody>
  </table>` : '<p style="color:#22c55e">✓ 暂无重点帮扶班级</p>'}
</div>

<!-- 高风险学生 -->
<div class="section">
  <div class="section-title">十、高风险未就业学生名单（前 100 名，共 ${highRisk} 名）</div>
  ${highRisk > 0 ? `
  <table>
    <thead><tr><th>姓名</th><th>学号</th><th>学院</th><th>专业</th><th>班级</th><th>风险等级</th><th>风险原因</th></tr></thead>
    <tbody>
${riskRows}
    </tbody>
  </table>` : '<p style="color:#22c55e">✓ 所有学生均已就业，无风险学生</p>'}
</div>

<div class="footer">
  <p>本报告由就业分析系统自动生成 · 数据来源于就业登记真实记录</p>
  <p>生成时间：${now}</p>
</div>

</body>
</html>`

  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const win = window.open(url, '_blank')
  if (win) {
    win.onload = () => {
      win.print()
    }
    ElMessage.success('报告已生成，请在弹出窗口中打印或另存为 PDF')
  } else {
    ElMessage.error('弹出窗口被拦截，请允许弹窗后重试')
  }
}

// ==================== 图表初始化 ====================
function initCharts() {
  if (typeChartRef.value) typeChart = echarts.init(typeChartRef.value)
  if (salaryChartRef.value) salaryChart = echarts.init(salaryChartRef.value)
  if (industryChartRef.value) industryChart = echarts.init(industryChartRef.value)
  if (mapChartRef.value) mapChart = echarts.init(mapChartRef.value)
  if (deptChartRef.value) deptChart = echarts.init(deptChartRef.value)
  if (classChartRef.value) classChart = echarts.init(classChartRef.value)
  if (majorChartRef.value) majorChart = echarts.init(majorChartRef.value)
}

function destroyCharts() {
  typeChart?.dispose()
  salaryChart?.dispose()
  industryChart?.dispose()
  mapChart?.dispose()
  deptChart?.dispose()
  classChart?.dispose()
  majorChart?.dispose()
  majorDetailChart?.dispose()
  typeChart = salaryChart = industryChart = mapChart = deptChart = classChart = majorChart = majorDetailChart = null
}

// ==================== 图表渲染 ====================
function renderTypeChart() {
  if (!typeChart) return
  const typeCount = trend.value.typeCount || {}
  const entries = Object.entries(typeCount).sort((a, b) => b[1] - a[1])
  typeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 12 } },
    color: COLOR_PALETTE,
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: entries.length <= 6, fontSize: 12 },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: entries.map(([name, value]) => ({ name, value: Number(value) }))
    }]
  }, true)
}

function renderSalaryChart() {
  if (!salaryChart) return
  const dist = salary.value.distribution || {}
  const labels = ['5k以下', '5k-8k', '8k-12k', '12k-20k', '20k以上']
  const values = labels.map(l => Number(dist[l]) || 0)
  salaryChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 }, name: '薪资范围' },
    yAxis: { type: 'value', name: '人数', axisLabel: { fontSize: 11 } },
    color: ['#2f6bff'],
    series: [{
      type: 'bar',
      data: values,
      barMaxWidth: 40,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', fontSize: 11 }
    }]
  }, true)
}

function renderIndustryChart() {
  if (!industryChart) return
  const dist = industry.value.distribution || []
  const top5 = Array.isArray(dist) ? dist.slice(0, 5) : []
  if (top5.length === 0) return
  industryChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value', name: '人数', axisLabel: { fontSize: 11 } },
    yAxis: { type: 'category', data: top5.map(d => d.name || d).reverse(), axisLabel: { fontSize: 11 } },
    color: ['#22c55e'],
    series: [{
      type: 'bar',
      data: top5.map(d => Number(d.value) || 0).reverse(),
      barMaxWidth: 30,
      itemStyle: { borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', fontSize: 11 }
    }]
  }, true)
}

function renderDeptComparisonChart() {
  if (!deptChart || !deptStats.value.length) return
  const sorted = [...deptStats.value].sort((a, b) => Number(a.employmentRate) - Number(b.employmentRate))
  deptChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'category', data: sorted.map(d => d.deptName || '未知').reverse(), axisLabel: { fontSize: 11, rotate: 30 }, name: '院系' },
    yAxis: { type: 'value', name: '就业率%', max: 100, axisLabel: { fontSize: 11 } },
    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: '#2f6bff' },
      { offset: 1, color: '#74add1' }
    ]),
    series: [{
      type: 'bar',
      data: sorted.map(d => Number(d.employmentRate) || 0).reverse(),
      barMaxWidth: 30,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', fontSize: 11, formatter: '{c}%' }
    }]
  }, true)
}

function renderClassTop10Chart() {
  if (!classChart || !classStats.value.length) return
  const top10 = [...classStats.value]
    .sort((a, b) => Number(a.employmentRate) - Number(b.employmentRate))
    .slice(0, 10)
  classChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value', name: '就业率%', max: 100, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'category', data: top10.map(c => c.className || '未知').reverse(), axisLabel: { fontSize: 11 } },
    color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
      { offset: 0, color: '#ef4444' },
      { offset: 1, color: '#f87171' }
    ]),
    series: [{
      type: 'bar',
      data: top10.map(c => Number(c.employmentRate) || 0).reverse(),
      barMaxWidth: 24,
      itemStyle: { borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', fontSize: 11, formatter: '{c}%' }
    }]
  }, true)
}

function renderMajorChart() {
  if (!majorChart || !majorStats.value.length) return
  const sorted = [...majorStats.value].sort((a, b) => Number(a.employmentRate) - Number(b.employmentRate))
  majorChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'category', data: sorted.map(m => m.majorName || '未知').reverse(), axisLabel: { fontSize: 11, rotate: 30 }, name: '专业' },
    yAxis: { type: 'value', name: '就业率%', max: 100, axisLabel: { fontSize: 11 } },
    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: '#22c55e' },
      { offset: 1, color: '#4ade80' }
    ]),
    series: [{
      type: 'bar',
      data: sorted.map(m => Number(m.employmentRate) || 0).reverse(),
      barMaxWidth: 30,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', fontSize: 11, formatter: '{c}%' }
    }]
  }, true)
}

function renderRegionChart() {
  if (!mapChart) return
  const dist = province.value.distribution || []
  const allMapData = (Array.isArray(dist) ? dist : [])
    .map(item => ({
      name: normalizeProvince(item.name || item),
      value: Number(item.value || 0)
    }))
    .filter(d => d.name !== null)

  if (allMapData.length === 0) return

  fetch('/china.json')
    .then(res => res.json())
    .then(chinaJson => {
      echarts.registerMap('china', chinaJson)
      const maxVal = Math.max(...allMapData.map(d => d.value), 1)
      mapChart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: (params) => {
            const value = params.data?.value ?? 0
            return `${params.name}<br/>就业人数: <b>${value}</b> 人`
          }
        },
        visualMap: {
          min: 0,
          max: maxVal,
          text: ['高', '低'],
          realtime: false,
          calculable: true,
          inRange: { color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#f46d43', '#d73027', '#a50026'] },
          textStyle: { fontSize: 12 },
          left: 16,
          bottom: 16
        },
        series: [{
          name: '就业人数',
          type: 'map',
          map: 'china',
          roam: true,
          roamDelay: 100,
          zoom: 1.2,
          scaleLimit: { min: 0.8, max: 3 },
          layoutCenter: ['50%', '52%'],
          layoutSize: '118%',
          aspectScale: 0.85,
          label: { show: false, fontSize: 10 },
          emphasis: {
            label: { show: true, fontSize: 11, color: '#333' },
            itemStyle: { areaColor: '#ffa500' }
          },
          itemStyle: {
            areaColor: '#f0f0f0',
            borderColor: '#b0b0b0',
            borderWidth: 0.5
          },
          data: allMapData
        }]
      }, true)
    })
    .catch(e => console.warn('地图加载失败:', e))
}

// ==================== 数据加载 ====================
async function loadYears() {
  try {
    const res = await analyticsApi.getAvailableYears()
    years.value = Array.isArray(res) ? res : []
  } catch {
    years.value = []
  }
}

async function loadAll() {
  loading.value = true
  const params = selectedYear.value ? { graduationYear: selectedYear.value } : {}
  try {
    const [ovRes, trendRes, salRes, indRes, provRes, deptRes, clsRes, unempRes, majorRes] = await Promise.all([
      analyticsApi.getOverview(params),
      analyticsApi.getEmploymentTrend(params),
      analyticsApi.getSalaryDist(params),
      analyticsApi.getIndustryDist(params),
      analyticsApi.getProvinceDist(params),
      analyticsApi.getDeptStats(params),
      analyticsApi.getClassStats(params),
      analyticsApi.getUnemployedStudents(params),
      analyticsApi.getMajorStats(params)
    ])

    overview.value = ovRes || {}
    trend.value = trendRes || {}
    salary.value = salRes || {}
    industry.value = indRes || {}
    province.value = provRes || {}
    deptStats.value = Array.isArray(deptRes) ? deptRes : []
    classStats.value = Array.isArray(clsRes) ? clsRes : []
    unemployedStudentsRes.value = unempRes || { list: [], total: 0 }
    majorStats.value = Array.isArray(majorRes) ? majorRes : []

    loading.value = false
    await nextTick()
    await nextTick()

    if (!typeChart) initCharts()

    typeChart?.resize()
    salaryChart?.resize()
    industryChart?.resize()
    mapChart?.resize()
    deptChart?.resize()
    classChart?.resize()
    majorChart?.resize()

    renderTypeChart()
    renderSalaryChart()
    renderIndustryChart()
    renderRegionChart()
    renderDeptComparisonChart()
    renderClassTop10Chart()
    renderMajorChart()

    await nextTick()
    typeChart?.resize()
    salaryChart?.resize()
    industryChart?.resize()
    mapChart?.resize()
    deptChart?.resize()
    classChart?.resize()
    majorChart?.resize()
  } catch (e) {
    loading.value = false
    console.error('加载数据失败', e)
  }
}

function handleTabChange() {
  nextTick(() => {
    typeChart?.resize()
    salaryChart?.resize()
    industryChart?.resize()
    mapChart?.resize()
    deptChart?.resize()
    classChart?.resize()
    majorChart?.resize()
  })
}

function handleAuxTabChange() {
  nextTick(() => {
    industryChart?.resize()
    deptChart?.resize()
    classChart?.resize()
    majorChart?.resize()
  })
}

async function notifyAll() {
  const students = unemployedStudents.value
  if (students.length === 0) {
    ElMessage.warning('无未就业学生可通知')
    return
  }

  try {
    // 调用后端批量通知接口，数据分析员向所有未就业学生对应的班主任发送通知
    const res = await request.post('/teacher/employment-reminder/batch-notify', {
      graduationYear: selectedYear.value || null
    })
    const result = res?.data || res
    const sentCount = result?.sentCount || 0
    const details = result?.details || []
    if (sentCount > 0) {
      const classNames = details.slice(0, 3).map(d => d.className).join('、')
      const more = details.length > 3 ? `等${details.length}个班级` : ''
      ElMessage.success(`已向 ${details.length} 个班级的班主任发送帮扶提醒（${classNames}${more}），请班主任重点跟进帮扶`)
    } else {
      ElMessage.info('暂无需要通知的班级')
    }
  } catch (e) {
    console.error('通知失败', e)
    ElMessage.error('通知失败：' + (e.message || '请重试'))
  }
}

function exportStudentList() {
  const students = unemployedStudents.value
  if (students.length === 0) {
    ElMessage.warning('无未就业学生可导出')
    return
  }
  const header = '姓名,学号,学院,专业,班级,风险等级,风险原因\n'
  const rows = students.map(s =>
    `${s.realName || ''},${s.studentNo || ''},${s.deptName || ''},${s.majorName || ''},${s.className || ''},${s.riskLevel || ''},${s.riskReason || ''}`
  ).join('\n')
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + header + rows], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `未就业学生名单_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出 ${students.length} 名未就业学生`)
}

// ==================== 弹窗功能 ====================

// 打开院系/班级下的学生列表
function openTargetStudents(type, item) {
  const all = unemployedStudents.value
  let list = []
  if (type === 'dept') {
    targetDialogTitle.value = `${item.deptName} - 未就业学生`
    list = all.filter(s => s.deptName === item.deptName)
  } else if (type === 'class') {
    targetDialogTitle.value = `${item.className} - 未就业学生`
    list = all.filter(s => s.className === item.className)
  } else if (type === 'major') {
    targetDialogTitle.value = `${item.majorName} - 未就业学生`
    list = all.filter(s => s.majorName === item.majorName)
  }
  targetStudents.value = list
  targetDialogVisible.value = true
}

// 打开学生详细信息
function openStudentDetail(student) {
  studentDetail.value = { ...student }
  studentDetailVisible.value = true
}

// 生成学生帮扶建议
function getStudentSuggestion(student) {
  if (!student) return ''
  if (student.riskLevel === '高风险') {
    return '该生未登记任何就业信息或审核未通过，属于高风险。建议班主任立即联系学生，了解求职意向，协助完善简历，一对一推荐岗位，必要时安排就业辅导。'
  } else if (student.riskLevel === '中风险') {
    return '该生就业登记待审核，建议跟进审核进度，如审核通过则解除风险；如审核驳回，请督促学生重新提交准确信息。'
  } else if (student.employmentType === '继续深造') {
    return '该生选择继续深造，建议持续关注录取结果，确保顺利完成学业去向登记。'
  } else if (student.employmentType === '自主创业') {
    return '该生选择自主创业，建议协助了解创业扶持政策，提供创业指导服务，确保创业信息真实有效。'
  } else if (student.employmentType === '应征入伍') {
    return '该生应征入伍，建议持续跟踪入伍状态，确保顺利完成去向登记。'
  } else if (student.employmentType === '出国出境') {
    return '该生选择出国出境，建议协助了解境外就业或留学手续，提供必要支持。'
  }
  return '建议班主任持续关注该生情况，定期跟进并及时更新就业去向。'
}

// 打开专业详情
function openMajorDetail(major) {
  majorDetail.value = { ...major }
  majorDetailVisible.value = true
  nextTick(() => {
    if (majorDetailChartRef.value && !majorDetailChart) {
      majorDetailChart = echarts.init(majorDetailChartRef.value)
    }
    renderMajorDetailChart()
  })
}

function renderMajorDetailChart() {
  if (!majorDetailChart || !majorDetail.value) return
  const m = majorDetail.value
  const employed = m.employed || 0
  const unemployed = m.unemployed || 0
  const belowAvg = m.belowAvgCount || 0
  const other = Math.max(0, (m.totalStudents || 0) - employed - unemployed)
  majorDetailChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    color: ['#22c55e', '#ef4444', '#f59e0b', '#d1d5db'],
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      label: { show: true, fontSize: 12 },
      data: [
        { name: '已就业', value: employed },
        { name: '未就业', value: unemployed },
        { name: '低于平均', value: belowAvg },
        { name: '其他', value: other }
      ].filter(d => d.value > 0)
    }]
  }, true)
}

onMounted(() => {
  loadYears()
  loadAll()
  resizeObserver = new ResizeObserver(() => {
    typeChart?.resize()
    salaryChart?.resize()
    industryChart?.resize()
    mapChart?.resize()
    deptChart?.resize()
    classChart?.resize()
    majorChart?.resize()
  })
  if (mapChartRef.value) resizeObserver.observe(mapChartRef.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  destroyCharts()
})
</script>

<style scoped>
/* ========== 页面骨架 ========== */
.page-wrap {
  padding: 24px;
  background: #f0f5ff;
  min-height: 100vh;
}

.page-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== 标题区 ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
  padding-bottom: 14px;
  border-bottom: 1px solid #dde6f5;
}
.page-title {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: #0f2a5f;
}
.page-subtitle {
  margin: 0;
  font-size: 13px;
  color: #5f6f8f;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ========== 通用卡片 ========== */
.card {
  background: #fff;
  border: 1px solid #e2e8f6;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(47, 107, 255, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  border-bottom: 1px solid #edf2f8;
  background: #fafcff;
  flex-shrink: 0;
  min-height: 46px;
}
.card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0f2a5f;
}

/* ========== 加载状态 ========== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 12px;
  color: #909399;
}

/* ========== 核心指标卡片 ========== */
.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}
.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e2e8f6;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(47, 107, 255, 0.04);
  height: 96px;
  box-sizing: border-box;
  transition: transform 0.2s, box-shadow 0.2s;
}
.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(47, 107, 255, 0.12);
}
.metric-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  flex-shrink: 0;
}
.metric-card--green .metric-card__icon  { background: linear-gradient(135deg, #22c55e, #4ade80); }
.metric-card--orange .metric-card__icon { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
.metric-card--red .metric-card__icon    { background: linear-gradient(135deg, #ef4444, #f87171); }
.metric-card--blue .metric-card__icon    { background: linear-gradient(135deg, #2f6bff, #5a9fff); }

.metric-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.metric-card__label {
  font-size: 12px;
  color: #5f6f8f;
  margin-bottom: 2px;
}
.metric-card__value {
  font-size: 26px;
  font-weight: 700;
  color: #0f2a5f;
  line-height: 1.2;
}
.metric-card__unit { font-size: 14px; font-weight: 400; }
.metric-card__sub {
  font-size: 11px;
  color: #909399;
  margin: 2px 0 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ========== 板块标题 ========== */
.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #0f2a5f;
}

/* ========== 预警看板 ========== */
.alert-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}
.alert-card {
  background: #fff;
  border: 1px solid #e2e8f6;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(47, 107, 255, 0.04);
  display: flex;
  flex-direction: column;
  max-height: 180px;
}
.alert-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fafcff;
  border-bottom: 1px solid #f0f2f5;
  font-size: 14px;
  font-weight: 600;
  color: #0f2a5f;
  flex-shrink: 0;
}
.alert-card__count {
  margin-left: auto;
  background: #e8edf7;
  color: #5f6f8f;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}
.alert-card--warn .alert-card__header  { background: #fffbeb; border-left: 3px solid #f59e0b; }
.alert-card--danger .alert-card__header { background: #fef2f2; border-left: 3px solid #ef4444; }
.alert-card--ok .alert-card__header    { background: #f0fdf4; border-left: 3px solid #22c55e; }

.alert-card__body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.alert-card__body--ok {
  justify-content: center;
}
.alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.alert-item__name {
  flex: 1;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.alert-item__rate {
  font-weight: 600;
  color: #ef4444;
  font-size: 13px;
}
.alert-desc {
  font-size: 13px;
  color: #5f6f8f;
  line-height: 1.5;
  margin: 0;
}
.alert-card__more {
  font-size: 12px;
  color: #909399;
  margin: 0;
  text-align: center;
}
.alert-card__action {
  padding: 8px 16px;
  border-top: 1px solid #f0f2f5;
  flex-shrink: 0;
}
.ok-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #22c55e;
}

/* ========== 两列网格 ========== */
.two-col-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

/* ========== 排名 Tabs ========== */
.rank-tabs { margin: 0; }
.rank-tabs :deep(.el-tabs__header) { margin: 0; }

/* ========== 排名列表（内部滚动） ========== */
.scroll-list {
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}
.rank-header {
  display: grid;
  grid-template-columns: 40px 1fr 70px 70px 80px 60px 90px;
  gap: 8px;
  padding: 8px 16px;
  background: #f8faff;
  border-bottom: 1px solid #edf2f8;
  font-size: 12px;
  color: #909399;
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 1;
}
.rank-row {
  display: grid;
  grid-template-columns: 40px 1fr 70px 70px 80px 60px 90px;
  gap: 8px;
  align-items: center;
  padding: 9px 16px;
  border-bottom: 1px solid #f0f2f5;
  font-size: 13px;
  color: #5f6f8f;
  transition: background 0.2s;
}
.rank-row:last-child { border-bottom: none; }
.rank-row:hover { background: #f5f9ff; }
.rank-num {
  font-weight: 600;
  color: #909399;
  text-align: center;
}
.rank-num--danger { color: #ef4444; }
.rank-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
  font-weight: 500;
}
.rate-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}
.rate-badge--red    { background: #fef2f2; color: #ef4444; }
.rate-badge--orange { background: #fffbeb; color: #f59e0b; }
.rate-badge--yellow { background: #f0fdf4; color: #22c55e; }
.scroll-more {
  text-align: center;
  font-size: 12px;
  color: #909399;
  padding: 10px;
  margin: 0;
}

/* ========== 反馈建议 ========== */
.advice-list {
  display: flex;
  flex-direction: column;
}
.advice-card {
  padding: 14px 18px;
  border-bottom: 1px solid #f0f2f5;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.advice-card:last-child { border-bottom: none; }
.advice-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #0f2a5f;
}
.advice-card__body {
  font-size: 13px;
  color: #5f6f8f;
  line-height: 1.7;
}
.advice-card__body--truncated {
  max-height: 66px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
}
.advice-text { margin: 0; }
.advice-placeholder {
  color: #c0c4cc !important;
  font-style: italic;
  margin: 0;
}
.advice-card__footer {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* ========== 图表容器（固定高度） ========== */
.chart-container {
  width: 100%;
  height: 300px;
  padding: 8px 14px 4px;
  box-sizing: border-box;
}

/* ========== 辅助分析 Tabs ========== */
.aux-tabs {
  padding: 0;
}
.aux-tabs__inner {
  padding: 0 18px;
}
.aux-content {
  padding: 0 18px 14px;
}

/* ========== 地图模块（全宽大卡片） ========== */
.card--map { overflow: visible; }
.map-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 0;
  min-height: 440px;
}
.map-main {
  padding: 14px;
  display: flex;
  align-items: stretch;
}
.map-container {
  width: 100%;
  height: 420px;
  min-height: 420px;
  background: linear-gradient(135deg, #f0f5ff, #e8f0fe);
  border-radius: 10px;
  border: 1px solid #e2e8f6;
}
.map-empty {
  width: 100%;
  height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: linear-gradient(135deg, #f0f5ff, #e8f0fe);
  border-radius: 10px;
  color: #909399;
  font-size: 14px;
}

/* 地图右侧 TOP5 */
.map-aside {
  border-left: 1px solid #edf2f8;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.map-aside__title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #0f2a5f;
  padding-bottom: 8px;
  border-bottom: 1px solid #edf2f8;
}
.map-aside__list {
  flex: 1;
  overflow-y: auto;
}
.map-aside__item {
  display: grid;
  grid-template-columns: 22px 1fr 70px;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #edf2f8;
}
.map-aside__item:last-child { border-bottom: none; }
.map-aside__rank {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e2e8f6;
  color: #909399;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.map-aside__rank--top {
  background: linear-gradient(135deg, #2f6bff, #5a9fff);
  color: #fff;
}
.map-aside__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.map-aside__name {
  font-size: 12px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.map-aside__bar-wrap {
  height: 4px;
  background: #edf2f8;
  border-radius: 4px;
  overflow: hidden;
}
.map-aside__bar {
  height: 100%;
  background: linear-gradient(90deg, #2f6bff, #74add1);
  border-radius: 4px;
  transition: width 0.5s ease;
}
.map-aside__nums {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.map-aside__value {
  font-size: 13px;
  font-weight: 700;
  color: #0f2a5f;
}
.map-aside__pct {
  font-size: 11px;
  color: #909399;
}

/* ========== 重点帮扶学生名单 ========== */
.card--student { overflow: visible; }
.panel-actions { display: flex; gap: 8px; }
.student-table-wrap {
  overflow: hidden;
}
.student-table-inner {
  overflow-y: auto;
  max-height: 380px;
}
.student-table-hd,
.student-row {
  display: grid;
  grid-template-columns: 80px 100px minmax(80px, 1fr) minmax(80px, 1fr) minmax(80px, 1fr) 90px 110px 90px;
  gap: 8px;
  align-items: center;
  padding: 10px 18px;
  font-size: 13px;
}
.student-table-hd {
  background: #f8faff;
  border-bottom: 1px solid #edf2f8;
  color: #909399;
  font-size: 12px;
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 1;
}
.student-row {
  border-bottom: 1px solid #f0f2f5;
  color: #5f6f8f;
  transition: background 0.2s;
}
.student-row:hover { background: #f5f9ff; }
.cell-name {
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cell-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cell-risk {
  color: #f59e0b;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.student-more {
  text-align: center;
  padding: 10px;
  border-top: 1px solid #edf2f8;
}

/* ========== 分析结论 ========== */
.analysis-conclusion {
  display: flex;
  gap: 10px;
  padding: 10px 16px;
  background: #f5f9ff;
  border-top: 1px solid #e2e8f6;
  font-size: 12px;
  line-height: 1.6;
  color: #5f6f8f;
}
.analysis-conclusion > .el-icon {
  color: #2f6bff;
  flex-shrink: 0;
  margin-top: 2px;
}
.analysis-conclusion__text { flex: 1; }

/* ========== 文本截断 / 展开 ========== */
.analysis-summary {
  display: inline;
  vertical-align: middle;
}
.analysis-summary:not(.expanded) {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ========== 滚动条 ========== */
.scroll-list::-webkit-scrollbar { width: 5px; height: 5px; }
.scroll-list::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 3px; }
.scroll-list::-webkit-scrollbar-track { background: transparent; }

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 20px;
  color: #909399;
  font-size: 13px;
}

/* ========== 响应式 ========== */
@media (max-width: 1400px) {
  .metric-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .alert-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .two-col-grid { grid-template-columns: 1fr; }
  .map-layout { grid-template-columns: 1fr; }
  .map-aside { border-left: none; border-top: 1px solid #edf2f8; }
}

@media (max-width: 992px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 768px) {
  .page-wrap { padding: 16px; }
  .page-header { flex-direction: column; gap: 12px; }
  .metric-grid { grid-template-columns: 1fr; }
  .alert-grid { grid-template-columns: 1fr; }
  .student-table-hd,
  .student-row {
    grid-template-columns: 80px 1fr 80px 90px;
    padding: 8px 12px;
  }
  .student-table-hd span:nth-child(3),
  .student-table-hd span:nth-child(5),
  .student-table-hd span:nth-child(7),
  .student-row span:nth-child(3),
  .student-row span:nth-child(5),
  .student-row span:nth-child(7) { display: none; }
  .map-container { height: 280px; min-height: 280px; }
}

/* ========== 弹窗样式 ========== */
.dialog-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

/* 学生详情 */
.student-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.detail-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.detail-section__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #0f2a5f;
  padding-bottom: 8px;
  border-bottom: 1px solid #edf2f8;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-item.full-width {
  grid-column: 1 / -1;
}
.detail-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}
.detail-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.risk-suggestion {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #f5f9ff;
  border-radius: 8px;
  border: 1px solid #e2e8f6;
  font-size: 13px;
  line-height: 1.6;
  color: #5f6f8f;
}
.risk-suggestion .el-icon {
  color: #2f6bff;
  flex-shrink: 0;
  margin-top: 2px;
}
.no-record {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  background: #fef9ec;
  border-radius: 8px;
  border: 1px solid #f5d78a;
  font-size: 13px;
  color: #a07800;
}
.no-record .el-icon { color: #f59e0b; }

/* 专业详情 */
.major-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.major-summary {
  display: flex;
  gap: 16px;
  padding: 14px;
  background: #f5f9ff;
  border-radius: 10px;
  border: 1px solid #e2e8f6;
  flex-wrap: wrap;
}
.major-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 80px;
}
.major-stat__value {
  font-size: 22px;
  font-weight: 700;
  color: #0f2a5f;
}
.major-stat__label {
  font-size: 11px;
  color: #909399;
}
.major-chart-wrap {
  padding: 0 4px;
}
</style>
