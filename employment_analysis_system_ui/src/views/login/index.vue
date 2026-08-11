<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-dots"></div>
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
      <div class="bg-circle bg-circle-4"></div>
    </div>

    <div class="login-card">
      <!-- 左侧品牌区域 -->
      <div class="login-left">
        <!-- 顶部轻量品牌标识 -->
        <div class="brand-badge">
          <img class="brand-logo" :src="lxLogo" alt="NX" />
          <span class="brand-tagline">数据驱动就业 · 智能创造价值</span>
        </div>

        <!-- 主标题区 -->
        <div class="brand-headline">
          <h1>高校就业数据综合分析平台</h1>
          <p class="subtitle">Employment Data Analysis Platform</p>
          <p class="description">
            整合招聘与就业数据，提供智能分析与可视化洞察，
            <br />助力高校、企业、师生高效协同，共创就业新价值。
          </p>
        </div>

        <!-- 功能亮点 -->
        <div class="features">
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">智能职位推荐</span>
              <span class="feature-desc">AI 驱动精准匹配</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">就业率分析</span>
              <span class="feature-desc">多维数据洞察</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">多角色协同</span>
              <span class="feature-desc">高校企业联动</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">数据导出</span>
              <span class="feature-desc">Excel / PDF 报告</span>
            </div>
          </div>
        </div>

        <!-- Hero 主视觉区域 -->
        <div class="hero-section">
          <div class="hero-glow"></div>
          <img class="hero-image" :src="heroImage" alt="高校就业数据分析平台" />
        </div>

        <!-- 底部签名 -->
        <div class="brand-footer"></div>
      </div>

      <!-- 右侧登录表单区域 -->
      <div class="login-right">
        <!-- 登录表单卡片 -->
        <div class="login-panel">
          <div class="login-title-wrap">
            <h2>用户登录</h2>
            <div class="title-line"></div>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                size="large"
                prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="login-btn"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="login-links">
            <span>还没有账号？</span>
            <router-link to="/register">立即注册</router-link>
            <span class="divider">|</span>
            <span>我是企业</span>
            <router-link to="/company-register" class="highlight">立即入驻</router-link>
          </div>
        </div>

        <!-- 快速登录区域 -->
        <div class="demo-section">
          <div class="demo-header">
            <span class="demo-label">快速登录</span>
            <span class="demo-hint">点击自动填入</span>
          </div>
          <div class="demo-pills">
            <div class="demo-pill" @click="fillDemo('admin', '123456')">
              <span class="demo-role">校级管理员</span>
              <span class="demo-creds">admin / 123456</span>
            </div>
            <div class="demo-pill" @click="fillDemo('datastaff', '123456')">
              <span class="demo-role">数据分析师</span>
              <span class="demo-creds">datastaff / 123456</span>
            </div>
            <div class="demo-pill" @click="fillDemo('bigdata571', '123456')">
              <span class="demo-role">院级老师</span>
              <span class="demo-creds">bigdata571 / 123456</span>
            </div>
            <div class="demo-pill" @click="fillDemo('dashuju3333', '123456')">
              <span class="demo-role">班级老师</span>
              <span class="demo-creds">dashuju3333 / 123456</span>
            </div>
            <div class="demo-pill" @click="fillDemo('20223333021', '123456')">
              <span class="demo-role">学生</span>
              <span class="demo-creds">20223333021 / 123456</span>
            </div>
            <div class="demo-pill" @click="fillDemo('hr0117', '123456')">
              <span class="demo-role">企业 HR</span>
              <span class="demo-creds">hr0117 / 123456</span>
            </div>
          </div>
          <p class="demo-tip">点击卡片可自动填充测试账号进行体验</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { DataAnalysis, Connection, User, Document } from '@element-plus/icons-vue'
import heroImage from '@/assets/login/hero.png'
import lxLogo from '@/assets/login/lx-logo.png'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await userStore.login(form)
    ElMessage.success('登录成功')
    const roleMap = {
      student: '/student/home',
      class_teacher: '/teacher/home',
      dept_teacher: '/teacher/home',
      admin: '/admin/home',
      company: '/company/home',
      employment_staff: '/data/home'
    }
    const redirectPath = roleMap[res.role] || '/student/home'
    router.push(redirectPath)
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function fillDemo(username, password) {
  form.username = username
  form.password = password
  ElMessage.success(`已填入账号: ${username}`)
}
</script>

<style scoped>
/* ===== 页面容器 ===== */
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 8% 15%, rgba(47, 107, 255, 0.07), transparent 26%),
    radial-gradient(circle at 92% 8%, rgba(64, 158, 255, 0.09), transparent 28%),
    radial-gradient(circle at 50% 80%, rgba(99, 179, 237, 0.04), transparent 30%),
    linear-gradient(145deg, #f7faff 0%, #eef4ff 50%, #ffffff 100%);
}

