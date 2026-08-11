<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <!-- Logo 区域 -->
      <div class="sidebar-logo">
        <img class="logo-img" :src="nxLogo" alt="NX" />
        <div v-if="!isCollapse" class="logo-text">
          <span class="logo-title">高校就业平台</span>
          <span class="logo-sub">数据驱动就业</span>
        </div>
        <el-icon v-else class="logo-collapse-icon"><Monitor /></el-icon>
      </div>

      <!-- 分割线 -->
      <div class="sidebar-divider"></div>

      <!-- 菜单 -->
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :router="true"
        class="aside-menu"
        :class="{ 'is-collapse': isCollapse }"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主体区域 -->
    <el-container class="main-container">
      <!-- 顶部 Header -->
      <el-header class="header">
        <div class="header-left">
          <div class="collapse-btn" @click="isCollapse = !isCollapse">
            <el-icon size="18">
              <Expand v-if="isCollapse" />
              <Fold v-else />
            </el-icon>
          </div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute.meta?.title">{{ currentRoute.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <span class="user-info">
              <el-avatar :size="32" :src="userInfo.avatar" class="user-avatar">
                {{ userInfo.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username">{{ userInfo.realName || userInfo.username }}</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人信息
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  Fold, Expand, Monitor, HomeFilled, User, Document, Search, Collection,
  Tickets, DataLine, Bell, Setting, DataAnalysis, FolderOpened,
  Key, OfficeBuilding, Lock, Memo, MagicStick, ArrowDown, SwitchButton
} from '@element-plus/icons-vue'
import nxLogo from '@/assets/login/lx-logo.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const currentRoute = route

const userInfo = computed(() => userStore.userInfo || {})

const studentMenus = [
  { path: '/student/home', title: '个人首页', icon: HomeFilled },
  { path: '/student/profile', title: '个人信息', icon: User },
  { path: '/student/resume', title: '简历管理', icon: Document },
  { path: '/student/job-search', title: '职位搜索', icon: Search },
  { path: '/student/applications', title: '投递记录', icon: Tickets },
  { path: '/student/favorites', title: '我的收藏', icon: Collection },
  { path: '/student/interviews', title: '面试邀约', icon: Tickets },
  { path: '/student/offers', title: '我的Offer', icon: Document },
  { path: '/student/agreements', title: '三方协议', icon: Tickets },
  { path: '/student/employment', title: '就业去向', icon: DataLine },
  { path: '/student/data-permission', title: '数据查看申请', icon: DataAnalysis },
  { path: '/student/recommendation', title: '职位推荐', icon: MagicStick },
  { path: '/student/messages', title: '消息通知', icon: Bell }
]

const teacherMenus = [
  { path: '/teacher/home', title: '工作台', icon: HomeFilled },
  { path: '/teacher/profile', title: '个人信息', icon: User },
  { path: '/teacher/students', title: '学生列表', icon: User },
  { path: '/teacher/agreements', title: '三方协议', icon: Tickets },
  { path: '/teacher/audit', title: '材料审核', icon: Document },
  { path: '/teacher/conversation', title: '谈心谈话', icon: Tickets },
  { path: '/teacher/statistics', title: '就业统计', icon: DataLine },
  { path: '/teacher/data-approval', title: '数据审批', icon: Setting }
]

const deptTeacherMenus = [
  { path: '/teacher/home', title: '工作台', icon: HomeFilled },
  { path: '/teacher/profile', title: '个人信息', icon: User },
  { path: '/teacher/dept-students', title: '各班人数', icon: User },
  { path: '/teacher/dept-statistics', title: '就业统计', icon: DataLine },
  { path: '/teacher/class-management', title: '班级管理', icon: Tickets },
  { path: '/teacher/agreements', title: '三方协议', icon: Tickets },
  { path: '/teacher/audit', title: '材料审核', icon: Document },
  { path: '/teacher/company-auth', title: '企业入驻审核', icon: OfficeBuilding },
  { path: '/teacher/data-approval', title: '数据查看审批', icon: Setting }
]

const adminMenus = [
  { path: '/admin/home', title: '管理首页', icon: HomeFilled },
  { path: '/admin/profile', title: '个人信息', icon: User },
  { path: '/admin/users', title: '用户管理', icon: User },
  { path: '/admin/accounts', title: '账号管理', icon: Key },
  { path: '/admin/basics', title: '基础数据管理', icon: FolderOpened },
  { path: '/admin/notices', title: '公告管理', icon: Bell },
  { path: '/admin/roles', title: '角色权限', icon: Lock },
  { path: '/admin/logs', title: '日志审计', icon: Memo }
]

const companyMenus = [
  { path: '/company/home', title: '企业首页', icon: HomeFilled },
  { path: '/company/profile', title: '个人信息', icon: User },
  { path: '/company/jobs', title: '职位管理', icon: Document },
  { path: '/company/resumes', title: '收到的简历', icon: Tickets },
  { path: '/company/interviews', title: '面试管理', icon: Tickets },
  { path: '/company/offers', title: 'Offer管理', icon: Document },
  { path: '/company/agreements', title: '三方协议', icon: Tickets },
  { path: '/company/statistics', title: '数据统计', icon: DataLine }
]

const dataMenus = [
  { path: '/data/home', title: '分析首页', icon: HomeFilled },
  { path: '/data/profile', title: '个人信息', icon: User },
  { path: '/data/employment-rate', title: '就业率分析', icon: DataAnalysis },
  { path: '/data/recommendation', title: '推荐算法', icon: DataLine },
  { path: '/data/spider', title: '爬虫管理', icon: Setting }
]

const menuItems = computed(() => {
  const role = userInfo.value?.role
  switch (role) {
    case 'student': return studentMenus.map(m => ({ ...m, icon: m.icon }))
    case 'class_teacher':
    case 'teacher': return teacherMenus.map(m => ({ ...m, icon: m.icon }))
    case 'dept_teacher': return deptTeacherMenus.map(m => ({ ...m, icon: m.icon }))
    case 'admin': return adminMenus.map(m => ({ ...m, icon: m.icon }))
    case 'company': return companyMenus.map(m => ({ ...m, icon: m.icon }))
    case 'employment_staff': return dataMenus.map(m => ({ ...m, icon: m.icon }))
    default: return []
  }
})

const activeMenu = computed(() => route.path)

function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    })
  } else if (command === 'profile') {
    const role = userStore.role
    const profileMap = {
      'student': '/student/profile',
      'class_teacher': '/teacher/profile',
      'dept_teacher': '/teacher/profile',
      'teacher': '/teacher/profile',
      'admin': '/admin/profile',
      'company': '/company/profile',
      'employment_staff': '/data/profile'
    }
    router.push(profileMap[role] || '/student/home')
  }
}

