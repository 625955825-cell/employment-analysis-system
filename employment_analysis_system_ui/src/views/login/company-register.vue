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
              <el-icon><Connection /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">精准人才匹配</span>
              <span class="feature-desc">面向高校毕业生智能推荐岗位</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">高效招聘流程</span>
              <span class="feature-desc">职位发布、简历处理、沟通反馈</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><School /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">校企深度合作</span>
              <span class="feature-desc">连接高校、学院、企业招聘需求</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><PieChart /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">数据招聘管理</span>
              <span class="feature-desc">招聘进展、岗位效果、人才画像</span>
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

      <!-- 右侧入驻表单区域 -->
      <div class="auth-right">
        <div class="form-panel">
          <div class="form-title-wrap">
            <h2>企业入驻</h2>
            <div class="title-line"></div>
          </div>

          <div class="form-scroll">
            <el-form ref="formRef" :model="form" :rules="rules" class="auth-form" size="default">
              <!-- 企业基本信息 -->
              <div class="form-section">
                <div class="section-title">企业基本信息</div>
                <el-form-item prop="companyName">
                  <el-input v-model="form.companyName" placeholder="企业名称" prefix-icon="OfficeBuilding" />
                </el-form-item>
                <el-form-item prop="deptId">
                  <el-select v-model="form.deptId" placeholder="请选择入驻学院" style="width: 100%;" clearable>
                    <el-option v-for="dept in deptList" :key="dept.id" :label="dept.deptName" :value="dept.id" />
                  </el-select>
                </el-form-item>
                <el-form-item prop="unifiedCreditCode">
                  <el-input v-model="form.unifiedCreditCode" placeholder="统一社会信用代码（可选）" prefix-icon="Postcard" />
                </el-form-item>
              </div>

              <!-- 联系人信息 -->
              <div class="form-section">
                <div class="section-title">联系人信息</div>
                <div class="form-row">
                  <el-form-item prop="contactPerson">
                    <el-input v-model="form.contactPerson" placeholder="联系人姓名" prefix-icon="UserFilled" />
                  </el-form-item>
                  <el-form-item prop="contactPhone">
                    <el-input v-model="form.contactPhone" placeholder="联系电话" prefix-icon="Phone" />
                  </el-form-item>
                </div>
                <el-form-item prop="contactEmail">
                  <el-input v-model="form.contactEmail" placeholder="联系邮箱（可选）" prefix-icon="Message" />
                </el-form-item>
              </div>

              <!-- 企业地址信息 -->
              <div class="form-section">
                <div class="section-title">企业地址信息</div>
                <div class="form-row">
                  <el-form-item prop="province">
                    <el-input v-model="form.province" placeholder="所在省份" prefix-icon="LocationInformation" />
                  </el-form-item>
                  <el-form-item prop="city">
                    <el-input v-model="form.city" placeholder="所在城市" prefix-icon="Location" />
                  </el-form-item>
                </div>
                <el-form-item prop="address">
                  <el-input v-model="form.address" placeholder="详细地址" prefix-icon="HomeFilled" />
                </el-form-item>
              </div>

              <!-- 企业属性信息 -->
              <div class="form-section">
                <div class="section-title">企业属性信息</div>
                <div class="form-row">
                  <el-form-item prop="industry">
                    <el-select v-model="form.industry" placeholder="所属行业" style="width: 100%;">
                      <el-option label="互联网/IT" value="互联网/IT" />
                      <el-option label="金融" value="金融" />
                      <el-option label="制造业" value="制造业" />
                      <el-option label="房地产" value="房地产" />
                      <el-option label="教育" value="教育" />
                      <el-option label="医疗健康" value="医疗健康" />
                      <el-option label="电子/半导体" value="电子/半导体" />
                      <el-option label="通信" value="通信" />
                      <el-option label="能源/化工" value="能源/化工" />
                      <el-option label="其他" value="其他" />
                    </el-select>
                  </el-form-item>
                  <el-form-item prop="scale">
                    <el-select v-model="form.scale" placeholder="企业规模" style="width: 100%;">
                      <el-option label="20人以下" value="20人以下" />
                      <el-option label="20-99人" value="20-99人" />
                      <el-option label="100-499人" value="100-499人" />
                      <el-option label="500-999人" value="500-999人" />
                      <el-option label="1000-4999人" value="1000-4999人" />
                      <el-option label="5000人以上" value="5000人以上" />
                    </el-select>
                  </el-form-item>
                </div>
                <el-form-item prop="nature">
                  <el-select v-model="form.nature" placeholder="企业性质" style="width: 100%;">
                    <el-option label="民营企业" value="民营企业" />
                    <el-option label="国有企业" value="国有企业" />
                    <el-option label="外资企业" value="外资企业" />
                    <el-option label="合资企业" value="合资企业" />
                    <el-option label="上市公司" value="上市公司" />
                    <el-option label="事业单位" value="事业单位" />
                    <el-option label="其他" value="其他" />
                  </el-select>
                </el-form-item>
              </div>

              <!-- 账号设置 -->
              <div class="form-section">
                <div class="section-title">账号设置</div>
                <el-form-item prop="username">
                  <el-input v-model="form.username" placeholder="登录账号" prefix-icon="User" />
                </el-form-item>
                <div class="form-row">
                  <el-form-item prop="password">
                    <el-input v-model="form.password" type="password" placeholder="设置密码" prefix-icon="Lock" show-password />
                  </el-form-item>
                  <el-form-item prop="confirmPassword">
                    <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password />
                  </el-form-item>
                </div>
              </div>

              <el-form-item>
                <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister">提交入驻申请</el-button>
              </el-form-item>
            </el-form>
          </div>

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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi, adminApi } from '@/api'
import { DataLine, Connection, School, PieChart, OfficeBuilding, UserFilled, Phone, Postcard, Message, LocationInformation, Location, HomeFilled, User, Lock } from '@element-plus/icons-vue'
import lxLogo from '@/assets/login/lx-logo.png'
import heroImage from '@/assets/login/hero.png'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const deptList = ref([])