/* ===== 背景装饰 ===== */
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
}

.bg-circle-1 {
  width: 700px;
  height: 700px;
  background: radial-gradient(circle, rgba(47, 107, 255, 0.05), transparent 70%);
  top: -280px;
  left: -200px;
}

.bg-circle-2 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.04), transparent 70%);
  bottom: -200px;
  right: -180px;
}

.bg-circle-3 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(99, 179, 237, 0.03), transparent 70%);
  top: 50%;
  left: 20%;
  transform: translateY(-50%);
}

.bg-circle-4 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(47, 107, 255, 0.04), transparent 70%);
  bottom: 30%;
  left: 40%;
}

/* 点阵装饰 */
.bg-dots {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle, rgba(47, 107, 255, 0.12) 1px, transparent 1px);
  background-size: 32px 32px;
  opacity: 0.35;
}

/* ===== 主卡片 ===== */
.login-card {
  display: flex;
  width: min(1260px, 94vw);
  min-height: 740px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 28px;
  box-shadow:
    0 32px 96px rgba(39, 84, 160, 0.12),
    0 8px 32px rgba(39, 84, 160, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.8);
  overflow: hidden;
  position: relative;
  z-index: 1;
  backdrop-filter: blur(20px);
}

/* ===== 左侧品牌区域 ===== */
.login-left {
  flex: 0 0 58%;
  background: linear-gradient(160deg, #ffffff 0%, #f2f7ff 100%);
  padding: 40px 52px 32px;
  display: flex;
  flex-direction: column;
  position: relative;
  border-right: 1px solid rgba(229, 234, 243, 0.6);
}

/* 顶部轻量品牌标识 */
.brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px 10px 12px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(210, 228, 255, 0.8);
  border-radius: 50px;
  box-shadow:
    0 4px 16px rgba(47, 107, 255, 0.08),
    0 1px 4px rgba(47, 107, 255, 0.04);
  backdrop-filter: blur(12px);
  margin-bottom: 28px;
  align-self: flex-start;
  transition: box-shadow 0.25s ease, transform 0.25s ease;
}

.brand-badge:hover {
  box-shadow:
    0 8px 24px rgba(47, 107, 255, 0.13),
    0 2px 8px rgba(47, 107, 255, 0.06);
  transform: translateY(-1px);
}

.brand-logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
  flex-shrink: 0;
  filter: drop-shadow(0 1px 4px rgba(47, 107, 255, 0.12));
}

.brand-tagline {
  font-size: 13px;
  color: #2f6bff;
  letter-spacing: 0.4px;
  white-space: nowrap;
  font-weight: 600;
}

/* 主标题区 */
.brand-headline {
  margin-bottom: 20px;
}

.brand-headline h1 {
  font-size: 32px;
  font-weight: 900;
  color: #0c2660;
  margin-bottom: 10px;
  line-height: 1.18;
  letter-spacing: -0.8px;
}

.subtitle {
  font-size: 13px;
  color: #9ab5d6;
  letter-spacing: 1.2px;
  margin-bottom: 16px;
  font-weight: 500;
  text-transform: uppercase;
}

.description {
  font-size: 14.5px;
  color: #5f7090;
  line-height: 2;
}

/* ===== 功能亮点 ===== */
.features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e4ecf8;
  border-radius: 14px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: default;
}

.feature-item:hover {
  border-color: #3d7fff;
  background: linear-gradient(135deg, #f5f9ff 0%, #eef3ff 100%);
  box-shadow: 0 4px 16px rgba(47, 107, 255, 0.1);
  transform: translateY(-3px);
}

.feature-icon {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #eef5ff 0%, #e3ecff 100%);
  border-radius: 10px;
  flex-shrink: 0;
  transition: background 0.25s ease;
}

.feature-item:hover .feature-icon {
  background: linear-gradient(145deg, #dbe8ff 0%, #c8dcff 100%);
}

.feature-icon .el-icon {
  font-size: 18px;
  color: #2f6bff;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-title {
  font-size: 13px;
  font-weight: 600;
  color: #2d4a72;
}

.feature-desc {
  font-size: 11px;
  color: #8fa8cc;
}

/* ===== Hero 视觉区域 ===== */
.hero-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  position: relative;
  min-height: 360px;
}

.hero-glow {
  position: absolute;
  width: 640px;
  height: 420px;
  background: radial-gradient(ellipse, rgba(47, 107, 255, 0.13) 0%, transparent 72%);
  border-radius: 50%;
  pointer-events: none;
}

.hero-image {
  width: 580px;
  max-width: 100%;
  height: auto;
  object-fit: contain;
  position: relative;
  z-index: 1;
  animation: float 6s ease-in-out infinite;
  filter: drop-shadow(0 20px 48px rgba(47, 107, 255, 0.18));
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-14px); }
}

