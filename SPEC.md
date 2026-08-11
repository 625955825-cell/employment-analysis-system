# 学生数据生成模块约束文档（Java版）

> 本文档是 `StudentDataInitRunner` 数据生成模块的约束参照。
> 运行时遵循本文档规则，若需修改规则，先改本文档，再改代码。

---

## 一、数据规模

| 项目 | 数值 |
|------|------|
| 班级总数 | **800 个**（41专业 × 5届：2018/2019/2020/2021/2022） |
| 年级数 | 5 届 |
| 每班人数 | 随机 40-45 人（seed=42，保证可复现） |
| 每届约人数 | ~6,900 人 |
| **五届总人数** | **~33,600 人** |

**班级来源**：从数据库 `sys_class` 表实时读取（不硬编码），要求 800 条记录存在。
**学生状态**：所有届（2018-2022）均为 `graduated`。
**执行方式**：随 Spring Boot 项目启动自动运行一次，运行后创建标记文件 `student_init_done.lock`，再次启动检测到该文件则跳过。
**密码**：`BCryptPasswordEncoder` 编码 `123456`（rounds=10）。

---

## 二、初始化入口

### 2.1 入口类

```
src/main/java/com/employment/init/
├── StudentDataInitRunner.java    # @Component，随项目启动自动运行
├── DataConstants.java           # 所有专业/学院/民族/简历模板等常量
└── RandomDataUtil.java          # 随机数据工具类
```

### 2.2 幂等机制

启动时检查 `DATA_INIT_LOCK_FILE = "student_init_done.lock"`（项目根目录）是否存在：
- **存在** → 跳过初始化，正常启动
- **不存在** → 执行初始化，结束后创建文件

### 2.3 错误处理

- 初始化失败时删除已插入的脏数据（回滚）
- 在 `try-catch` 中执行，失败不阻塞项目启动，仅打印错误日志

---

## 三、就业率与就业类型

### 3.1 按届别区分（所有专业统一就业率）

| 年级 | 就业率 | 未就业率 |
|------|-------|---------|
| 2018级 | 94% | 6% |
| 2019级 | 94% | 6% |
| 2020级 | 94% | 6% |
| 2021级 | 94% | 6% |
| 2022级 | 83% | 17% |

> 大数据学院（网工/大数据/智科/网安）与其他专业就业率一致，不再单独区分。

### 3.2 就业类型分布（占已就业人数）

| 就业类型（数据库值） | 占比 | 三方协议 | 简历 |
|------|------|---------|------|
| 签订三方协议 | 52% | ✅ | **完整，专业匹配** |
| 继续深造（考研升学） | 12% | ❌ | 完整，专业匹配 |
| 签订劳动合同 | 20% | ❌ | 完整，专业匹配 |
| 自由职业（灵活就业） | 7% | ❌ | 完整 |
| 出国出境 | 2% | ❌ | 完整 |
| 应征入伍 | 1% | ❌ | 完整 |

> 数据库 `employment_record.employment_type` 实际值：考研升学→"继续深造"，灵活就业→"自由职业"。

### 3.3 未就业学生（占未就业人数）

| 简历类型 | 占比 | 特征描述 |
|---------|------|---------|
| 无简历 | 50% | 根本不创建简历记录 |
| 简陋简历（心不在焉型） | 50% | 创建简历但内容空洞 |

**简陋简历（心不在焉型）特征**：
- 内容少：仅有基本信息，无实质内容
- 很空：自我评价泛泛而谈，没有具体事例
- 不突出：技能描述千篇一律，与专业/职位无关
- 缺乏细节：没有具体的项目经历、技术栈、时间线
- 所有专业千篇一律：无法从简历看出学生是什么专业的

---

## 四、籍贯与民族

### 4.1 籍贯分布

| 籍贯 | 占比 |
|------|------|
| 贵州省 | 30% |
| 湖北省 | 22% |
| 湖南省 | 15% |
| 河南省 | 10% |
| 广东省 | 8% |
| 江西省 | 7% |
| 安徽省 | 5% |
| 四川省 | 2% |
| 其他 | 1% |

### 4.2 民族分布

