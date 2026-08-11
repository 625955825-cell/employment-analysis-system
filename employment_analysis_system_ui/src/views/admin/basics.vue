<template>
  <div class="page-container">
    <h2>基础数据管理</h2>

    <el-card class="init-card" shadow="never">
      <div class="init-info">
        <span class="init-label">当前数据：</span>
        <el-tag type="info" style="margin-right: 8px;">院系 {{ initStatus.deptCount }} 个</el-tag>
        <el-tag type="info" style="margin-right: 8px;">专业 {{ initStatus.majorCount }} 个</el-tag>
        <el-tag type="info" style="margin-right: 8px;">班级 {{ initStatus.classCount }} 个</el-tag>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" style="margin-top: 16px;">
      <el-tab-pane label="院系管理" name="dept">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>院系列表</span>
              <div style="display:flex;gap:8px;align-items:center;">
                <el-dropdown trigger="click" @command="cmd => exportDepts(cmd)">
                  <el-button type="success" size="small" plain>
                    导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="page">导出当前页</el-dropdown-item>
                      <el-dropdown-item command="all">导出全部数据</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button type="primary" size="small" @click="openDeptDialog()">添加院系</el-button>
              </div>
            </div>
          </template>
          <el-table :data="deptList" stripe>
            <el-table-column prop="deptName" label="院系名称" />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                  {{ row.status === '0' ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openDeptDialog(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="deleteDept(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="专业管理" name="major">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>专业列表</span>
              <div style="display:flex;gap:8px;align-items:center;">
                <el-dropdown trigger="click" @command="cmd => exportMajors(cmd)">
                  <el-button type="success" size="small" plain>
                    导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="page">导出当前页</el-dropdown-item>
                      <el-dropdown-item command="all">导出全部数据</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button type="primary" size="small" @click="openMajorDialog()">添加专业</el-button>
              </div>
            </div>
          </template>
          <el-table :data="majorList" stripe>
            <el-table-column prop="majorName" label="专业名称" />
            <el-table-column prop="shortName" label="专业简称" width="120" />
            <el-table-column prop="deptName" label="所属院系" width="180" />
            <el-table-column prop="isTopLevel" label="一流专业" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isTopLevel === '1'" type="success" size="small">一流</el-tag>
                <span v-else style="color: #c0c4cc;">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="recommendEnabled" label="推荐算法" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.recommendEnabled === '1'"
                  active-text="开"
                  inactive-text="关"
                  inline-prompt
                  @change="toggleMajorRecommend(row)"
                  style="--el-switch-on-color: #67c23a; --el-switch-off-color: #909399;"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openMajorDialog(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="deleteMajor(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="班级管理" name="cls">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>班级列表</span>
              <div class="filter-row">
                <el-input
                  v-model="classKeyword"
                  placeholder="搜索班级/专业名称"
                  clearable
                  style="width: 160px; margin-right: 8px;"
                  @input="onClassKeywordSearch"
                />
                <el-select v-model="classFilterDeptId" placeholder="筛选院系" clearable style="width: 160px; margin-right: 8px;" @change="onClassFilterDeptChange">
                  <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
                </el-select>
                <el-select v-model="classFilterMajorId" placeholder="筛选专业" clearable :disabled="!classFilterDeptId" style="width: 180px; margin-right: 8px;" @change="onClassFilterMajorChange">
                  <el-option v-for="m in classFilterMajors" :key="m.id" :label="m.majorName" :value="m.id" />
                </el-select>
                <el-button type="primary" size="small" @click="openClassDialog()">添加班级</el-button>
                <el-dropdown trigger="click" @command="cmd => exportClasses(cmd)">
                  <el-button type="success" size="small" plain>
                    导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="page">导出当前页</el-dropdown-item>
                      <el-dropdown-item command="all">导出全部数据</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button type="success" size="small" @click="openBatchGenerateDialog">批量生成</el-button>
                <el-button type="danger" size="small" @click="openBatchDeleteDialog">批量删除</el-button>
              </div>
            </div>
          </template>
          <el-table :data="classList" stripe :span-method="classSpanMethod">
            <el-table-column prop="className" label="班级名称" />
            <el-table-column prop="majorName" label="所属专业" width="150" />
            <el-table-column prop="deptName" label="所属院系" width="150" />
            <el-table-column prop="grade" label="年级" width="80" />
            <el-table-column prop="advisor" label="班主任" width="100" />
            <el-table-column prop="studentCount" label="学生数" width="80" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openClassDialog(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="deleteClass(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              :current-page="classPage"
              :page-size="classPageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="classTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="onClassSizeChange"
              @current-change="onClassPageChange"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="注册码管理" name="invite">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>注册码列表</span>
              <div style="display: flex; gap: 8px; align-items: center;">
                <el-input-number v-model="inviteCount" :min="1" :max="20" size="small" style="width: 100px;" />
                <el-button type="primary" size="small" @click="handleBatchGenerate">批量生成</el-button>
                <el-button
                  type="danger"
                  size="small"
                  :disabled="selectedInviteIds.length === 0"
                  @click="handleBatchDelete"
                >
                  批量删除{{ selectedInviteIds.length > 0 ? ` (${selectedInviteIds.length})` : '' }}
                </el-button>
              </div>
            </div>
          </template>
          <el-table :data="inviteList" stripe @selection-change="onInviteSelectionChange">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="code" label="注册码" width="150">
              <template #default="{ row }">
                <span style="font-family: monospace; font-weight: bold; letter-spacing: 2px; color: #409eff;">{{ row.code }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="roleKey" label="注册角色" width="100" align="center">
              <template #default="{ row }">
                {{ row.roleKey || '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="usedUsername" label="使用者" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.usedUsername" type="info" size="small">{{ row.usedUsername }}</el-tag>
                <span v-else style="color: #67c23a;">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="usedTime" label="使用时间" width="170">
              <template #default="{ row }">
                {{ row.usedTime ? row.usedTime.replace('T', ' ').slice(0, 16) : '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="生成时间" width="170">
              <template #default="{ row }">
                {{ row.createTime ? row.createTime.replace('T', ' ').slice(0, 16) : '' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="danger" link size="small" @click="deleteInvite(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="企业管理" name="company">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="filter-row">
                <el-input v-model="companyKeyword" placeholder="搜索企业名称/联系人" clearable style="width: 180px; margin-right: 8px;" @input="loadCompanies" />
                <el-select v-model="companyFilterStatus" placeholder="认证状态" clearable style="width: 120px; margin-right: 8px;" @change="loadCompanies">
                  <el-option label="待审核" value="pending" />
                  <el-option label="已通过" value="approved" />
                  <el-option label="已驳回" value="rejected" />
                </el-select>
              </div>
              <div style="display:flex;gap:8px;align-items:center;">
                <el-dropdown trigger="click" @command="cmd => exportCompanies(cmd)">
                  <el-button type="success" size="small" plain>
                    导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="page">导出当前页</el-dropdown-item>
                      <el-dropdown-item command="all">导出全部数据</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button type="primary" size="small" @click="openCompanyDialog()">添加企业</el-button>
              </div>
            </div>
          </template>
          <el-table :data="companyList" stripe v-loading="companyLoading">
            <el-table-column prop="companyName" label="企业名称" min-width="160">
              <template #default="{ row }"><span style="font-weight:600;">{{ row.companyName }}</span></template>
            </el-table-column>
            <el-table-column prop="contactPerson" label="联系人" width="100" align="center" />
            <el-table-column prop="contactPhone" label="联系电话" width="130" align="center" />
            <el-table-column prop="industry" label="行业" width="110" align="center" />
            <el-table-column prop="scale" label="规模" width="110" align="center" />
            <el-table-column prop="city" label="城市" width="90" align="center" />
            <el-table-column prop="authStatus" label="认证状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="companyStatusTagType(row.authStatus)" size="small">{{ companyStatusText(row.authStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openCompanyDialog(row)">编辑</el-button>
                <el-button v-if="row.authStatus === 'pending'" type="success" link size="small" @click="companyAudit(row, 'approve')">通过</el-button>
                <el-button v-if="row.authStatus === 'pending'" type="danger" link size="small" @click="openCompanyReject(row)">驳回</el-button>
                <el-button type="danger" link size="small" @click="companyDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              :current-page="companyPage" :page-size="companyPageSize" :page-sizes="[10, 20, 50]" :total="companyTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="v => { companyPageSize = v; companyPage = 1; loadCompanies() }"
              @current-change="v => { companyPage = v; loadCompanies() }"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="职位管理" name="job">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="filter-row">
                <el-input v-model="jobKeyword" placeholder="搜索职位/企业名称" clearable style="width: 180px; margin-right: 8px;" @input="loadJobs" />
                <el-select v-model="jobFilterStatus" placeholder="状态" clearable style="width: 120px; margin-right: 8px;" @change="loadJobs">
                  <el-option label="已发布" value="published" />
                  <el-option label="已下架" value="paused" />
                </el-select>
              </div>
              <div style="display:flex;gap:8px;align-items:center;">
                <el-dropdown trigger="click" @command="cmd => exportJobs(cmd)">
                  <el-button type="success" size="small" plain>
                    导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="page">导出当前页</el-dropdown-item>
                      <el-dropdown-item command="all">导出全部数据</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button type="primary" size="small" @click="openJobDialog()">添加职位</el-button>
              </div>
            </div>
          </template>
          <el-table :data="jobList" stripe v-loading="jobLoading">
            <el-table-column prop="jobName" label="职位名称" min-width="140">
              <template #default="{ row }">
                <div style="font-weight:600;">{{ row.jobName }}</div>
                <div style="font-size:12px; color:#f56c6c;">{{ row.salaryText }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="companyName" label="所属企业" min-width="150" />
            <el-table-column prop="companyIndustry" label="行业" width="100" align="center" />
            <el-table-column prop="workCity" label="城市" width="80" align="center" />
            <el-table-column prop="recruitNumber" label="人数" width="70" align="center">
              <template #default="{ row }"><el-tag type="info" size="small">{{ row.recruitNumber || '若干' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="educationRequired" label="学历" width="90" align="center" />
            <el-table-column prop="viewCount" label="浏览" width="60" align="center" />
            <el-table-column prop="applyCount" label="投递" width="60" align="center" />
            <el-table-column prop="status" label="状态" width="85" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'published' ? 'success' : 'info'" size="small">{{ row.status === 'published' ? '已发布' : '已下架' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openJobDialog(row)">编辑</el-button>
                <el-button v-if="row.status === 'paused'" type="success" link size="small" @click="jobPublish(row)">上架</el-button>
                <el-button v-if="row.status === 'published'" type="warning" link size="small" @click="jobPause(row)">下架</el-button>
                <el-button type="danger" link size="small" @click="jobDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              :current-page="jobPage" :page-size="jobPageSize" :page-sizes="[10, 20, 50]" :total="jobTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="v => { jobPageSize = v; jobPage = 1; loadJobs() }"
              @current-change="v => { jobPage = v; loadJobs() }"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="deptDialogVisible" :title="isDeptEdit ? '编辑院系' : '添加院系'" width="500px" destroy-on-close>
      <el-form :model="deptForm" label-width="90px">
        <el-form-item label="院系名称" required>
          <el-input v-model="deptForm.deptName" placeholder="请输入院系名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDept" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="majorDialogVisible" :title="isMajorEdit ? '编辑专业' : '添加专业'" width="500px" destroy-on-close>
      <el-form :model="majorForm" label-width="90px">
        <el-form-item label="专业名称" required>
          <el-input v-model="majorForm.majorName" placeholder="请输入专业名称" />
        </el-form-item>
        <el-form-item label="所属院系" required>
          <el-select v-model="majorForm.deptId" placeholder="请选择院系" style="width: 100%;">
            <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业简称">
          <el-input v-model="majorForm.shortName" placeholder="用于班级名生成，如：机械、电气" />
        </el-form-item>
        <el-form-item label="一流专业">
          <el-switch v-model="majorForm.isTopLevel" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="majorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMajor" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="classDialogVisible" :title="isClassEdit ? '编辑班级' : '添加班级'" width="500px" destroy-on-close>
      <el-form :model="classForm" label-width="90px">
        <el-form-item label="班级名称" required>
          <el-input v-model="classForm.className" placeholder="如：计科21-1班" />
        </el-form-item>
        <el-form-item label="所属院系" required>
          <el-select v-model="classForm.deptId" placeholder="请选择院系" style="width: 100%;" @change="classForm.majorId = ''">
            <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属专业" required>
          <el-select v-model="classForm.majorId" placeholder="请先选择院系" style="width: 100%;">
            <el-option v-for="m in filteredMajors" :key="m.id" :label="m.majorName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="classForm.grade" placeholder="如：2021" />
        </el-form-item>
        <el-form-item label="班主任">
          <el-input v-model="classForm.advisor" placeholder="请输入班主任姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClass" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量生成班级" width="600px" destroy-on-close>
      <div style="margin-bottom: 16px; color: #666; font-size: 13px;">
        遍历所有专业，为每个专业生成对应数量的班级，命名格式：<span style="color: #409eff;">专业简称 + 年级后两位 + 序号</span>，如：机械261、电气262
      </div>
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="年级" required>
          <el-input-number v-model="batchForm.grade" :min="2000" :max="2099" placeholder="如：2022" style="width: 100%;" />
          <span style="margin-left: 8px; color: #999; font-size: 12px;">如当前为2026年，22届毕业生请填 2022</span>
        </el-form-item>
        <el-form-item label="每个专业班级数" required>
          <el-input-number v-model="batchForm.classCountPerMajor" :min="1" :max="20" style="width: 100%;" />
          <div style="margin-top: 6px; color: #909399; font-size: 12px;">
            当前共 {{ majorList.length }} 个专业，将生成约 {{ majorList.length * batchForm.classCountPerMajor }} 个班级
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchGenerateClasses" :loading="submitting">确认生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDeleteDialogVisible" title="批量删除班级" width="450px" destroy-on-close>
      <div style="color: #f56c6c; font-size: 13px; margin-bottom: 16px;">
        警告：此操作将删除该年级所有班级及其所属学生，数据无法恢复，请谨慎操作！
      </div>
      <el-form :model="batchDeleteForm" label-width="80px">
        <el-form-item label="年级" required>
          <el-input-number v-model="batchDeleteForm.grade" :min="2000" :max="2099" placeholder="如：2026" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDeleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleBatchDeleteClasses" :loading="submitting">确认删除</el-button>
      </template>
    </el-dialog>

    <!-- 企业管理弹窗 -->
    <el-dialog v-model="companyDialogVisible" :title="isCompanyEdit ? '编辑企业' : '添加企业'" width="700px" destroy-on-close>
      <el-form :model="companyForm" label-width="110px" ref="companyFormRef">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="企业名称" required>
              <el-input v-model="companyForm.companyName" placeholder="请输入企业全称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统一社会信用代码">
              <el-input v-model="companyForm.unifiedCreditCode" placeholder="18位信用代码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法定代表人">
              <el-input v-model="companyForm.legalPerson" placeholder="法定代表人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="companyForm.contactPerson" placeholder="联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="companyForm.contactPhone" placeholder="手机或座机" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="companyForm.contactEmail" placeholder="email@example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在省份">
              <el-input v-model="companyForm.province" placeholder="如：广东省" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市">
              <el-input v-model="companyForm.city" placeholder="如：深圳市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址">
              <el-input v-model="companyForm.address" placeholder="详细办公地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属行业">
              <el-select v-model="companyForm.industry" placeholder="请选择" style="width:100%;">
                <el-option v-for="ind in industryOptions" :key="ind" :label="ind" :value="ind" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业规模">
              <el-select v-model="companyForm.scale" placeholder="请选择" style="width:100%;">
                <el-option v-for="s in scaleOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业性质">
              <el-select v-model="companyForm.nature" placeholder="请选择" style="width:100%;">
                <el-option v-for="n in natureOptions" :key="n" :label="n" :value="n" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绑定院系">
              <el-select v-model="companyForm.deptId" placeholder="不绑定" clearable style="width:100%;">
                <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="企业简介">
              <el-input v-model="companyForm.introduction" type="textarea" :rows="3" placeholder="请输入企业简介（选填）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="认证状态">
              <el-select v-model="companyForm.authStatus" style="width:100%;">
                <el-option label="待审核" value="pending" />
                <el-option label="已通过" value="approved" />
                <el-option label="已驳回" value="rejected" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-select v-model="companyForm.status" style="width:100%;">
                <el-option label="正常" value="0" />
                <el-option label="禁用" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="companyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="companySave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 驳回企业弹窗 -->
    <el-dialog v-model="companyRejectDialogVisible" title="驳回企业入驻" width="400px" destroy-on-close>
      <el-form>
        <el-form-item label="驳回原因">
          <el-input v-model="companyRejectRemark" type="textarea" :rows="3" placeholder="请输入驳回原因（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="companyRejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="companyConfirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 职位管理弹窗 -->
    <el-dialog v-model="jobDialogVisible" :title="isJobEdit ? '编辑职位' : '添加职位'" width="720px" destroy-on-close>
      <el-form :model="jobForm" label-width="100px" ref="jobFormRef">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属企业" required>
              <el-select v-model="jobForm.companyId" placeholder="请选择企业" filterable style="width:100%;" @change="onJobCompanyChange">
                <el-option v-for="c in allCompanies" :key="c.id" :label="c.companyName" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位名称" required>
              <el-input v-model="jobForm.jobName" placeholder="如：Java开发工程师" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位类型">
              <el-select v-model="jobForm.jobType" placeholder="请选择" style="width:100%;">
                <el-option v-for="t in jobTypeOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作城市">
              <el-input v-model="jobForm.workCity" placeholder="如：深圳市" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低月薪">
              <el-input-number v-model="jobForm.salaryMin" :min="0" :step="500" :precision="0" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高月薪">
              <el-input-number v-model="jobForm.salaryMax" :min="0" :step="500" :precision="0" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历要求">
              <el-select v-model="jobForm.educationRequired" placeholder="请选择" style="width:100%;">
                <el-option v-for="e in educationOptions" :key="e" :label="e" :value="e" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="招聘人数">
              <el-input-number v-model="jobForm.recruitNumber" :min="1" :max="1000" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止日期">
              <el-date-picker v-model="jobForm.deadline" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="任职要求">
              <el-input v-model="jobForm.requirement" type="textarea" :rows="3" placeholder="请输入任职要求" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="岗位职责">
              <el-input v-model="jobForm.responsibility" type="textarea" :rows="3" placeholder="请输入岗位职责" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="福利待遇">
              <el-input v-model="jobForm.benefits" type="textarea" :rows="2" placeholder="如：五险一金、年终奖、带薪年假..." />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="技能要求">
              <el-input v-model="jobForm.skillRequired" placeholder="如：Java、Spring Boot、MySQL" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位状态">
              <el-select v-model="jobForm.status" style="width:100%;">
                <el-option label="发布（已上架）" value="published" />
                <el-option label="下架（草稿）" value="paused" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="jobDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="jobSave">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { adminApi, dictApi } from '@/api'

const activeTab = ref('dept')
const submitting = ref(false)
const initLoading = ref(false)
const initStatus = reactive({ deptCount: 0, majorCount: 0, classCount: 0 })
const deptList = ref([])
const majorList = ref([])
const classList = ref([])
const deptDialogVisible = ref(false)
const majorDialogVisible = ref(false)
const classDialogVisible = ref(false)
const batchDialogVisible = ref(false)
const batchDeleteDialogVisible = ref(false)
const batchForm = reactive({ grade: new Date().getFullYear(), classCountPerMajor: 2 })
const batchDeleteForm = reactive({ grade: new Date().getFullYear() })
const isDeptEdit = ref(false)
const isMajorEdit = ref(false)
const isClassEdit = ref(false)
const deptForm = reactive({ id: null, deptName: '' })
const majorForm = reactive({ id: null, majorName: '', deptId: '', isTopLevel: '0', shortName: '' })
const classForm = reactive({ id: null, className: '', majorId: '', deptId: '', grade: '', advisor: '' })
const classFilterDeptId = ref(null)
const classFilterMajorId = ref(null)
const classKeyword = ref('')
const classPage = ref(1)
const classPageSize = ref(10)
const classTotal = ref(0)
const inviteList = ref([])
const inviteCount = ref(1)
const selectedInviteIds = ref([])
const refreshTimer = ref(null)

// ---------- 企业管理 ----------
const companyLoading = ref(false)
const companyList = ref([])
const companyPage = ref(1)
const companyPageSize = ref(10)
const companyTotal = ref(0)
const companyKeyword = ref('')
const companyFilterStatus = ref('')
const companyDialogVisible = ref(false)
const companyRejectDialogVisible = ref(false)
const isCompanyEdit = ref(false)
const companyRejectTarget = ref(null)
const companyRejectRemark = ref('')
const companyFormRef = ref(null)
const allCompanies = ref([])
const companyForm = reactive({
  id: null, companyName: '', unifiedCreditCode: '', legalPerson: '',
  contactPerson: '', contactPhone: '', contactEmail: '',
  province: '', city: '', address: '', industry: '', scale: '',
  nature: '', introduction: '', deptId: null, authStatus: 'approved', status: '0'
})

const industryOptions = [
  '互联网', '计算机软件', '电子/半导体', '通信', '机械/装备',
  '建筑/房地产', '金融/银行', '教育培训', '医疗卫生', '化工/材料',
  '矿业/能源', '航空航天', '政府/军队', '批发/零售', '其他'
]
const scaleOptions = ['0-99人', '100-499人', '500-999人', '1000-9999人', '10000人以上']
const natureOptions = [
  '民营企业', '国有企业', '合资企业', '外资企业', '上市公司',
  '事业单位', '政府机关', '非营利组织', '其他'
]

function companyStatusText(s) {
  return { pending: '待审核', approved: '已通过', rejected: '已驳回' }[s] || s
}
function companyStatusTagType(s) {
  return { pending: 'warning', approved: 'success', rejected: 'danger' }[s] || 'info'
}

function loadCompanies() {
  companyLoading.value = true
  adminApi.company.list({
    page: companyPage.value, size: companyPageSize.value,
    keyword: companyKeyword.value || undefined,
    status: companyFilterStatus.value || undefined
  }).then(res => {
    companyList.value = res?.records || []
    companyTotal.value = res?.total || 0
  }).catch(() => { companyList.value = []; companyTotal.value = 0 })
    .finally(() => { companyLoading.value = false })
}

function loadAllCompanies() {
  adminApi.company.list({ page: 1, size: 1000 }).then(res => {
    allCompanies.value = res?.records || []
  }).catch(() => { allCompanies.value = [] })
}

function openCompanyDialog(row = null) {
  if (row) {
    isCompanyEdit.value = true
    Object.assign(companyForm, {
      id: row.id, companyName: row.companyName || '', unifiedCreditCode: row.unifiedCreditCode || '',
      legalPerson: row.legalPerson || '', contactPerson: row.contactPerson || '',
      contactPhone: row.contactPhone || '', contactEmail: row.contactEmail || '',
      province: row.province || '', city: row.city || '', address: row.address || '',
      industry: row.industry || '', scale: row.scale || '', nature: row.nature || '',
      introduction: row.introduction || '', deptId: row.deptId || null,
      authStatus: row.authStatus || 'approved', status: row.status || '0'
    })
  } else {
    isCompanyEdit.value = false
    Object.assign(companyForm, {
      id: null, companyName: '', unifiedCreditCode: '', legalPerson: '',
      contactPerson: '', contactPhone: '', contactEmail: '',
      province: '', city: '', address: '', industry: '', scale: '',
      nature: '', introduction: '', deptId: null, authStatus: 'approved', status: '0'
    })
  }
  companyDialogVisible.value = true
}

function companySave() {
  if (!companyForm.companyName) { ElMessage.warning('请输入企业名称'); return }
  submitting.value = true
  const api = companyForm.id
    ? adminApi.company.update(companyForm.id, companyForm)
    : adminApi.company.create(companyForm)
  api.then(() => {
    ElMessage.success('保存成功')
    companyDialogVisible.value = false
    loadCompanies()
  }).catch(err => ElMessage.error(err?.response?.data?.message || '保存失败'))
    .finally(() => { submitting.value = false })
}

function companyAudit(row, action) {
  if (action === 'reject') {
    companyRejectTarget.value = row
    companyRejectRemark.value = ''
    companyRejectDialogVisible.value = true
    return
  }
  ElMessageBox.confirm(`确定通过企业「${row.companyName}」的入驻申请吗？`, '审核确认', { type: 'success' })
    .then(() => adminApi.company.audit(row.id, 'approve', '')
      .then(() => { ElMessage.success('已通过'); loadCompanies() })
      .catch(err => ElMessage.error(err?.response?.data?.message || '操作失败'))
    ).catch(() => {})
}

function companyConfirmReject() {
  submitting.value = true
  adminApi.company.audit(companyRejectTarget.value.id, 'reject', companyRejectRemark.value)
    .then(() => { ElMessage.success('已驳回'); companyRejectDialogVisible.value = false; loadCompanies() })
    .catch(err => ElMessage.error(err?.response?.data?.message || '操作失败'))
    .finally(() => { submitting.value = false })
}

function openCompanyReject(row) {
  companyRejectTarget.value = row
  companyRejectRemark.value = ''
  companyRejectDialogVisible.value = true
}

function companyDelete(row) {
  ElMessageBox.confirm(`确定删除企业「${row.companyName}」吗？`, '删除确认', { type: 'warning', confirmButtonText: '确认删除' })
    .then(() => adminApi.company.delete(row.id)
      .then(() => { ElMessage.success('删除成功'); loadCompanies() })
      .catch(err => ElMessage.error(err?.response?.data?.message || '删除失败'))
    ).catch(() => {})
}

// ---------- 职位管理 ----------
const jobLoading = ref(false)
const jobList = ref([])
const jobPage = ref(1)
const jobPageSize = ref(10)
const jobTotal = ref(0)
const jobKeyword = ref('')
const jobFilterStatus = ref('')
const jobDialogVisible = ref(false)
const isJobEdit = ref(false)
const jobFormRef = ref(null)
const jobForm = reactive({
  id: null, companyId: null, jobName: '', jobType: '', workCity: '',
  salaryMin: null, salaryMax: null, educationRequired: '', recruitNumber: null,
  requirement: '', responsibility: '', benefits: '', skillRequired: '',
  deadline: '', status: 'published'
})
const jobTypeOptions = ['全职', '兼职', '实习', '校招', '社招']
const educationOptions = ['初中及以下', '高中/中专', '大专', '本科', '硕士', '博士', '不限']

function loadJobs() {
  jobLoading.value = true
  adminApi.job.list({
    page: jobPage.value, size: jobPageSize.value,
    keyword: jobKeyword.value || undefined,
    status: jobFilterStatus.value || undefined
  }).then(res => {
    jobList.value = res?.records || []
    jobTotal.value = res?.total || 0
  }).catch(() => { jobList.value = []; jobTotal.value = 0 })
    .finally(() => { jobLoading.value = false })
}

function openJobDialog(row = null) {
  if (row) {
    isJobEdit.value = true
    Object.assign(jobForm, {
      id: row.id, companyId: row.companyId, jobName: row.jobName || '',
      jobType: row.jobType || '', workCity: row.workCity || '',
      salaryMin: row.salaryMin || null, salaryMax: row.salaryMax || null,
      educationRequired: row.educationRequired || '', recruitNumber: row.recruitNumber || null,
      requirement: row.requirement || '', responsibility: row.responsibility || '',
      benefits: row.benefits || '', skillRequired: row.skillRequired || '',
      deadline: row.deadline || '', status: row.status || 'published'
    })
  } else {
    isJobEdit.value = false
    Object.assign(jobForm, {
      id: null, companyId: null, jobName: '', jobType: '', workCity: '',
      salaryMin: null, salaryMax: null, educationRequired: '', recruitNumber: null,
      requirement: '', responsibility: '', benefits: '', skillRequired: '',
      deadline: '', status: 'published'
    })
  }
  jobDialogVisible.value = true
}

function onJobCompanyChange(companyId) {
  const company = allCompanies.value.find(c => c.id === companyId)
  if (company) {
    // 自动填充企业名称
  }
}

function jobSave() {
  if (!jobForm.companyId) { ElMessage.warning('请选择所属企业'); return }
  if (!jobForm.jobName) { ElMessage.warning('请输入职位名称'); return }
  submitting.value = true
  const api = jobForm.id
    ? adminApi.job.update(jobForm.id, jobForm)
    : adminApi.job.create(jobForm)
  api.then(() => {
    ElMessage.success('保存成功')
    jobDialogVisible.value = false
    loadJobs()
  }).catch(err => ElMessage.error(err?.response?.data?.message || '保存失败'))
    .finally(() => { submitting.value = false })
}

function jobPublish(row) {
  ElMessageBox.confirm(`确定上架职位「${row.jobName}」吗？`, '上架确认', { type: 'success' })
    .then(() => adminApi.job.publish(row.id)
      .then(() => { ElMessage.success('已上架'); loadJobs() })
      .catch(err => ElMessage.error(err?.response?.data?.message || '操作失败'))
    ).catch(() => {})
}

function jobPause(row) {
  ElMessageBox.confirm(`确定下架职位「${row.jobName}」吗？`, '下架确认', { type: 'warning' })
    .then(() => adminApi.job.pause(row.id)
      .then(() => { ElMessage.success('已下架'); loadJobs() })
      .catch(err => ElMessage.error(err?.response?.data?.message || '操作失败'))
    ).catch(() => {})
}

function jobDelete(row) {
  ElMessageBox.confirm(`确定删除职位「${row.jobName}」吗？`, '删除确认', { type: 'warning', confirmButtonText: '确认删除' })
    .then(() => adminApi.job.delete(row.id)
      .then(() => { ElMessage.success('删除成功'); loadJobs() })
      .catch(err => ElMessage.error(err?.response?.data?.message || '删除失败'))
    ).catch(() => {})
}

const REFRESH_INTERVAL = 5000

watch(() => activeTab.value, (newTab) => {
  if (newTab === 'major') loadMajors()
  else if (newTab === 'cls') loadClasses()
  else if (newTab === 'company') loadCompanies()
  else if (newTab === 'job') loadJobs()
})


const filteredMajors = computed(() => {
  if (!classForm.deptId) return []
  return majorList.value.filter(m => m.deptId === classForm.deptId)
})

const classFilterMajors = computed(() => {
  if (!classFilterDeptId.value) return majorList.value
  return majorList.value.filter(m => m.deptId === classFilterDeptId.value)
})

function loadClasses() {
  const params = {
    pageNum: classPage.value,
    pageSize: classPageSize.value,
    keyword: classKeyword.value || undefined,
    deptId: classFilterDeptId.value || undefined,
    majorId: classFilterMajorId.value || undefined
  }
  adminApi.sysClass.list(params).then(res => {
    const records = res?.records || res?.list || []
    const total = res?.total || 0
    classList.value = [...records]
    classTotal.value = total
    normalizeClassListForSpan(classList.value)
  }).catch(() => { classList.value = []; classTotal.value = 0 })
}
function onClassFilterDeptChange() {
  classFilterMajorId.value = null
  classPage.value = 1
  loadClasses()
}
function onClassFilterMajorChange() {
  classPage.value = 1
  loadClasses()
}
function onClassKeywordSearch() {
  classPage.value = 1
  loadClasses()
}
function onClassPageChange(page) { classPage.value = page; loadClasses() }
function onClassSizeChange(size) { classPageSize.value = size; classPage.value = 1; loadClasses() }

function normalizeClassListForSpan(list) {
  let idx = 0
  for (const item of list) {
    item._index = idx
    item._majorIndex = item.majorId || 'none'
    idx++
  }
}

function classSpanMethod({ row, columnIndex }) {
  if (columnIndex === 1 || columnIndex === 2) {
    const currentIdx = row._index
    const currentMajorId = row._majorIndex
    const prev = currentIdx > 0 ? classList.value[currentIdx - 1] : null
    if (prev && prev._majorIndex === currentMajorId) {
      return { rowspan: 0, colspan: 0 }
    }
    let rowspan = 0
    for (let i = currentIdx; i < classList.value.length; i++) {
      if (classList.value[i]._majorIndex === currentMajorId) {
        rowspan++
      } else {
        break
      }
    }
    return { rowspan, colspan: 1 }
  }
}

function loadDepts() { adminApi.dept.list().then(res => { deptList.value = res || [] }).catch(() => { deptList.value = [] }) }
function loadMajors() {
  adminApi.major.all().then(res => {
    majorList.value = Array.isArray(res) ? res : []
  }).catch(() => { majorList.value = [] })
}
function loadInitStatus() {
  adminApi.autoInit.status().then(res => {
    initStatus.deptCount = res?.deptCount || 0
    initStatus.majorCount = res?.majorCount || 0
    initStatus.classCount = res?.classCount || 0
  }).catch(() => {})
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer.value = setInterval(() => {
    loadInitStatus()
  }, REFRESH_INTERVAL)
}

function stopAutoRefresh() {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

async function handleAutoInit() {
  initLoading.value = true
  try {
    await adminApi.autoInit.doInit()
    ElMessage.success('初始化成功')
    loadInitStatus()
    loadDepts()
    loadMajors()
    loadClasses()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '初始化失败')
  } finally {
    initLoading.value = false
  }
}

async function handleResetInit() {
  await ElMessageBox.confirm('强制重置将清空所有院系、专业、班级数据，确定继续？', '警告', { type: 'warning' })
  initLoading.value = true
  try {
    await adminApi.autoInit.reset()
    ElMessage.success('强制重置成功')
    loadInitStatus()
    loadDepts()
    loadMajors()
    loadClasses()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '重置失败')
  } finally {
    initLoading.value = false
  }
}

function openDeptDialog(row = null) {
  isDeptEdit.value = !!row
  if (row) Object.assign(deptForm, { id: row.id, deptName: row.deptName })
  else Object.assign(deptForm, { id: null, deptName: '' })
  deptDialogVisible.value = true
}
function saveDept() {
  if (!deptForm.deptName) { ElMessage.warning('请输入院系名称'); return }
  submitting.value = true
  const api = deptForm.id ? adminApi.dept.update(deptForm.id, deptForm) : adminApi.dept.create(deptForm)
  api.then(() => { ElMessage.success('保存成功'); deptDialogVisible.value = false; loadDepts(); loadInitStatus() })
    .catch(err => ElMessage.error(err?.response?.data?.message || '保存失败'))
    .finally(() => { submitting.value = false })
}
function deleteDept(row) {
  ElMessageBox.confirm(`确定删除院系 "${row.deptName}" 吗？`, '提示', { type: 'warning' })
    .then(() => adminApi.dept.delete(row.id)
      .then(() => { ElMessage.success('删除成功'); loadDepts(); loadInitStatus() })
      .catch(err => ElMessage.error(err?.response?.data?.message || '删除失败'))
    )
    .catch(() => {})
}

function openMajorDialog(row = null) {
  isMajorEdit.value = !!row
  if (row) Object.assign(majorForm, { id: row.id, majorName: row.majorName, deptId: row.deptId, isTopLevel: row.isTopLevel || '0', shortName: row.shortName || '' })
  else Object.assign(majorForm, { id: null, majorName: '', deptId: '', isTopLevel: '0' })
  majorDialogVisible.value = true
}
function saveMajor() {
  if (!majorForm.majorName || !majorForm.deptId) { ElMessage.warning('请填写完整信息'); return }
  submitting.value = true
  const api = majorForm.id ? adminApi.major.update(majorForm.id, majorForm) : adminApi.major.create(majorForm)
  api.then(() => { ElMessage.success('保存成功'); majorDialogVisible.value = false; loadMajors(); loadInitStatus() })
    .catch(err => ElMessage.error(err?.response?.data?.message || '保存失败'))
    .finally(() => { submitting.value = false })
}
function deleteMajor(row) {
  ElMessageBox.confirm(`确定删除专业 "${row.majorName}" 吗？`, '提示', { type: 'warning' })
    .then(() => adminApi.major.delete(row.id)
      .then(() => { ElMessage.success('删除成功'); loadMajors(); loadInitStatus() })
      .catch(err => ElMessage.error(err?.response?.data?.message || '删除失败'))
    )
    .catch(() => {})
}

async function toggleMajorRecommend(row) {
  const newValue = row.recommendEnabled === '1' ? '0' : '1'
  const action = newValue === '1' ? '开启' : '关闭'
  try {
    await ElMessageBox.confirm(
      `确定${action}专业「${row.majorName}」的推荐算法吗？`,
      `${action}确认`,
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await adminApi.major.update(row.id, { ...row, recommendEnabled: newValue })
    ElMessage.success(`推荐算法已${action}`)
    loadMajors()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  }
}

function openClassDialog(row = null) {
  isClassEdit.value = !!row
  if (row) Object.assign(classForm, { id: row.id, className: row.className, majorId: row.majorId, deptId: row.deptId, grade: row.grade || '', advisor: row.advisor || '' })
  else Object.assign(classForm, { id: null, className: '', majorId: '', deptId: '', grade: '', advisor: '' })
  classDialogVisible.value = true
}
function saveClass() {
  if (!classForm.className || !classForm.deptId || !classForm.majorId) { ElMessage.warning('请填写完整信息'); return }
  submitting.value = true
  const api = classForm.id ? adminApi.sysClass.update(classForm.id, classForm) : adminApi.sysClass.create(classForm)
  api.then(() => { ElMessage.success('保存成功'); classDialogVisible.value = false; loadClasses(); loadInitStatus() })
    .catch(err => ElMessage.error(err?.response?.data?.message || '保存失败'))
    .finally(() => { submitting.value = false })
}
function deleteClass(row) {
  ElMessageBox.confirm(`确定删除班级 "${row.className}" 吗？`, '提示', { type: 'warning' })
    .then(() => adminApi.sysClass.delete(row.id)
      .then(() => { ElMessage.success('删除成功'); loadClasses(); loadInitStatus() })
      .catch(err => ElMessage.error(err?.response?.data?.message || '删除失败'))
    )
    .catch(() => {})
}

function openBatchGenerateDialog() {
  batchDialogVisible.value = true
}

function openBatchDeleteDialog() {
  batchDeleteForm.grade = new Date().getFullYear()
  batchDeleteDialogVisible.value = true
}

async function handleBatchGenerateClasses() {
  if (!batchForm.grade || !batchForm.classCountPerMajor) {
    ElMessage.warning('请填写年级和班级数量')
    return
  }
  submitting.value = true
  try {
    await adminApi.sysClass.batchGenerate({
      grade: String(batchForm.grade),
      classCountPerMajor: batchForm.classCountPerMajor,
      customClassCounts: null
    })
    ElMessage.success('批量生成成功')
    batchDialogVisible.value = false
    loadClasses()
    loadInitStatus()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '生成失败')
  } finally {
    submitting.value = false
  }
}

async function handleBatchDeleteClasses() {
  if (!batchDeleteForm.grade) {
    ElMessage.warning('请填写年级')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除 ${batchDeleteForm.grade} 届所有班级及其所属学生吗？此操作不可恢复！`,
      '危险操作',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    await adminApi.sysClass.batchDeleteByGrade(String(batchDeleteForm.grade))
    ElMessage.success('删除成功')
    batchDeleteDialogVisible.value = false
    loadClasses()
    loadInitStatus()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
  } finally {
    submitting.value = false
  }
}
function loadInviteCodes() {
  adminApi.invitationCode.list().then(res => {
    inviteList.value = res || []
  }).catch(() => { inviteList.value = [] })
}
async function handleBatchGenerate() {
  submitting.value = true
  try {
    await adminApi.invitationCode.generateBatch({ count: inviteCount.value })
    ElMessage.success(`成功生成 ${inviteCount.value} 个注册码`)
    loadInviteCodes()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '生成失败')
  } finally {
    submitting.value = false
  }
}
function deleteInvite(row) {
  ElMessageBox.confirm(`确定删除注册码 "${row.code}" 吗？`, '提示', { type: 'warning' })
    .then(() => adminApi.invitationCode.delete(row.id)
      .then(() => { ElMessage.success('删除成功'); loadInviteCodes() })
      .catch(err => ElMessage.error(err?.response?.data?.message || '删除失败'))
    )
    .catch(() => {})
}
function onInviteSelectionChange(rows) {
  selectedInviteIds.value = rows.map(r => r.id)
}
async function handleBatchDelete() {
  if (selectedInviteIds.value.length === 0) return
  await ElMessageBox.confirm(`确定删除选中的 ${selectedInviteIds.value.length} 个注册码吗？`, '批量删除', { type: 'warning' })
  try {
    await adminApi.invitationCode.batchDelete(selectedInviteIds.value)
    ElMessage.success('批量删除成功')
    selectedInviteIds.value = []
    loadInviteCodes()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '批量删除失败')
  }
}

function exportDepts(cmd) {
  if (cmd === 'all') {
    adminApi.dept.list({ page: 1, size: 99999 }).then(res => {
      const data = Array.isArray(res) ? res : (res?.records || [])
      if (!data.length) { ElMessage.warning('暂无数据可导出'); return }
      const headers = [['院系名称', '状态']]
      const rows = data.map(d => [d.deptName, d.status === '0' ? '正常' : '禁用'])
      downloadCSV('院系列表_全部', [...headers, ...rows])
    }).catch(() => {})
  } else {
    if (!deptList.value.length) { ElMessage.warning('暂无数据可导出'); return }
    const headers = [['院系名称', '状态']]
    const rows = deptList.value.map(d => [d.deptName, d.status === '0' ? '正常' : '禁用'])
    downloadCSV('院系列表_当前页', [...headers, ...rows])
  }
}

function exportMajors(cmd) {
  if (cmd === 'all') {
    adminApi.major.all().then(res => {
      const data = Array.isArray(res) ? res : []
      if (!data.length) { ElMessage.warning('暂无数据可导出'); return }
      const headers = [['专业名称', '专业简称', '所属院系', '一流专业']]
      const rows = data.map(m => [m.majorName, m.shortName || '', m.deptName || '', m.isTopLevel === '1' ? '是' : '否'])
      downloadCSV('专业列表_全部', [...headers, ...rows])
    }).catch(() => {})
  } else {
    if (!majorList.value.length) { ElMessage.warning('暂无数据可导出'); return }
    const headers = [['专业名称', '专业简称', '所属院系', '一流专业']]
    const rows = majorList.value.map(m => [m.majorName, m.shortName || '', m.deptName || '', m.isTopLevel === '1' ? '是' : '否'])
    downloadCSV('专业列表_当前页', [...headers, ...rows])
  }
}

function exportClasses(cmd) {
  if (cmd === 'all') {
    dictApi.getAllClasses().then(res => {
      const data = Array.isArray(res) ? res : (res?.data || res?.records || [])
      if (!data.length) { ElMessage.warning('暂无数据可导出'); return }
      const headers = [['班级名称', '所属专业', '所属院系', '年级', '班主任', '学生数']]
      const rows = data.map(c => [c.className, c.majorName || '', c.deptName || '', c.grade || '', c.advisor || '', c.studentCount || 0])
      downloadCSV('班级列表_全部', [...headers, ...rows])
    }).catch(() => {})
  } else {
    if (!classList.value.length) { ElMessage.warning('暂无数据可导出'); return }
    const headers = [['班级名称', '所属专业', '所属院系', '年级', '班主任', '学生数']]
    const rows = classList.value.map(c => [c.className, c.majorName || '', c.deptName || '', c.grade || '', c.advisor || '', c.studentCount || 0])
    downloadCSV('班级列表_当前页', [...headers, ...rows])
  }
}

function exportCompanies(cmd) {
  if (cmd === 'all') {
    adminApi.company.list({ page: 1, size: 99999, exportAll: true }).then(res => {
      const data = res?.records || []
      if (!data.length) { ElMessage.warning('暂无数据可导出'); return }
      const headers = [['企业名称', '联系人', '联系电话', '所属行业', '规模', '城市', '认证状态', '账号状态', '创建时间']]
      const rows = data.map(c => [
        c.companyName, c.contactPerson || '', c.contactPhone || '',
        c.industry || '', c.scale || '', c.city || '',
        companyStatusText(c.authStatus),
        c.status === '0' ? '正常' : '禁用',
        formatTime(c.createTime)
      ])
      downloadCSV('企业列表_全部', [...headers, ...rows])
    }).catch(() => {})
  } else {
    if (!companyList.value.length) { ElMessage.warning('暂无数据可导出'); return }
    const headers = [['企业名称', '联系人', '联系电话', '所属行业', '规模', '城市', '认证状态', '账号状态', '创建时间']]
    const rows = companyList.value.map(c => [
      c.companyName, c.contactPerson || '', c.contactPhone || '',
      c.industry || '', c.scale || '', c.city || '',
      companyStatusText(c.authStatus),
      c.status === '0' ? '正常' : '禁用',
      formatTime(c.createTime)
    ])
    downloadCSV('企业列表_当前页', [...headers, ...rows])
  }
}

function exportJobs(cmd) {
  if (cmd === 'all') {
    adminApi.job.list({ page: 1, size: 99999, exportAll: true }).then(res => {
      const data = res?.records || []
      if (!data.length) { ElMessage.warning('暂无数据可导出'); return }
      const headers = [['职位名称', '薪资区间', '所属企业', '行业', '城市', '招聘人数', '学历要求', '浏览次数', '投递次数', '状态', '发布时间']]
      const rows = data.map(j => [
        j.jobName, j.salaryText || '', j.companyName || '',
        j.companyIndustry || '', j.workCity || '',
        j.recruitNumber || '若干', j.educationRequired || '',
        j.viewCount || 0, j.applyCount || 0,
        j.status === 'published' ? '已发布' : '已下架',
        j.publishTime || ''
      ])
      downloadCSV('职位列表_全部', [...headers, ...rows])
    }).catch(() => {})
  } else {
    if (!jobList.value.length) { ElMessage.warning('暂无数据可导出'); return }
    const headers = [['职位名称', '薪资区间', '所属企业', '行业', '城市', '招聘人数', '学历要求', '浏览次数', '投递次数', '状态', '发布时间']]
    const rows = jobList.value.map(j => [
      j.jobName, j.salaryText || '', j.companyName || '',
      j.companyIndustry || '', j.workCity || '',
      j.recruitNumber || '若干', j.educationRequired || '',
      j.viewCount || 0, j.applyCount || 0,
      j.status === 'published' ? '已发布' : '已下架',
      j.publishTime || ''
    ])
    downloadCSV('职位列表_当前页', [...headers, ...rows])
  }
}

function downloadCSV(filename, data) {
  const BOM = '\uFEFF'
  const csv = data.map(row =>
    row.map(cell => {
      const str = String(cell ?? '')
      if (str.includes(',') || str.includes('"') || str.includes('\n')) {
        return '"' + str.replace(/"/g, '""') + '"'
      }
      return str
    }).join(',')
  ).join('\r\n')
  const blob = new Blob([BOM + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename + '_' + new Date().toISOString().slice(0, 10) + '.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadInitStatus()
  loadDepts()
  loadMajors()
  loadClasses()
  loadInviteCodes()
  loadAllCompanies()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.init-card { margin-bottom: 0; }
.init-info { display: flex; align-items: center; }
.init-label { font-weight: 600; color: #333; margin-right: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header .filter-row { display: flex; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
