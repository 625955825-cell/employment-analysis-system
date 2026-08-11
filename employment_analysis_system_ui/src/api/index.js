import request from '@/utils/request'

export const authApi = {
  login(data) {
    return request.post('/auth/login', data)
  },
  register(data) {
    return request.post('/auth/register', data)
  },
  companyRegister(data) {
    return request.post('/auth/company-register', data)
  },
  getUserInfo() {
    return request.get('/auth/userinfo')
  },
  logout() {
    return request.post('/auth/logout')
  },
  refreshToken() {
    return request.post('/auth/refresh')
  },
  verifyCode(code) {
    return request.get('/admin/invitation-codes/verify', { params: { code } })
  }
}

export const dictApi = {
  getDepartments() {
    return request.get('/dict/departments')
  },
  getMajors(deptId) {
    return request.get('/dict/majors', { params: { deptId } })
  },
  getClasses(majorId) {
    return request.get('/admin/classes/by-major', { params: { majorId } })
  },
  getAllClasses() {
    return request.get('/admin/classes/all')
  }
}

export const fileApi = {
  upload(formData) {
    return request.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const studentApi = {
  getProfile() {
    return request.get('/student/profile')
  },
  updateProfile(data) {
    return request.put('/student/profile', data)
  },
  getHomeStats() {
    return request.get('/student/home-stats')
  },
  getResumes() {
    return request.get('/student/resume/list')
  },
  getResume(id) {
    return request.get(`/student/resume/${id}`)
  },
  createResume(data) {
    return request.post('/student/resume', data)
  },
  updateResume(id, data) {
    return request.put(`/student/resume/${id}`, data)
  },
  deleteResume(id) {
    return request.delete(`/student/resume/${id}`)
  },
  setDefaultResume(id) {
    return request.put(`/student/resume/${id}/default`)
  },
  getExportUrl(id) {
    return `/student/resume/${id}/export`
  },
  getMyApplications(params) {
    return request.get('/job/my-applications', { params })
  },
  getFavorites(params) {
    return request.get('/job/favorites', { params })
  },
  favoriteJob(jobId) {
    return request.post(`/job/favorite/${jobId}`)
  },
  unfavoriteJob(jobId) {
    return request.delete(`/job/favorite/${jobId}`)
  },
  applyJob(jobId, data) {
    return request.post(`/job/apply/${jobId}`, data)
  },
  cancelApplication(id) {
    return request.delete(`/job/application/${id}`)
  },
  // Offer
  getOffers() { return request.get('/student/offers') },
  acceptOffer(id) { return request.put('/student/offer/' + id + '/accept') },
  declineOffer(id) { return request.put('/student/offer/' + id + '/decline') },
  // 三方协议
  getMyAgreements() { return request.get('/student/agreements') },
  applyAgreement(data) { return request.post('/student/agreement/apply', data) },
  // 面试邀请（学生端）
  getMyInterviews() { return request.get('/student/interviews') },
  acceptInterview(id) { return request.put('/student/interview/' + id + '/accept') },
  declineInterview(id) { return request.put('/student/interview/' + id + '/decline') }
}

export const jobApi = {
  getList(params) {
    return request.get('/job/list', { params })
  },
  getDetail(id) {
    return request.get(`/job/${id}`)
  },
  applyJob(jobId, data) {
    return request.post(`/job/apply/${jobId}`, data)
  },
  favoriteJob(jobId) {
    return request.post(`/job/favorite/${jobId}`)
  },
  unfavoriteJob(jobId) {
    return request.delete(`/job/favorite/${jobId}`)
  },
  getMyInterviews(params) {
    return request.get('/interview/invitations', { params })
  },
  acceptInterview(id) {
    return request.put(`/interview/invitation/${id}/accept`)
  },
  rejectInterview(id) {
    return request.put(`/interview/invitation/${id}/reject`)
  },
  getMyOffers(params) {
    return request.get('/interview/offers', { params })
  },
  acceptOffer(id) {
    return request.put(`/interview/offer/${id}/accept`)
  },
  rejectOffer(id) {
    return request.put(`/interview/offer/${id}/reject`)
  }
}

export const employmentApi = {
  getMyRecord() {
    return request.get('/employment/my-record')
  },
  createRecord(data) {
    return request.post('/employment/record', data)
  },
  updateRecord(id, data) {
    return request.put(`/employment/record/${id}`, data)
  },
  uploadAttachment(employmentId, formData) {
    return request.post(`/employment/record/${employmentId}/attachment`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getRecordStatus(id) {
    return request.get(`/employment/record/${id}/status`)
  }
}

export const dataPermissionApi = {
  getMyRequests(params) {
    return request.get('/student/data-permission/list', { params })
  },
  apply(data) {
    return request.post('/student/data-permission/apply', data)
  },
  getRequestDetail(id) {
    return request.get(`/student/data-permission/${id}`)
  },
  cancelRequest(id) {
    return request.delete(`/student/data-permission/${id}`)
  },
  getApprovedData(requestId) {
    return request.get('/student/data-permission/data', { params: { requestId } })
  }
}

export const notificationApi = {
  getList(params) {
    return request.get('/notification/list', { params })
  },
  getUnreadCount() {
    return request.get('/notification/unread-count')
  },
  markAsRead(id) {
    return request.put(`/notification/${id}/read`)
  },
  markAllAsRead() {
    return request.put('/notification/read-all')
  }
}

export const recommendApi = {
  // 学生端
  getRecommendStatus() {
    return request.get('/recommend/status')
  },
  getJobRecommendations(params = { topN: 10, sourceFilter: 'all' }) {
    return request.get('/recommend/jobs', { params })
  },
  submitFeedback(data) {
    return request.post('/recommend/feedback', data)
  },
  applyHrJob(jobId, data) {
    return studentApi.applyJob(jobId, data)
  },
  getHistory(params) {
    return request.get('/recommend/history', { params })
  },
  // 数据分析员端
  getMajorOverview() {
    return request.get('/recommend/admin/overview')
  },
  getMajorDetail(majorId) {
    return request.get(`/recommend/admin/major/${majorId}`)
  },
  triggerRecommend(data) {
    return request.post('/recommend/admin/trigger', data)
  },
  toggleRecommendEnabled(majorId, recommendEnabled) {
    return request.put(`/recommend/admin/major/${majorId}/toggle`, { recommendEnabled })
  },
  trainRecommendModel(majorId, algorithmType = 'tfidf') {
    return request.post(`/recommend/admin/train/${majorId}`, { algorithmType })
  },
  trainAllMajors(algorithmType = 'tfidf') {
    return request.post('/recommend/admin/train-all', { algorithmType })
  },
  enableAllTrainedMajors() {
    return request.post('/recommend/admin/enable-all')
  },
  getModelTrainingStats() {
    return request.get('/recommend/admin/training-stats')
  },
  getRecommendResults(params) {
    return request.get('/recommend/admin/results', { params })
  },
  getRecommendStats() {
    return request.get('/recommend/admin/stats')
  },
  getWeightList() {
    return request.get('/data/weight/list')
  },
  getWeightMap() {
    return request.get('/data/weight/map')
  },
  updateWeights(data) {
    return request.post('/data/weight/update', data)
  },
  resetWeights() {
    return request.post('/data/weight/reset')
  },
  evaluateModel() {
    return request.get('/recommend/admin/evaluate')
  }
}

export const analyticsApi = {
  getOverview(params) {
    return request.get('/analytics/overview', { params })
  },
  getDeptStats(params) {
    return request.get('/analytics/dept-stats', { params })
  },
  getClassStats(params) {
    return request.get('/analytics/class-stats', { params })
  },
  getEmploymentTrend(params) {
    return request.get('/analytics/employment-trend', { params })
  },
  getSalaryDist(params) {
    return request.get('/analytics/salary-dist', { params })
  },
  getIndustryDist(params) {
    return request.get('/analytics/industry-dist', { params })
  },
  getProvinceDist(params) {
    return request.get('/analytics/province-dist', { params })
  },
  getAvailableYears() {
    return request.get('/analytics/available-years')
  },
  // 未就业学生列表
  getUnemployedStudents(params) {
    return request.get('/analytics/unemployed-students', { params })
  },
  // 专业预警统计
  getMajorStats(params) {
    return request.get('/analytics/major-stats', { params })
  }
}

export const spiderApi = {
  // 站点管理
  getSites() { return request.get('/spider/sites') },
  getMajorKeywords() { return request.get('/spider/majors') },
  getAllMajors() { return request.get('/spider/all-majors') },
  addKeyword(majorName, keyword) { return request.post(`/spider/major/${encodeURIComponent(majorName)}/keyword`, { keyword }) },
  addKeywords(majorName, keywords) { return request.post(`/spider/major/${encodeURIComponent(majorName)}/keywords`, { keywords }) },
  deleteKeyword(majorName, keyword) { return request.delete(`/spider/major/${encodeURIComponent(majorName)}/keyword/${encodeURIComponent(keyword)}`) },
  // 任务管理
  getTasks(params) { return request.get('/spider/tasks', { params }) },
  createTask(data) { return request.post('/spider/task', data) },
  updateTask(id, data) { return request.put(`/spider/task/${id}`, data) },
  deleteTask(id) { return request.delete(`/spider/task/${id}`) },
  // 爬虫执行控制
  runTask(id) { return request.post(`/spider/task/${id}/run`) },
  stopTask(id) { return request.post(`/spider/task/${id}/stop`) },
  getProgress(id) { return request.get(`/spider/task/${id}/progress`) },
  getTaskStatus(id) { return request.get(`/spider/task/${id}/status`) },
  // 采集数据
  getCollectedData(params) { return request.get('/spider/data', { params }) },
  deleteData(ids) { return request.delete('/spider/data/batch', { data: ids }) },
  syncData(ids) { return request.post('/spider/data/sync', ids) },
  syncAllData() { return request.post('/spider/data/sync-all') },
  clearAllData() { return request.delete('/spider/data/clear-all') },
  // ETL与训练池
  executeEtl(taskId) {
    const params = taskId ? { taskId } : {}
    return request.post('/spider/etl', null, { params })
  },
  getTrainingPool() { return request.get('/spider/training-pool') },
  // 全国就业统计
  getNationalStats() { return request.get('/spider/national-stats') },
  getEnhancedAnalytics(params) { return request.get('/spider/analytics/enhanced', { params }) },
  // 日志
  getLogs(params) { return request.get('/spider/logs', { params }) },
  clearLogs() { return request.delete('/spider/logs/clear') },
  // Hive查询
  executeHiveQuery(sql, database) {
    return request.post('/spider/hive/query', { sql, database })
  },
  // Flink任务
  getFlinkJobs() { return request.get('/spider/flink/jobs') },
  submitFlinkJob(data) { return request.post('/spider/flink/job', data) },
  toggleFlinkJob(jobId, action) { return request.put(`/spider/flink/job/${jobId}`, null, { params: { action } }) },
  deleteFlinkJob(jobId) { return request.delete(`/spider/flink/job/${jobId}`) },
  // 集群状态
  getClusterStatus() { return request.get('/spider/cluster/status') }
}

export const noticeApi = {
  getList(params) {
    return request.get('/notice/list', { params })
  },
  getMyNotices() {
    return request.get('/notice/my-notices')
  },
  getUnreadCount() {
    return request.get('/notice/unread-count')
  },
  create(data) {
    return request.post('/notice', data)
  },
  update(id, data) {
    return request.put(`/notice/${id}`, data)
  },
  toggleTop(id, topStatus) {
    return request.put(`/notice/${id}/top`, null, { params: { topStatus } })
  },
  delete(id) {
    return request.delete(`/notice/${id}`)
  }
}

export const adminApi = {
  autoInit: {
    status() { return request.get('/admin/auto-init/status') },
    doInit() { return request.post('/admin/auto-init') },
    reset() { return request.post('/admin/auto-init/reset') }
  },
  dept: {
    list() { return request.get('/admin/depts/list') },
    get(id) { return request.get(`/admin/depts/${id}`) },
    create(data) { return request.post('/admin/depts', data) },
    update(id, data) { return request.put(`/admin/depts/${id}`, data) },
    delete(id) { return request.delete(`/admin/depts/${id}`) }
  },
  major: {
    list() { return request.get('/admin/majors/list') },
    all() { return request.get('/admin/majors/all') },
    byDept(deptId) { return request.get('/admin/majors/by-dept', { params: { deptId } }) },
    get(id) { return request.get(`/admin/majors/${id}`) },
    create(data) { return request.post('/admin/majors', data) },
    update(id, data) { return request.put(`/admin/majors/${id}`, data) },
    delete(id) { return request.delete(`/admin/majors/${id}`) }
  },
  sysClass: {
    list(params) { return request.get('/admin/classes/list', { params }) },
    get(id) { return request.get(`/admin/classes/${id}`) },
    create(data) { return request.post('/admin/classes', data) },
    update(id, data) { return request.put(`/admin/classes/${id}`, data) },
    delete(id) { return request.delete(`/admin/classes/${id}`) },
    byMajor(majorId) { return request.get('/admin/classes/by-major', { params: { majorId } }) },
    batchGenerate(data) { return request.post('/admin/classes/batch-generate', data) },
    batchDeleteByGrade(grade) { return request.delete('/admin/classes/batch-by-grade', { params: { grade } }) }
  },
  invitationCode: {
    list() { return request.get('/admin/invitation-codes/list') },
    generateBatch(data) { return request.post('/admin/invitation-codes/generate', null, { params: data }) },
    get(id) { return request.get(`/admin/invitation-codes/${id}`) },
    delete(id) { return request.delete(`/admin/invitation-codes/${id}`) },
    batchDelete(ids) { return request.delete('/admin/invitation-codes/batch', { data: ids }) }
  },
  stats: {
    overview() { return request.get('/admin/stats/overview') },
    summary() { return request.get('/admin/stats/summary') },
    dashboard() { return request.get('/admin/stats/dashboard') }
  },
  account: {
    list(params) { return request.get('/admin/accounts/list', { params }) },
    roles() { return request.get('/admin/accounts/roles') },
    create(data) { return request.post('/admin/accounts', data) },
    update(id, data) { return request.put(`/admin/accounts/${id}`, data) },
    delete(id) { return request.delete(`/admin/accounts/${id}`) },
    resetPassword(id) { return request.put(`/admin/accounts/${id}/reset-password`) },
    assignRole(id, roleKey) { return request.put(`/admin/accounts/${id}/role`, { role: roleKey }) }
  },
  role: {
    list() { return request.get('/admin/roles/list') },
    permissions() { return request.get('/admin/roles/permissions') },
    get(id) { return request.get(`/admin/roles/${id}`) },
    create(data) { return request.post('/admin/roles', data) },
    update(id, data) { return request.put(`/admin/roles/${id}`, data) },
    delete(id) { return request.delete(`/admin/roles/${id}`) },
    assignPermissions(id, permissionIds) { return request.put(`/admin/roles/${id}/permissions`, { permissionIds }) }
  },
  log: {
    list(params) { return request.get('/admin/logs/list', { params }) },
    types() { return request.get('/admin/logs/types') },
    delete(id) { return request.delete(`/admin/logs/${id}`) },
    clear() { return request.delete('/admin/logs/clear') }
  },
  company: {
    list(params) { return request.get('/admin/company/list', { params }) },
    get(id) { return request.get(`/admin/company/${id}`) },
    create(data) { return request.post('/admin/company', data) },
    update(id, data) { return request.put(`/admin/company/${id}`, data) },
    delete(id) { return request.delete(`/admin/company/${id}`) },
    audit(id, action, remark) {
      return request.put(`/admin/company/${id}/audit`, null, { params: { action, remark } })
    },
    getAuthRecords(id) { return request.get(`/admin/company/${id}/auth-records`) },
    stats() { return request.get('/admin/company/stats') }
  },
  job: {
    list(params) { return request.get('/admin/job/list', { params }) },
    get(id) { return request.get(`/admin/job/${id}`) },
    create(data) { return request.post('/admin/job', data) },
    update(id, data) { return request.put(`/admin/job/${id}`, data) },
    delete(id) { return request.delete(`/admin/job/${id}`) },
    publish(id) { return request.put(`/admin/job/${id}/publish`) },
    pause(id) { return request.put(`/admin/job/${id}/pause`) },
    stats() { return request.get('/admin/job/stats') }
  }
}

export const teacherApi = {
  getMyClass() { return request.get('/teacher/my-class') },
  getStudents(params) { return request.get('/teacher/students', { params }) },
  getClasses() { return request.get('/teacher/classes') },
  getEmploymentStats(graduationYear) {
    const params = graduationYear ? { graduationYear } : {}
    return request.get('/teacher/employment-stats', { params })
  },
  getClassEmploymentStats(classId, graduationYear) {
    const params = graduationYear ? { graduationYear } : {}
    return request.get(`/teacher/class-employment-stats/${classId}`, { params })
  },
  getEmploymentDetail(classId, page = 1, size = 20) {
    return request.get('/teacher/employment-detail', { params: { classId, page, size } })
  },
  getEmploymentDetailAll(classId) {
    return request.get('/teacher/employment-detail-all', { params: { classId } })
  },
  getPendingEmployments() { return request.get('/teacher/employment-pending') },
  getEmploymentHistory() { return request.get('/teacher/employment-history') },
  auditEmployment(id, action, remark) {
    return request.put(`/teacher/employment/${id}/audit`, null, { params: { action, remark } })
  },
  getConversations() { return request.get('/teacher/conversations') },
  createConversation(data) { return request.post('/teacher/conversation', data) },
  getConversation(id) { return request.get(`/teacher/conversation/${id}`) },
  updateConversation(id, data) { return request.put(`/teacher/conversation/${id}`, data) },
  deleteConversation(id) { return request.delete(`/teacher/conversation/${id}`) },
  getPermissionRequests() { return request.get('/teacher/permission-requests') },
  getPermissionHistory() { return request.get('/teacher/permission-history') },
  auditPermissionRequest(id, action, remark) {
    return request.put(`/teacher/permission-request/${id}/audit`, null, { params: { action, remark } })
  },
  // 就业提醒相关
  getEmploymentReminders() { return request.get('/teacher/employment-reminders') },
  sendEmploymentReminder(data) { return request.post('/teacher/employment-reminder', data) },
  markReminderAsRead(id) { return request.put(`/teacher/employment-reminder/${id}/read`) },
  getUnreadReminderCount() { return request.get('/teacher/employment-reminder/unread-count') },
  // 企业入驻审核
  getPendingCompanyAuths() { return request.get('/teacher/company-auth-pending') },
  auditCompanyAuth(id, action, remark) {
    return request.put(`/teacher/company-auth/${id}/audit`, null, { params: { action, remark } })
  },
  // 三方协议
  getAgreements(params) { return request.get('/teacher/agreements', { params }) },
  signAgreement(id) { return request.put(`/teacher/agreement/${id}/sign`) },
  getAgreementStats() { return request.get('/teacher/agreement-stats') },
  // 工作台聚合数据
  getHomeStats() { return request.get('/teacher/home-stats') }
}

export const companyApi = {
  getHomeStats() { return request.get('/company/home-stats') },
  getProfile() { return request.get('/company/profile') },
  updateProfile(data) { return request.put('/company/profile', data) },
  getReceivedResumes(params) { return request.get('/company/resumes', { params }) },
  updateApplicationStatus(id, status, companyRemark) {
    return request.put('/company/application/' + id + '/status', null, { params: { status, companyRemark } })
  },
  // 职位管理
  getJobs(params) { return request.get('/company/jobs', { params }) },
  getJob(id) { return request.get('/company/job/' + id) },
  createJob(data) { return request.post('/company/job', data) },
  updateJob(id, data) { return request.put('/company/job/' + id, data) },
  publishJob(id) { return request.put('/company/job/' + id + '/publish') },
  pauseJob(id) { return request.put('/company/job/' + id + '/pause') },
  deleteJob(id) { return request.delete('/company/job/' + id) },
  // 面试管理
  getInterviews(params) { return request.get('/company/interviews', { params }) },
  createInterview(data) { return request.post('/company/interview', data) },
  cancelInterview(id) { return request.put('/company/interview/' + id + '/cancel') },
  deleteApplicationsBatch(ids) { return request.post('/company/applications/delete-batch', { ids }) },
  // 统计数据
  getStatistics(params) { return request.get('/company/statistics', { params }) },
  // 重新申请入驻
  reApply(data) { return request.post('/company/reapply', data) },
  // Offer管理
  getOffers(params) { return request.get('/company/offers', { params }) },
  sendOffer(data) { return request.post('/company/offer', data) },
  withdrawOffer(id) { return request.put('/company/offer/' + id + '/withdraw') },
  // 面试记录
  addInterviewRecord(data) { return request.post('/company/interview-record', data) },
  // 三方协议
  getAgreements(params) { return request.get('/company/agreements', { params }) },
  signAgreement(id) { return request.put('/company/agreement/' + id + '/sign') }
}

export const profileApi = {
  getProfile() {
    return request.get('/profile')
  },
  updateProfile(data) {
    return request.put('/profile', data)
  },
  changePassword(oldPassword, newPassword) {
    return request.put('/profile/password', { oldPassword, newPassword })
  },
  changeUsername(username) {
    return request.put('/profile/username', { username })
  },
  uploadAvatar(formData) {
    return request.post('/profile/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