**少数民族优先分配贵州省籍学生（符合贵州实际民族分布）。**

| 民族 | 占比 | 籍贯 |
|------|------|------|
| 汉族 | 65% | 按籍贯分布随机分配 |
| **苗族** | **8%** | **贵州省** |
| **土家族** | **6%** | **贵州省** |
| **布依族** | **5%** | **贵州省** |
| **侗族** | **4%** | **贵州省** |
| **彝族** | **3%** | **贵州省** |
| **仡佬族** | **2.5%** | **贵州省** |
| **水族** | **2%** | **贵州省** |
| **瑶族** | **1.5%** | **贵州省** |
| **壮族** | **1.5%** | **贵州省** |
| **毛南族** | **0.5%** | **贵州省** |
| 其他少数民族 | 1% | 其他省份 |

> 少数民族合计35%，其中99%分配贵州省籍（1%极少量散落在其他省份）。

### 4.3 政治面貌

| 政治面貌 | 占比 |
|---------|------|
| 共青团员 | 75% |
| 群众 | 15% |
| 中共党员/预备党员 | 10% |

---

## 五、简历内容设计

### 5.1 完整简历（已就业学生）

**原则：专业定制 + 内容多样。同专业学生之间内容不能一模一样，需有随机变化。**

#### personal_summary（专业定制，80-150字，随机选模板）

每个专业准备3-5个不同侧重点的模板，随机分配给学生（seed=42固定内部随机，保证可复现）。

#### education_experience（完整，随机变化）

```
基本格式：
  {入学年}-09 至 {毕业年}-07  |  {院系名称}  |  {专业名称}  |  {班级名}

随机变化项（每学生独立）：
  - GPA: 2.5-4.0 之间随机，保留2位小数
  - 排名: 专业前1%-30%（与GPA负相关）
  - 英语: 30%无证书 / 70%有CET-4 / 40%有CET-6（可叠加）
  - 主修课程: 从该专业课程列表中随机选8-10门，随机排列顺序
```

#### project_experience（2-3个，随机从项目池中抽取并变化）

- 项目时间：{起始年}-{起始年+1}年（随机偏移±1年）
- 项目名称：随机选一个
- 技术栈：从该专业技能池随机选2-3个
- 个人职责：3种职责模板随机选一个
- 项目描述：2-3种描述角度随机组合

#### skill_certificates（随机变化）

每专业准备4-6个证书池，随机选2个。

#### self_evaluation（3种语气风格随机）

```
积极型：本人性格开朗，学习能力强，具备良好的团队协作能力。
        在校期间认真学习专业知识，积极参与实践活动，综合素质较好。
        毕业后希望从事与本专业相关的工作。

沉稳型：本人踏实稳重，学习态度端正，专业基础扎实。
        在校期间积极参与课程实践和实习，具备一定的工程实践能力。
        期望毕业后从事与专业相关的工作，在岗位上持续成长。

进取型：本人思维活跃，动手能力强，善于将理论知识应用于实践。
        在校期间认真学习专业知识，积极参加学科竞赛和创新项目。
        期望在{专业相关}领域发展，为企业创造价值。
```

#### expected_salary_min/max：5000-12000元/月（随机）
#### expected_city/position/industry：与专业匹配（随机选）

### 5.2 简陋简历（心不在焉型，未就业50%）

```
personal_summary：
  本人性格开朗，乐观向上。（仅一句话，空洞无物，无法体现专业能力）

education_experience：
  2018-09 至 2022-07  |  XX学院  |  XX专业  |  XX181班
  （仅此一行，无GPA、无排名、无英语、无课程，敷衍了事）

project_experience：（空）
skill_certificates：（空）
self_evaluation：希望找到一份工作。（仅一句话，没有自我认知，没有具体方向）
expected_salary_min/max：（空）
```

### 5.3 student_info 各字段生成规则（所有学生）