onMounted(() => {
  if (userStore.token && !userStore.userInfo?.username) {
    userStore.getUserInfo()
  }
})
</script>

<style scoped>
/* ===== 整体布局 ===== */
.layout-container {
  height: 100vh;
  overflow: hidden;
}

/* ===== 侧边栏 ===== */
.aside {
  background: linear-gradient(180deg, #ffffff 0%, #f4f8ff 100%);
  border-right: 1px solid #e5eaf3;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ===== Logo 区域 ===== */
.sidebar-logo {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid #edf3fc;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.logo-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
  flex-shrink: 0;
  filter: drop-shadow(0 2px 6px rgba(47, 107, 255, 0.15));
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
}

.logo-title {
  font-size: 15px;
  font-weight: 800;
  color: #0c2660;
  letter-spacing: 0.3px;
  line-height: 1.2;
  white-space: nowrap;
}

.logo-sub {
  font-size: 11px;
  color: #7090c0;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.logo-collapse-icon {
  font-size: 22px;
  color: #2f6bff;
  margin: 0 auto;
}

.sidebar-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #e0e8f8, transparent);
  flex-shrink: 0;
}

/* ===== 菜单 ===== */
.aside-menu {
  border-right: none;
  background: transparent;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
}

.aside-menu::-webkit-scrollbar {
  width: 4px;
}

.aside-menu::-webkit-scrollbar-track {
  background: transparent;
}

.aside-menu::-webkit-scrollbar-thumb {
  background: #d0dcf0;
  border-radius: 4px;
}

.aside-menu::-webkit-scrollbar-thumb:hover {
  background: #a8bfe0;
}

/* 覆盖 el-menu 样式 */
:deep(.el-menu) {
  background: transparent;
  border: none;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 46px;
  line-height: 46px;
  color: #5f6f8f;
  font-size: 14px;
  font-weight: 500;
  margin: 2px 10px;
  padding-left: 16px !important;
  padding-right: 16px;
  border-radius: 10px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 10px;
}

:deep(.el-menu-item .el-icon),
:deep(.el-sub-menu__title .el-icon) {
  font-size: 16px;
  color: #8aa0c8;
  flex-shrink: 0;
  transition: color 0.2s ease;
  min-width: 20px;
}

/* hover 状态 */
:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: #f0f6ff;
  color: #2f6bff;
  transform: translateX(2px);
}

