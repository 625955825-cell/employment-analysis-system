import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/company-register',
    name: 'CompanyRegister',
    component: () => import('@/views/login/company-register.vue'),
    meta: { title: '企业入驻', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    children: [
      {
        path: '', redirect: to => {
          const role = localStorage.getItem('userInfo')
          if (!role) return '/login'
          try {
            const info = JSON.parse(role)
            const roleMap = {
              'student': '/student/home',
              'class_teacher': '/teacher/home',
              'dept_teacher': '/teacher/home',
              'company': '/company/home',
              'admin': '/admin/home',
              'employment_staff': '/data/home'
            }
            return roleMap[info.role] || '/login'
          } catch { return '/login' }
        }
      },
      {
        path: 'student',
        component: () => import('@/views/layout/Layout.vue'),
        redirect: '/student/home',
        meta: { title: '学生端' },
        children: [
          { path: 'home', name: 'StudentHome', component: () => import('@/views/student/home.vue'), meta: { title: '个人首页', roles: ['student'] } },
          { path: 'profile', name: 'StudentProfile', component: () => import('@/views/student/profile.vue'), meta: { title: '个人信息', roles: ['student'] } },
          { path: 'resume', name: 'ResumeList', component: () => import('@/views/student/resume/list.vue'), meta: { title: '简历管理', roles: ['student'] } },
          { path: 'resume/create', name: 'ResumeCreate', component: () => import('@/views/student/resume/form.vue'), meta: { title: '创建简历', roles: ['student'] } },
          { path: 'resume/edit/:id', name: 'ResumeEdit', component: () => import('@/views/student/resume/form.vue'), meta: { title: '编辑简历', roles: ['student'] } },
          { path: 'job-search', name: 'JobSearch', component: () => import('@/views/student/job-search.vue'), meta: { title: '职位搜索', roles: ['student'] } },
          { path: 'job-detail/:id', name: 'JobDetail', component: () => import('@/views/student/job-detail.vue'), meta: { title: '职位详情', roles: ['student'] } },
          { path: 'applications', name: 'Applications', component: () => import('@/views/student/applications.vue'), meta: { title: '投递记录', roles: ['student'] } },
          { path: 'interviews', name: 'Interviews', component: () => import('@/views/student/interviews.vue'), meta: { title: '面试邀约', roles: ['student'] } },
          { path: 'offers', name: 'StudentOffers', component: () => import('@/views/student/offers.vue'), meta: { title: '我的Offer', roles: ['student'] } },
          { path: 'agreements', name: 'StudentAgreements', component: () => import('@/views/student/agreements.vue'), meta: { title: '三方协议', roles: ['student'] } },
          { path: 'favorites', name: 'Favorites', component: () => import('@/views/student/favorites.vue'), meta: { title: '我的收藏', roles: ['student'] } },
          { path: 'employment', name: 'Employment', component: () => import('@/views/student/employment/index.vue'), meta: { title: '就业去向', roles: ['student'] } },
          { path: 'recommendation', name: 'Recommendation', component: () => import('@/views/student/recommendation.vue'), meta: { title: '职位推荐', roles: ['student'] } },
          { path: 'data-permission', name: 'DataPermission', component: () => import('@/views/student/data-permission.vue'), meta: { title: '数据查看申请', roles: ['student'] } },
          { path: 'messages', name: 'StudentMessages', component: () => import('@/views/student/messages.vue'), meta: { title: '消息通知', roles: ['student'] } }
        ]
      },
      {
        path: 'teacher',
        component: () => import('@/views/layout/Layout.vue'),
        redirect: '/teacher/home',
        meta: { title: '老师端' },
        children: [
          { path: 'home', name: 'TeacherHome', component: () => import('@/views/teacher/home.vue'), meta: { title: '工作台', roles: ['class_teacher', 'dept_teacher'] } },
          { path: 'profile', name: 'TeacherProfile', component: () => import('@/views/sysadmin/profile.vue'), meta: { title: '个人信息', roles: ['class_teacher', 'dept_teacher'] } },
          { path: 'students', name: 'TeacherStudents', component: () => import('@/views/teacher/students.vue'), meta: { title: '学生列表', roles: ['class_teacher'] } },
          { path: 'dept-students', name: 'DeptStudents', component: () => import('@/views/teacher/dept-students.vue'), meta: { title: '各班人数', roles: ['dept_teacher'] } },
          { path: 'agreements', name: 'TeacherAgreements', component: () => import('@/views/teacher/agreements.vue'), meta: { title: '三方协议', roles: ['class_teacher', 'dept_teacher'] } },
          { path: 'audit', name: 'TeacherAudit', component: () => import('@/views/teacher/audit.vue'), meta: { title: '材料审核', roles: ['class_teacher', 'dept_teacher'] } },
          { path: 'company-auth', name: 'CompanyAuth', component: () => import('@/views/teacher/company-auth.vue'), meta: { title: '企业入驻审核', roles: ['dept_teacher'] } },
          { path: 'conversation', name: 'TeacherConversation', component: () => import('@/views/teacher/conversation.vue'), meta: { title: '谈心谈话', roles: ['class_teacher'] } },
          { path: 'statistics', name: 'TeacherStatistics', component: () => import('@/views/teacher/statistics.vue'), meta: { title: '就业统计', roles: ['class_teacher'] } },
          { path: 'dept-statistics', name: 'DeptStatistics', component: () => import('@/views/teacher/dept-statistics.vue'), meta: { title: '就业统计', roles: ['dept_teacher'] } },
          { path: 'class-management', name: 'ClassManagement', component: () => import('@/views/teacher/class-management.vue'), meta: { title: '班级管理', roles: ['dept_teacher'] } },
          { path: 'data-approval', name: 'DataApproval', component: () => import('@/views/teacher/data-approval.vue'), meta: { title: '数据查看审批', roles: ['class_teacher', 'dept_teacher'] } }
        ]
      },
      {
        path: 'admin',
        component: () => import('@/views/layout/Layout.vue'),
        redirect: '/admin/home',
        meta: { title: '管理端' },
        children: [
          { path: 'home', name: 'AdminHome', component: () => import('@/views/admin/home.vue'), meta: { title: '管理首页', roles: ['admin'] } },
          { path: 'profile', name: 'AdminProfile', component: () => import('@/views/sysadmin/profile.vue'), meta: { title: '个人信息', roles: ['admin'] } },
          { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/users.vue'), meta: { title: '用户管理', roles: ['admin'] } },
          { path: 'basics', name: 'AdminBasics', component: () => import('@/views/admin/basics.vue'), meta: { title: '基础数据管理', roles: ['admin'] } },
          { path: 'notices', name: 'AdminNotices', component: () => import('@/views/admin/notices.vue'), meta: { title: '公告管理', roles: ['admin'] } },
          { path: 'accounts', name: 'AdminAccounts', component: () => import('@/views/sysadmin/accounts.vue'), meta: { title: '账号管理', roles: ['admin'] } },
          { path: 'roles', name: 'AdminRoles', component: () => import('@/views/sysadmin/roles.vue'), meta: { title: '角色权限', roles: ['admin'] } },
          { path: 'logs', name: 'AdminLogs', component: () => import('@/views/sysadmin/logs.vue'), meta: { title: '日志审计', roles: ['admin'] } }
        ]
      },
      {
        path: 'company',
        component: () => import('@/views/layout/Layout.vue'),
        redirect: '/company/home',
        meta: { title: '企业端' },
        children: [
          { path: 'home', name: 'CompanyHome', component: () => import('@/views/company/home.vue'), meta: { title: '企业首页', roles: ['company'] } },
          { path: 'profile', name: 'CompanyProfilePage', component: () => import('@/views/company/profile.vue'), meta: { title: '个人信息', roles: ['company'] } },
          { path: 'jobs', name: 'CompanyJobs', component: () => import('@/views/company/jobs.vue'), meta: { title: '职位管理', roles: ['company'] } },
          { path: 'resumes', name: 'CompanyResumes', component: () => import('@/views/company/resumes.vue'), meta: { title: '收到的简历', roles: ['company'] } },
          { path: 'interviews', name: 'CompanyInterviews', component: () => import('@/views/company/interviews.vue'), meta: { title: '面试管理', roles: ['company'] } },
          { path: 'offers', name: 'CompanyOffers', component: () => import('@/views/company/offers.vue'), meta: { title: 'Offer管理', roles: ['company'] } },
          { path: 'agreements', name: 'CompanyAgreements', component: () => import('@/views/company/agreements.vue'), meta: { title: '三方协议', roles: ['company'] } },
          { path: 'statistics', name: 'CompanyStatistics', component: () => import('@/views/company/statistics.vue'), meta: { title: '数据统计', roles: ['company'] } }
        ]
      },
      {
        path: 'data',
        component: () => import('@/views/layout/Layout.vue'),
        redirect: '/data/home',
        meta: { title: '数据分析端' },
        children: [
          { path: 'home', name: 'DataHome', component: () => import('@/views/data/home.vue'), meta: { title: '分析首页', roles: ['employment_staff'] } },
          { path: 'profile', name: 'DataProfile', component: () => import('@/views/sysadmin/profile.vue'), meta: { title: '个人信息', roles: ['employment_staff'] } },
          { path: 'employment-rate', name: 'EmploymentRate', component: () => import('@/views/data/employment-rate.vue'), meta: { title: '就业率分析', roles: ['employment_staff'] } },
          { path: 'recommendation', name: 'DataRecommendation', component: () => import('@/views/data/recommendation.vue'), meta: { title: '推荐算法', roles: ['employment_staff'] } },
          { path: 'spider', name: 'DataSpider', component: () => import('@/views/data/spider.vue'), meta: { title: '爬虫管理', roles: ['employment_staff'] } }
        ]
      }
    ]
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  NProgress.start()
  document.title = to.meta.title ? `${to.meta.title} - 高校就业数据综合分析平台` : '高校就业数据综合分析平台'

  const userStore = useUserStore()
  const token = userStore.token

  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (token && (to.path === '/login' || to.path === '/register')) {
    next('/')
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