| 字段 | 生成规则 |
|------|---------|
| `birth_date` | 根据身份证号推算，或随机 1998-2004 年范围内 |
| `nation` | 见第四章民族分布规则（少数民族99%贵州省籍） |
| `politics_status` | 共青团员75%、群众15%、中共党员/预备党员10% |
| `phone` | 随机生成11位手机号（1开头） |
| `email` | username@student.edu.cn 格式 |
| `address` | 格式："贵州省XX市XX区/县"（贵州省籍）；其他省籍填对应省份地址 |
| `study_type` | 全部填 "统招" |
| `dormitory` | 随机生成，如 "南苑1舍301" |
| `emergency_contact` | 随机亲属称谓 + 姓名，如 "父亲 张XX" |
| `emergency_phone` | 随机生成11位手机号 |
| `avatar` | 使用系统默认头像 URL（可空） |
| `status` | 全部为 "graduated" |

---

## 六、专业→企业匹配规则

### 6.1 专业群 → 行业 → 典型企业

根据学生专业，从 `company_info.industry` 匹配企业，再从 `job_position` 匹配对应职位。
**核心原则：简历内容必须与学生专业及签约岗位高度匹配。**

| 专业 | 匹配行业（company_info.industry like '%XX%'） |
|------|------------------------------------------|
| 资源勘查工程 | 地质勘查 |
| 环境工程 | 环保/工程，环保科技，节能环保 |
| 地质工程 | 地质勘查 |
| 机械设计制造及其自动化 | 工程机械，装备制造 |
| 机械电子工程 | 激光/装备，汽车制造，汽车研发 |
| 电气工程及其自动化 | 电力/能源，工业自动化 |
| 自动化 | 电力/能源，工业自动化 |
| 土木工程 | 建筑工程，建筑施工 |
| 水利水电工程 | 水利水电 |
| 道路桥梁与渡河工程 | 交通工程，工程设计 |
| 化学工程与工艺 | 化工/矿产，石油化工，化工/能源，化工工程，化工/石化 |
| 新能源科学与工程 | 光伏/新能源，电力/新能源 |
| 过程装备与控制工程 | 装备制造，化工工程 |
| 制药工程 | 医药制造，医药研发 |
| 生物制药 | 生物医药 |
| 酿酒工程 | 酒类制造，酒类酿造，酒类/保健 |
| 安全工程 | 安全咨询，能源/矿业，煤炭/能源，能源/装备 |
| 采矿工程 | 能源/矿业，煤炭/能源 |
| 智能采矿工程 | 煤炭/能源，能源/装备 |
| 工程管理 | 建筑工程，房地产开发 |
| 工程造价 | 建筑工程，建筑施工 |
| 投资学 | 金融/证券，金融/银行，互联网金融 |
| 网络工程 | IT/网络，网络安全 |
| 数据科学与大数据技术 | 互联网/大数据，互联网/数据，大数据服务 |
| 智能科学与技术 | AI/语音，互联网/AI |
| 网络空间安全 | 网络安全 |
| 飞行器制造工程 | 航空航天，航空航天/军工 |
| 飞行器动力工程 | 航空航天 |
| 人文地理与城乡规划 | 规划设计 |
| 环境设计 | 建筑装饰，室内/环境设计 |
| 建筑学 | 建筑设计 |
| 材料科学与工程 | 钢铁冶金，有色金属，科研/材料 |
| 新能源材料与器件 | 新材料，新能源/材料 |
| 焊接技术与工程 | 焊接技术，装备制造 |
| 交通工程 | 交通工程，交通规划 |
| 交通运输 | 城市轨道交通 |
| 应用统计学 | 政府/统计，大数据服务 |
| 应用物理学 | 科研/材料 |
| 休闲体育 | 体育/旅游，体育产业，体育培训，体育科技 |
| 英语 | 教育培训，翻译/语言服务 |

---

## 七、谈心谈话记录（conversation_record）

**触发条件**：所有未就业学生必须有谈心谈话记录。

**字段值**：
- `conversation_type`：`就业指导`（40%）、`心理疏导`（25%）、`学业辅导`（15%）、`生活关怀`（15%）、`其他`（5%）
- `conversation_place`：随机选，如"辅导员办公室"、"线上"、"学生宿舍"
- `topic`：根据类型生成
- `content`：`RandomDataUtil.generateConversationContent(type)` 返回辅导员谈话内容（50-200字）
- `result`：谈话结果
- `next_plan`：下一步计划

