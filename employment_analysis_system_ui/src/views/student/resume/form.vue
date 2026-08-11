<template>
  <div class="resume-form-container">
    <div class="page-header">
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>{{ isEdit ? '编辑简历' : '创建简历' }}</h2>
      <el-button type="primary" :loading="saving" @click="handleSave">
        <el-icon><Select /></el-icon> 保存
      </el-button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="resume-form">
      <el-card header="基本信息">
        <el-form-item label="简历名称" prop="resumeName">
          <el-input v-model="form.resumeName" placeholder="例如：我的简历、软件开发简历" maxlength="50" show-word-limit />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="期望职位" prop="expectedPosition">
              <el-input v-model="form.expectedPosition" placeholder="例如：Java开发工程师" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望城市" prop="expectedCity">
              <el-input v-model="form.expectedCity" placeholder="例如：北京、上海" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最低薪资(K)">
              <el-input-number v-model="form.expectedSalaryMin" :min="0" :max="999" placeholder="最低" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高薪资(K)">
              <el-input-number v-model="form.expectedSalaryMax" :min="0" :max="999" placeholder="最高" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="期望行业">
          <el-input v-model="form.expectedIndustry" placeholder="例如：互联网、金融" />
        </el-form-item>
      </el-card>

      <el-card header="个人简介" class="mt-16">
        <el-form-item label="个人简介" prop="personalSummary">
          <el-input v-model="form.personalSummary" type="textarea" :rows="3" placeholder="简明扼要地介绍自己" maxlength="500" show-word-limit />
        </el-form-item>
      </el-card>

      <el-card header="教育经历" class="mt-16">
        <el-form-item label="教育经历" prop="educationExperience">
          <el-input v-model="form.educationExperience" type="textarea" :rows="4" placeholder="请按以下格式填写：
学校名称 | 专业 | 学历 | 起止时间
例如：
XX大学 | 计算机科学与技术 | 本科 | 2020.09 - 2024.06" />
        </el-form-item>
      </el-card>

      <el-card header="项目经历" class="mt-16">
        <el-form-item label="项目经历">
          <el-input v-model="form.projectExperience" type="textarea" :rows="4" placeholder="请按以下格式填写：
项目名称
项目时间：
项目描述：
项目职责：
技术栈：" />
        </el-form-item>
      </el-card>

      <el-card header="工作/实习经历" class="mt-16">
        <el-form-item label="工作经历">
          <el-input v-model="form.workExperience" type="textarea" :rows="4" placeholder="请按以下格式填写：
公司名称 | 职位 | 起止时间
工作内容：" />
        </el-form-item>
      </el-card>

      <el-card header="技能与证书" class="mt-16">
        <el-form-item label="技能证书">
          <el-input v-model="form.skillCertificates" type="textarea" :rows="3" placeholder="请列出掌握的专业技能和获得的证书，如：
- 熟练掌握Java、Spring Boot
- 熟悉MySQL、Redis
- 获得CET-4证书" />
        </el-form-item>
      </el-card>

      <el-card header="荣誉奖项" class="mt-16">
        <el-form-item label="荣誉奖项">
          <el-input v-model="form.awardsHonors" type="textarea" :rows="3" placeholder="请列出获得的奖学金、竞赛奖项等荣誉" />
        </el-form-item>
      </el-card>

      <el-card header="自我评价" class="mt-16">
        <el-form-item label="自我评价">
          <el-input v-model="form.selfEvaluation" type="textarea" :rows="3" placeholder="简要描述自己的性格特点、优势等" />
        </el-form-item>
      </el-card>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentApi } from '@/api'
import { ArrowLeft, Select } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const resumeId = computed(() => route.params.id)
const isEdit = computed(() => !!resumeId.value)

const form = reactive({
  resumeName: '',
  personalSummary: '',
  educationExperience: '',
  projectExperience: '',
  workExperience: '',
  skillCertificates: '',
  awardsHonors: '',
  selfEvaluation: '',
  expectedSalaryMin: 0,
  expectedSalaryMax: 0,
  expectedCity: '',
  expectedPosition: '',
  expectedIndustry: ''
})

const rules = {
  resumeName: [
    { required: true, message: '请输入简历名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  expectedPosition: [
    { required: true, message: '请输入期望职位', trigger: 'blur' }
  ]
}

async function loadResume() {
  if (!isEdit.value) return
  try {
    const data = await studentApi.getResume(resumeId.value)
    Object.assign(form, data)
  } catch (e) {
    // error handled by interceptor
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await studentApi.updateResume(resumeId.value, form)
      ElMessage.success('更新成功')
    } else {
      await studentApi.createResume(form)
      ElMessage.success('创建成功')
    }
    router.push('/student/resume')
  } catch (e) {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadResume()
})
</script>

<style scoped>
.resume-form-container {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.resume-form {
  max-width: 900px;
}

.mt-16 {
  margin-top: 16px;
}
</style>
