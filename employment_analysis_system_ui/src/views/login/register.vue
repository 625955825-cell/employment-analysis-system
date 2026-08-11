<template>
  <div class="auth-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-dots"></div>
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
      <div class="bg-circle bg-circle-4"></div>
    </div>

    <div class="auth-card">
      <!-- 左侧品牌区域 -->
      <div class="auth-left">
        <!-- 顶部品牌标识 -->
        <div class="brand-badge">
          <img class="brand-logo" :src="lxLogo" alt="NX" />
          <span class="brand-tagline">数据驱动就业 · 智能创造价值</span>
        </div>

        <!-- 主标题 -->
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
              <el-icon><User /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">学生求职服务</span>
              <span class="feature-desc">简历管理、职位搜索、智能推荐</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">多角色协同</span>
              <span class="feature-desc">学生、教师、企业高效联动</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">就业数据跟踪</span>
              <span class="feature-desc">就业去向、协议审核、就业统计</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Lock /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">安全账号体系</span>
              <span class="feature-desc">统一身份认证与权限管理</span>
            </div>
          </div>
        </div>

        <!-- Hero 图 -->
        <div class="hero-section">
          <div class="hero-glow"></div>
          <img class="hero-image" :src="heroImage" alt="高校就业数据分析平台" />
        </div>

        <div class="brand-footer"></div>
      </div>

      <!-- 右侧注册表单区域 -->
      <div class="auth-right">
        <div class="form-panel">
          <div class="form-title-wrap">
            <h2>用户注册</h2>
            <div class="title-line"></div>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" class="auth-form" size="large">
            <el-form-item prop="role">
              <el-select v-model="form.role" placeholder="选择注册身份" style="width: 100%;" @change="onRoleChange">
                <el-option label="学生" value="student">
                  <div class="role-option">
                    <el-icon><User /></el-icon>
                    <span>学生</span>
                  </div>
                </el-option>
                <el-option label="班主任" value="class_teacher">
                  <div class="role-option">
                    <el-icon><Avatar /></el-icon>
                    <span>班主任</span>
                  </div>
                </el-option>
                <el-option label="院级老师" value="dept_teacher">
                  <div class="role-option">
                    <el-icon><School /></el-icon>
                    <span>院级老师</span>
                  </div>
                </el-option>
                <el-option label="企业" value="company">
                  <div class="role-option">
                    <el-icon><OfficeBuilding /></el-icon>
                    <span>企业入驻</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <div class="form-row">
              <el-form-item prop="username">
                <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
              </el-form-item>
              <el-form-item prop="realName">
                <el-input v-model="form.realName" placeholder="真实姓名" prefix-icon="UserFilled" />
              </el-form-item>
            </div>

            <div class="form-row">
              <el-form-item prop="password">
                <el-input v-model="form.password" type="password" placeholder="设置密码" prefix-icon="Lock" show-password />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password />
              </el-form-item>
            </div>

            <el-form-item v-if="form.role !== 'student'" prop="invitationCode">
              <el-input v-model="form.invitationCode" placeholder="注册码（必填）" prefix-icon="Key" />
            </el-form-item>

            <div class="form-row">
              <el-form-item prop="deptId">
                <el-select v-model="form.deptId" placeholder="选择学院" style="width: 100%;" @change="handleDeptChange">
                  <el-option v-for="dept in depts" :key="dept.id" :label="dept.deptName" :value="dept.id" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="form.role !== 'dept_teacher'" prop="majorId">
                <el-select v-model="form.majorId" placeholder="选择专业" style="width: 100%;" :disabled="!form.deptId" @change="handleMajorChange">
                  <el-option v-for="major in majors" :key="major.id" :label="major.majorName" :value="major.id" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item v-if="form.role === 'student' || form.role === 'class_teacher'" prop="classId">
              <el-select v-model="form.classId" placeholder="选择班级" style="width: 100%;" :disabled="!form.majorId">
                <el-option v-for="cls in classes" :key="cls.id" :label="cls.className" :value="cls.id" />
              </el-select>
            </el-form-item>

            <div class="form-row">
              <el-form-item v-if="form.role === 'student'" prop="studentNo">
                <el-input v-model="form.studentNo" placeholder="学号" prefix-icon="Postcard" />
              </el-form-item>
              <el-form-item v-else prop="phone">
                <el-input v-model="form.phone" placeholder="联系电话" prefix-icon="Phone" />
              </el-form-item>
            </div>

            <el-form-item>
              <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister">注 册</el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>已有账号？</span>
            <router-link to="/login">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { dictApi } from '@/api'