**teacher_id 来源**：`sys_class.advisor_id` 关联的 `sys_user.id`（class_teacher 账号）。

**conversation_time**：毕业前一年内随机分布，间隔合理。

**谈话次数**：
- 未就业学生：2-4次（随机）
- 已就业学生：0-1次（5%概率有一次确认就业）

---

## 八、job_application 投递链路

### 核心规则
- **每个学生独立决策**：是否投递、投递几家、哪家给offer，完全独立
- **面试 ≠ 被录取**：面试后可能无offer（rejected）、有offer但学生选择其他（declined）
- **多投递多机会**：一个学生可以有多条 application 记录
- **签约 = 最终接受了某家offer**

### 已就业学生投递链路

```
投递A（最终签约）:
  application: read=1, interview_status=passed, offer_status=offered, status=accepted
    → interview_invitation: status=completed
    → offer_letter: status=accepted
    → employment_record + tripartite_agreement（若签三方）

投递B（面试后被拒）:
  application: read=1, interview_status=passed, offer_status=declined, status=rejected
    → interview_invitation: status=completed

投递C（无回音）:
  application: read=1, interview_status=no_response, status=rejected
```

### interview_invitation / offer_letter / interview_record
- **interview_invitation**：仅对面试过的 application 生成
- **offer_letter**：仅对最终签约的 application 生成
- **interview_record**：仅对有完整面试流程的 invitation 生成
- **tripartite_agreement**：仅对 employment_type='签订三方协议' 且有 offer_letter 的学生生成

---

## 九、数据表闭环关系

```
sys_user.id (= student_id)
    ↓
sys_user_role (user_id → sys_user.id)
student_info (user_id → sys_user.id, status = 'graduated')
student_resume (student_id → sys_user.id)
    ↓
employment_record (student_id → sys_user.id, 仅已就业,
    employment_type = '签订三方协议'|'继续深造'|'签订劳动合同'|
                     '自由职业'|'出国出境'|'应征入伍',
    audit_status = 'approved')
    ↓
tripartite_agreement (student_id → sys_user.id, 仅 employment_type='签订三方协议',
    school_sign_time = 毕业年6月)
    ↓
job_application (student_id → sys_user.id, job_id, company_id)
    ↓
interview_invitation (application_id → job_application.id, 仅面试学生)
offer_letter (application_id → job_application.id, 仅最终签约学生)
interview_record (invitation_id → interview_invitation.id)
    ↓
conversation_record (student_id → sys_user.id,
    teacher_id → sys_user.id 来自 sys_class.advisor_id)
    ↓
sys_class (UPDATE student_count)
```

---

## 十、Java 代码模块约束

### 10.1 类文件结构

```
src/main/java/com/employment/init/
├── StudentDataInitRunner.java    # @Component，启动时运行
├── DataConstants.java           # 常量（专业列表、学院列表、民族池、简历模板等）
└── RandomDataUtil.java          # 随机工具类 + StudentProfile 一次性生成学生画像
```

### 10.2 字段类型对应（Java → MySQL）

### 10.3 数据一致性保证

**`StudentProfile` 机制**：`RandomDataUtil.StudentProfile` 在每个学生循环中只生成一次，包含该学生的姓名、GPA、排名、英语水平、精选项课、精选项目、精选技术栈、精选证书、自我评价、期望薪资。所有简历字段直接复用该 Profile 中的数据，**保证学生个人信息与简历内容完全一致**。

生成顺序（关键）：
1. 判定 `isEmployed` 和 `employmentType`
2. 确定 `matchedCompany` + `matchedJob`（就业去向）
3. 生成 `StudentProfile`（姓名、课程、项目、证书等，一次性）
4. 调用 `generateFullResume(profile, ...)` 填入简历（直接使用 Profile 数据）
5. 生成就业链路 / 谈话记录