onMounted(async () => {
  try {
    const res = await adminApi.dept.list()
    deptList.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.error('加载学院列表失败', e)
  }
})

const form = reactive({
  companyName: '',
  deptId: null,
  unifiedCreditCode: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  province: '',
  city: '',
  address: '',
  industry: '',
  scale: '',
  nature: '',
  username: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules = {
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择入驻学院', trigger: 'change' }],
  contactPerson: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入所在省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入所在城市', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  industry: [{ required: true, message: '请选择所属行业', trigger: 'change' }],
  scale: [{ required: true, message: '请选择企业规模', trigger: 'change' }],
  nature: [{ required: true, message: '请选择企业性质', trigger: 'change' }],
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 4, message: '账号长度至少4位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.companyRegister(form)
    ElMessage.success('入驻申请已提交，请等待管理员审核')
    router.push('/login')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
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
  min-height: 260px;
}

.hero-glow {
  position: absolute;
  width: 500px;
  height: 320px;
  background: radial-gradient(ellipse, rgba(47, 107, 255, 0.12) 0%, transparent 72%);
  border-radius: 50%;
  pointer-events: none;
}

.hero-image {
  width: 460px;
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
}

.form-panel {
  text-align: center;
  width: 100%;
  max-width: 460px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.form-title-wrap {
  margin-bottom: 20px;
  flex-shrink: 0;
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

/* 表单滚动区域 */
.form-scroll {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
  max-height: 520px;
}

.form-scroll::-webkit-scrollbar {
  width: 4px;
}

.form-scroll::-webkit-scrollbar-track {
  background: #f0f4ff;
  border-radius: 4px;
}

.form-scroll::-webkit-scrollbar-thumb {
  background: #c0d0ee;
  border-radius: 4px;
}

.form-scroll::-webkit-scrollbar-thumb:hover {
  background: #a0b8dc;
}

/* 表单 */
.auth-form {
  width: 100%;
  text-align: left;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.auth-form :deep(.el-input__wrapper) {
  padding: 3px 14px;
  border-radius: 10px;
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
  height: 40px;
  font-size: 13px;
  color: #2d4a72;
}

.auth-form :deep(.el-select .el-input__wrapper) {
  padding-right: 36px;
}

/* 表单分组 */
.form-section {
  margin-bottom: 16px;
  padding: 16px;
  background: rgba(245, 249, 255, 0.5);
  border: 1px solid rgba(218, 232, 252, 0.7);
  border-radius: 14px;
}

.section-title {
  font-size: 12px;
  font-weight: 700;
  color: #2d4a72;
  letter-spacing: 0.5px;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(218, 232, 252, 0.8);
  text-align: left;
}

/* 双列布局 */
.form-row {
  display: flex;
  gap: 10px;
}

.form-row > .el-form-item {
  flex: 1;
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
  margin-top: 8px;
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
  flex-shrink: 0;
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
    max-width: 600px;
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
    min-height: 180px;
  }

  .hero-image {
    width: 320px;
  }

  .auth-right {
    flex: none;
    padding: 36px 40px;
  }

  .form-scroll {
    max-height: none;
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
    min-height: 140px;
  }

  .hero-image {
    width: 260px;
  }

  .auth-right {
    padding: 28px 24px;
  }

  .form-section {
    padding: 12px;
  }
}
</style>