/* ===== 底部签名 ===== */
.brand-footer {
  height: 1px;
  margin-top: 8px;
}

/* ===== 右侧登录区域 ===== */
.login-right {
  flex: 0 0 42%;
  background: #ffffff;
  padding: 44px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 20px;
}

/* 登录面板 */
.login-panel {
  text-align: center;
}

.login-title-wrap {
  margin-bottom: 22px;
}

.login-title-wrap h2 {
  font-size: 22px;
  font-weight: 700;
  color: #0c2660;
  margin-bottom: 10px;
}

.title-line {
  width: 36px;
  height: 3px;
  background: linear-gradient(90deg, #2f6bff, #60a5fa);
  border-radius: 4px;
  margin: 0 auto;
}

/* 登录表单 */
.login-form {
  max-width: 390px;
  margin: 0 auto;
  width: 100%;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 3px 16px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px #d8e5f5 inset;
  transition: all 0.25s ease;
  background: #fafcff;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1.5px #a8c4f8 inset;
  background: #ffffff;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #2f6bff inset !important;
  background: #ffffff;
}

.login-form :deep(.el-input__inner) {
  height: 46px;
  font-size: 15px;
  color: #2d4a72;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #2f6bff 0%, #1d4ed8 100%);
  box-shadow: 0 6px 24px rgba(47, 107, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 2px;
}

.login-btn:hover {
  background: linear-gradient(135deg, #4d7fff 0%, #2f6bff 100%);
  box-shadow: 0 10px 32px rgba(47, 107, 255, 0.4);
  transform: translateY(-2px);
}

.login-btn:active {
  transform: translateY(0);
}

/* 登录链接 */
.login-links {
  text-align: center;
  margin-top: 18px;
  font-size: 13px;
  color: #8fa8cc;
  letter-spacing: 0.2px;
}

.login-links a {
  color: #3d7fff;
  margin: 0 4px;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s;
}

.login-links a:hover {
  color: #1d4ed8;
}

.login-links .highlight {
  color: #2f6bff;
}

.divider {
  color: #c8d8ee;
  margin: 0 8px;
}

/* ===== 快速登录区域 ===== */
.demo-section {
  background: rgba(245, 249, 255, 0.65);
  border: 1px solid rgba(218, 232, 252, 0.8);
  border-radius: 18px;
  padding: 20px 22px;
}

.demo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.demo-label {
  font-size: 12px;
  font-weight: 700;
  color: #3d5a8a;
  letter-spacing: 0.5px;
}

.demo-hint {
  font-size: 10px;
  color: #7a9fcc;
  background: rgba(200, 222, 255, 0.6);
  padding: 2px 10px;
  border-radius: 20px;
}

.demo-pills {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.demo-pill {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  background: #ffffff;
  border: 1px solid rgba(220, 234, 252, 0.7);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.demo-pill:hover {
  border-color: #3d7fff;
  background: linear-gradient(135deg, #f0f6ff 0%, #e8f0ff 100%);
  box-shadow: 0 4px 16px rgba(47, 107, 255, 0.12);
  transform: translateY(-3px);
}

.demo-pill:active {
  background: #e0ebff;
  transform: translateY(-1px);
}

.demo-role {
  font-weight: 700;
  color: #2d4a72;
  font-size: 12px;
  margin-bottom: 3px;
}

.demo-creds {
  color: #8fa8cc;
  font-size: 10px;
}

.demo-tip {
  text-align: center;
  font-size: 10px;
  color: #a8bfd8;
  margin: 10px 0 0;
  letter-spacing: 0.3px;
}

/* ===== 响应式适配 ===== */
@media (max-width: 1100px) {
  .login-card {
    flex-direction: column;
    min-height: auto;
    max-width: 540px;
  }

  .login-left {
    flex: none;
    padding: 40px 44px 36px;
    border-right: none;
    border-bottom: 1px solid rgba(229, 234, 243, 0.6);
  }

  .features {
    grid-template-columns: repeat(4, 1fr);
  }

  .hero-section {
    min-height: 220px;
  }

  .hero-image {
    width: 380px;
  }

  .login-right {
    flex: none;
    padding: 40px 44px;
  }

  .demo-pills {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .login-container {
    padding: 12px;
  }

  .login-card {
    border-radius: 20px;
  }

  .login-left {
    padding: 28px 24px;
  }

  .brand-headline h1 {
    font-size: 22px;
  }

  .features {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .feature-item {
    padding: 11px 12px;
  }

  .hero-section {
    min-height: 160px;
  }

  .hero-image {
    width: 280px;
  }

  .login-right {
    padding: 32px 24px;
  }

  .demo-pills {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