**简历丰富度（就业学生）**：`generateFullResume` 新增以下真实信息：
- **实习经历**（`workExperience`）：引用真实签约公司的名称、行业、规模、工作城市，生成"大四下学期在某公司实习"的标准格式经历。
- **学科竞赛与获奖**（`awardsHonors`）：从 `DataConstants.MAJOR_AWARDS` 中按专业匹配（覆盖约 20 个主流专业），生成国家级 / 省级 / 校级竞赛获奖信息；未覆盖专业使用校级奖学金兜底。
- **自我介绍**（`personalSummary`）：从 `DataConstants.PERSONAL_SUMMARIES` 中按专业匹配（约 40 个专业），生成定制化自我介绍。
- 未就业学生简历使用 "XX" 占位符，不泄露个人信息。

### 10.5 Entity 字段映射

| MySQL 类型 | Java 类型 |
|-----------|---------|
| VARCHAR | String |
| INT | Integer |
| BIGINT | Long |
| DATETIME | LocalDateTime |
| TEXT | String |
| DOUBLE | Double |

### 10.3 Entity 字段映射

| Entity | 关键字段 |
|--------|---------|
| SysUser | username, password, realName, gender, phone, email, idCard, deptId, majorId, classId, studentNo, graduationYear, remark='批量导入学生' |
| StudentInfo | userId, studentNo, realName, nation, politicsStatus, deptId, majorId, classId, graduationYear, status='graduated' |
| StudentResume | studentId, resumeName, isDefault='1', personalSummary, educationExperience, projectExperience, **workExperience**, skillCertificates, **awardsHonors**, selfEvaluation, expectedSalaryMin/Max, expectedCity/Position/Industry |
| EmploymentRecord | studentId, employmentType, companyName, companyScale, positionName, workCity/Province, salary, isThreePartySigned, threePartyNo, auditStatus='approved' |
| ConversationRecord | teacherId, studentId, conversationTime, conversationType, topic, content, result, nextPlan |
| JobApplication | studentId, jobId, companyId, resumeId, status, interviewStatus, offerStatus |
| InterviewInvitation | applicationId, interviewTime, interviewType, status |
| OfferLetter | applicationId, positionName, salary, startDate, status |
| TripartiteAgreement | studentId, companyId, agreementNo, studentSignTime, companySignTime, schoolSignTime, status |
| SysUserRole | userId, roleId |

### 10.6 密码生成

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String encodedPassword = encoder.encode("123456");
```

### 10.5 学号格式

`{毕业年份(4位)}{班级数据库ID(4位)}{学生序号(3位)}`，如 `202249020333=2601/第1个学生）。

学号 = username（两字段值相同），全局唯一，无碰撞。
- 使用班级在数据库中的 `id` 主键（非班级序号）保证全校 800 个班级的学生学号无重叠。
- 每班 40~45 人，学生序号 001~045。
- 示例：`202249020333` → 毕业年份2022 / 班级ID=4902 / 第033个学生

### 10.8 三方协议编号格式

`TP{毕业年份(4位)}{全局序号(6位)}`，如 `TP2022000001`。

全局序号在 `StudentDataService` 中全局递增（`totalAgreementsSeq`），保证全校不重复。

### 10.9 随机种子

全局 `Random random = new Random(42);`，所有随机调用通过此实例。
`random.nextInt()`, `random.nextDouble()`, `random.nextBoolean()` 等。

### 10.10 批量提交策略

每处理完一个班级（40-45人）立即 `entityManager.flush()` + `entityManager.clear()`，避免内存溢出。

### 10.11 事务管理

整个初始化在一个 `@Transactional` 方法中执行，失败自动回滚。

---

## 十一、执行前提

1. `sys_dept` 表有数据（16个学院）
2. `sys_major` 表有数据（41个专业）
3. `sys_class` 表有数据（800个班级，已有关联 advisor_id）
4. `company_info` 表有数据（200家企业）
5. `job_position` 表有数据（585个职位）
6. `sys_role` 表有 `student` 角色（role_key='student'）

以上数据由 `import_companies.py` 和 `import_class_teachers.py` 先生成。

---

## 十二、生成后处理

1. 执行 `UPDATE sys_class c SET student_count = (SELECT COUNT(*) FROM sys_user WHERE class_id = c.id)` 更新班级学生数
2. 创建标记文件 `student_init_done.lock`
3. 输出统计信息：总学生数、总简历数、总就业记录数、总投递数等