import { DataLine, Connection, User, Avatar, School, OfficeBuilding, Lock, Key } from '@element-plus/icons-vue'
import lxLogo from '@/assets/login/lx-logo.png'
import heroImage from '@/assets/login/hero.png'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const depts = ref([])
const majors = ref([])
const classes = ref([])

const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  invitationCode: '',
  deptId: null,
  majorId: null,
  classId: null,
  studentNo: '',
  className: '',
  role: 'student',
  phone: ''
})

function onRoleChange() {
  if (form.role === 'company') {
    router.push('/company-register')
    return
  }
  form.deptId = null
  form.majorId = null
  form.classId = null
  form.className = ''
  form.studentNo = ''
  form.phone = ''
  form.invitationCode = ''
  majors.value = []
  classes.value = []
  formRef.value?.clearValidate()
}

const validateConfirmPassword = (rule, value, callback) => {
  // 学生注册时不显示确认密码字段，跳过验证
  if (form.role === 'student') {
    callback()
    return
  }
  // 其他角色必须填写且与密码一致
  if (!value || value.trim() === '') {
    callback(new Error('请确认密码'))
  } else if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const baseRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  // confirmPassword 的验证由 validator 统一处理，不再单独设置 required
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择学院', trigger: 'change' }]
}

const teacherRules = {
  invitationCode: [{ required: true, message: '请输入注册码', trigger: 'blur' }]
}

const classTeacherRules = {
  invitationCode: [{ required: true, message: '请输入注册码', trigger: 'blur' }],
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }]
}

const rules = computed(() => {
  const r = { ...baseRules }
  if (form.role === 'student') {
    Object.assign(r, { studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }] })
  } else if (form.role === 'class_teacher') {
    Object.assign(r, classTeacherRules)
  } else if (form.role === 'dept_teacher') {
    Object.assign(r, teacherRules)
  }
  return r
})

async function loadDepts() {
  try {
    const res = await dictApi.getDepartments()
    depts.value = res || []
  } catch (e) {
    console.error('加载院系失败:', e)
  }
}

async function handleDeptChange(deptId) {
  form.majorId = null
  form.classId = null
  classes.value = []
  if (deptId) {
    try {
      const res = await dictApi.getMajors(deptId)
      majors.value = res || []
    } catch (e) {
      console.error('加载专业失败:', e)
    }
  } else {
    majors.value = []
  }
}