:deep(.el-menu-item:hover .el-icon),
:deep(.el-sub-menu__title:hover .el-icon) {
  color: #2f6bff;
}

/* 选中状态 */
:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #eaf2ff 0%, #f0f6ff 100%);
  color: #2f6bff;
  font-weight: 600;
  position: relative;
  border: 1px solid rgba(47, 107, 255, 0.15);
}

:deep(.el-menu-item.is-active .el-icon) {
  color: #2f6bff;
}

/* 选中状态左侧蓝色竖线 */
:deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: linear-gradient(180deg, #2f6bff, #60a5fa);
  border-radius: 0 3px 3px 0;
}

/* 子菜单 */
:deep(.el-sub-menu .el-menu-item) {
  height: 42px;
  line-height: 42px;
  padding-left: 50px !important;
  font-size: 13px;
  margin: 2px 10px;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  transform: translateX(2px);
}

:deep(.el-sub-menu .el-menu-item.is-active) {
  background: linear-gradient(135deg, #eaf2ff 0%, #f0f6ff 100%);
  color: #2f6bff;
  font-weight: 600;
  border: 1px solid rgba(47, 107, 255, 0.15);
}

:deep(.el-sub-menu .el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: linear-gradient(180deg, #2f6bff, #60a5fa);
  border-radius: 0 3px 3px 0;
}

/* 子菜单展开箭头 */
:deep(.el-sub-menu__icon-arrow) {
  color: #8aa0c8;
  transition: transform 0.3s ease;
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #2f6bff;
  font-weight: 600;
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title .el-icon) {
  color: #2f6bff;
}

/* 折叠状态 */
:deep(.el-menu--collapse) {
  width: 64px;
}

:deep(.el-menu--collapse .el-menu-item),
:deep(.el-menu--collapse .el-sub-menu__title) {
  padding-left: 20px !important;
  justify-content: center;
}

:deep(.el-menu--collapse .el-menu-item span),
:deep(.el-menu--collapse .el-sub-menu__title span) {
  display: none;
}

:deep(.el-menu--collapse .el-menu-item::before),
:deep(.el-menu--collapse .el-sub-menu__title::before) {
  display: none;
}

:deep(.el-menu--collapse .el-menu-item.is-active)::before {
  display: none;
}

/* ===== 主体容器 ===== */
.main-container {
  flex-direction: column;
  background: #f3f6fb;
  overflow: hidden;
}

/* ===== Header ===== */
.header {
  height: 64px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 8px rgba(47, 107, 255, 0.06);
  border-bottom: 1px solid #edf3fc;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #5f6f8f;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  background: #f0f6ff;
  color: #2f6bff;
}

:deep(.el-breadcrumb__inner) {
  color: #5f6f8f;
  font-size: 13px;
}

:deep(.el-breadcrumb__inner.is-link:hover) {
  color: #2f6bff;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.user-info:hover {
  background: #f0f6ff;
}

.user-avatar {
  border: 2px solid #e0ebff;
}

.username {
  font-size: 14px;
  color: #2d4a72;
  font-weight: 500;
}

.user-arrow {
  font-size: 12px;
  color: #8aa0c8;
}

/* 下拉菜单样式 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #5f6f8f;
  padding: 10px 16px;
}

:deep(.el-dropdown-menu__item:hover) {
  background: #f0f6ff;
  color: #2f6bff;
}

/* ===== 主内容区 ===== */
.main {
  background: #f3f6fb;
  overflow-y: auto;
  padding: 20px;
}

.main::-webkit-scrollbar {
  width: 6px;
}

.main::-webkit-scrollbar-track {
  background: #f3f6fb;
}

.main::-webkit-scrollbar-thumb {
  background: #d0dcf0;
  border-radius: 3px;
}

.main::-webkit-scrollbar-thumb:hover {
  background: #a8bfe0;
}
</style>