async function handleMajorChange(majorId) {
  form.classId = null
  form.className = ''
  if (majorId) {
    try {
      const res = await dictApi.getClasses(majorId)
      classes.value = res || []
    } catch (e) {
      console.error('加载班级失败:', e)
    }
  } else {
    classes.value = []
  }
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const submitData = { ...form }
    const selectedClass = classes.value.find(c => c.id === form.classId)
    submitData.className = selectedClass ? selectedClass.className : ''
    submitData.invitationCode = form.invitationCode.trim()
    // 学生注册时，后端要求 confirmPassword 字段不能为空，这里自动填充
    if (form.role === 'student') {
      submitData.confirmPassword = form.password
    }
    // 其他角色的 confirmPassword 已经在表单中填写了，直接使用
    await userStore.register(submitData)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

loadDepts()
</script>

<style scoped>
/* ===== 页面容器 ===== */
.auth-container {
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

.bg-dots {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle, rgba(47, 107, 255, 0.12) 1px, transparent 1px);
  background-size: 32px 32px;
  opacity: 0.35;
}

/* ===== 主卡片 ===== */
.auth-card {
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
.auth-left {
  flex: 0 0 58%;
  background: linear-gradient(160deg, #ffffff 0%, #f2f7ff 100%);
  padding: 40px 52px 32px;
  display: flex;
  flex-direction: column;
  position: relative;
  border-right: 1px solid rgba(229, 234, 243, 0.6);
}

/* 顶部品牌标识 */
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
  margin-bottom: 20px;
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
  min-height: 300px;
}

.hero-glow {
  position: absolute;
  width: 560px;
  height: 360px;
  background: radial-gradient(ellipse, rgba(47, 107, 255, 0.12) 0%, transparent 72%);
  border-radius: 50%;
  pointer-events: none;
}

.hero-image {
  width: 520px;
  max-width: 100%;
  height: auto;
  object-fit: contain;
  position: relative;
  z-index: 1;
  animation: float 6s ease-in-out infinite;
  filter: drop-shadow(0 16px 40px rgba(47, 107, 255, 0.16));
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

/* ===== 右侧表单区域 ===== */
.auth-right {
  flex: 0 0 42%;
  background: #ffffff;
  padding: 44px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow-y: auto;
}

.form-panel {
  text-align: center;
  width: 100%;
  max-width: 440px;
  margin: 0 auto;
}

.form-title-wrap {
  margin-bottom: 22px;
}

.form-title-wrap h2 {
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

/* 表单 */
.auth-form {
  width: 100%;
  text-align: left;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.auth-form :deep(.el-input__wrapper) {
  padding: 3px 16px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px #d8e5f5 inset;
  transition: all 0.25s ease;
  background: #fafcff;
}

.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1.5px #a8c4f8 inset;
  background: #ffffff;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #2f6bff inset !important;
  background: #ffffff;
}

.auth-form :deep(.el-input__inner) {
  height: 44px;
  font-size: 14px;
  color: #2d4a72;
}

.auth-form :deep(.el-select .el-input__wrapper) {
  padding-right: 36px;
}

/* 双列布局 */
.form-row {
  display: flex;
  gap: 12px;
}

.form-row > .el-form-item {
  flex: 1;
}

/* 角色选择 */
.role-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #2f6bff 0%, #1d4ed8 100%);
  box-shadow: 0 6px 24px rgba(47, 107, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 2px;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #4d7fff 0%, #2f6bff 100%);
  box-shadow: 0 10px 32px rgba(47, 107, 255, 0.4);
  transform: translateY(-2px);
}

.submit-btn:active {
  transform: translateY(0);
}

/* 底部链接 */
.form-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #8fa8cc;
  letter-spacing: 0.2px;
}

.form-footer a {
  color: #3d7fff;
  margin-left: 4px;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s;
}

.form-footer a:hover {
  color: #1d4ed8;
}

/* ===== 响应式适配 ===== */
@media (max-width: 1100px) {
  .auth-card {
    flex-direction: column;
    min-height: auto;
    max-width: 580px;
  }

  .auth-left {
    flex: none;
    padding: 36px 40px 32px;
    border-right: none;
    border-bottom: 1px solid rgba(229, 234, 243, 0.6);
  }

  .features {
    grid-template-columns: repeat(4, 1fr);
  }

  .hero-section {
    min-height: 200px;
  }

  .hero-image {
    width: 360px;
  }

  .auth-right {
    flex: none;
    padding: 36px 40px;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }
}

@media (max-width: 640px) {
  .auth-container {
    padding: 12px;
  }

  .auth-card {
    border-radius: 20px;
  }

  .auth-left {
    padding: 28px 24px;
  }

  .brand-headline h1 {
    font-size: 24px;
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

  .auth-right {
    padding: 28px 24px;
  }
}
</style>
