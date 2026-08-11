package com.employment.init;

import lombok.Data;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 随机数据工具类
 * 所有随机数据生成逻辑集中在此类，调用方无需关心随机细节
 */
public class RandomDataUtil {

    private final Random random;

    public RandomDataUtil(long seed) {
        this.random = new Random(seed);
    }

    // ==================== 基础随机 ====================

    /** 返回 [0, bound) 的整数 */
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    /** 返回 [0.0, 1.0) 的 double */
    public double nextDouble() {
        return random.nextDouble();
    }

    /** 返回 true/false */
    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    /** 打乱列表顺序（in-place） */
    public <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    // ==================== 姓名生成 ====================

    public String generateName(String gender) {
        String surname = DataConstants.XING.get(random.nextInt(DataConstants.XING.size()));
        List<String> mingPool = "男".equals(gender)
                ? DataConstants.MING_MALE
                : DataConstants.MING_FEMALE;
        String givenName = mingPool.get(random.nextInt(mingPool.size()));
        return surname + givenName;
    }

    // ==================== 手机号生成 ====================

    private static final String[] PHONE_PREFIXES = {
            "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
            "150", "151", "152", "153", "155", "156", "157", "158", "159",
            "170", "171", "172", "173", "175", "176", "177", "178",
            "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
            "198", "199"
    };

    public String generatePhone() {
        String prefix = PHONE_PREFIXES[random.nextInt(PHONE_PREFIXES.length)];
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    // ==================== 身份证号生成 ====================

    private static final DateTimeFormatter ID_BIRTHDAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generateIdCard(int gradYear) {
        // 随机选择省份
        String province =
                randomChoice(DataConstants.PROVINCES);
        List<String> codes = DataConstants.IDCARD_CODES.get(province);
        if (codes == null || codes.isEmpty()) {
            codes = DataConstants.IDCARD_CODES.get("其他");
        }
        String code = codes.get(random.nextInt(codes.size()));

        // 出生年: gradYear-24 ~ gradYear-20
        int birthYear = gradYear - random.nextInt(5) - 20;
        int birthMonth = random.nextInt(12) + 1;
        int birthDay = random.nextInt(28) + 1;
        String birthday = String.format("%04d%02d%02d", birthYear, birthMonth, birthDay);

        // 顺序码
        int seq = random.nextInt(999) + 1;
        // 校验码
        int genderCode = seq * 2 + (random.nextBoolean() ? 1 : 0);

        return code + birthday + String.format("%03d", seq) + (genderCode % 10);
    }

    // ==================== 籍贯生成 ====================

    public String generateProvince() {
        return randomChoice(DataConstants.PROVINCES);
    }

    public String generateCity(String province) {
        List<String> cities = DataConstants.PROVINCE_CITIES.get(province);
        if (cities == null || cities.isEmpty()) {
            cities = DataConstants.PROVINCE_CITIES.get("其他");
        }
        return cities.get(random.nextInt(cities.size()));
    }

    public Pair<String, String> generateProvinceCity() {
        String province = generateProvince();
        String city = generateCity(province);
        return new Pair<>(province, city);
    }

    // ==================== 民族生成 ====================

    public String generateNation(String province) {
        String nation = weightedChoiceByArray(DataConstants.NATIONS);
        if ("汉族".equals(nation)) {
            return nation;
        }
        // 少数民族: 99%贵州省
        if (random.nextDouble() < 0.99) {
            return nation;
        }
        // 1%散落到其他省份
        return nation;
    }

    // ==================== 政治面貌生成 ====================

    public String generatePolitics() {
        return weightedChoiceByArray(DataConstants.POLITICS);
    }

    // ==================== 宿舍/地址生成 ====================

    public String generateDormitory() {
        int building = random.nextInt(20) + 1;
        int room = random.nextInt(400) + 1;
        String pattern = DataConstants.DORMITORY_PATTERNS.get(random.nextInt(DataConstants.DORMITORY_PATTERNS.size()));
        if (pattern.contains("{2}")) {
            int floor = (room - 1) / 100 + 1;
            return String.format(pattern, building, floor, room);
        }
        return String.format(pattern, building, room);
    }

    public String generateAddress(String province, String city) {
        String suffix = DataConstants.STREET_SUFFIXES.get(random.nextInt(DataConstants.STREET_SUFFIXES.size()));
        return province + city + suffix;
    }

    public String generateEmergencyContact() {
        String relation = DataConstants.EMERGENCY_RELATIONS.get(random.nextInt(DataConstants.EMERGENCY_RELATIONS.size()));
        String gender = DataConstants.MALE_RELATIONS.contains(relation) ? "男" : "女";
        String name = generateName(gender);
        return relation + " " + name;
    }

    // ==================== 学号生成 ====================

    /**
     * 生成唯一学号：{毕业年份}{班级ID(4位)}{学生序号(3位)}
     * 使用班级数据库主键ID保证全校唯一，无碰撞
     * 例如：20182601001（2018届/班级ID=2601/第1个学生）
     */
    public String generateStudentNo(int gradYear, long classId, int stuSeq) {
        return String.format("%d%04d%03d", gradYear, classId, stuSeq);
    }

    // ==================== 权重选择 ====================

    public <T> T weightedChoice(List<Map.Entry<T, Double>> weightedList) {
        double total = 0;
        for (Map.Entry<T, Double> e : weightedList) {
            total += e.getValue();
        }
        double r = random.nextDouble() * total;
        double cumulative = 0;
        for (Map.Entry<T, Double> e : weightedList) {
            cumulative += e.getValue();
            if (r < cumulative) {
                return e.getKey();
            }
        }
        return weightedList.get(weightedList.size() - 1).getKey();
    }

    public <T> T weightedChoiceInt(List<Map.Entry<T, Integer>> weightedList) {
        int total = 0;
        for (Map.Entry<T, Integer> e : weightedList) {
            total += e.getValue();
        }
        int r = random.nextInt(total);
        int cumulative = 0;
        for (Map.Entry<T, Integer> e : weightedList) {
            cumulative += e.getValue();
            if (r < cumulative) {
                return e.getKey();
            }
        }
        return weightedList.get(weightedList.size() - 1).getKey();
    }

    /**
     * 基于字符串数组的加权选择，数组格式：[选项, 权重]
     * 例如: new String[]{"共青团员", "75"}
     */
    public String weightedChoiceByArray(List<String[]> weightedList) {
        double total = 0;
        for (String[] entry : weightedList) {
            total += Double.parseDouble(entry[1]);
        }
        double r = random.nextDouble() * total;
        double cumulative = 0;
        for (String[] entry : weightedList) {
            cumulative += Double.parseDouble(entry[1]);
            if (r < cumulative) {
                return entry[0];
            }
        }
        return weightedList.get(weightedList.size() - 1)[0];
    }

    // ==================== 从列表随机抽取 ====================

    public <T> List<T> pickNRandom(List<T> population, int n) {
        int size = Math.min(n, population.size());
        List<T> copy = new ArrayList<>(population);
        Collections.shuffle(copy, random);
        return copy.subList(0, size);
    }

    public <T> T randomChoice(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    // ==================== GPA & 排名 ====================

    public double generateGpa() {
        return Math.round((2.5 + random.nextDouble() * 1.5) * 100.0) / 100.0;
    }

    public int calculateRank(double gpa) {
        // gpa 4.0 → rank 1%; gpa 2.5 → rank 30%
        double factor = (4.0 - gpa) / 1.5; // 0.0 ~ 1.0
        int rank = (int) Math.round(factor * 29) + 1;
        rank += random.nextInt(10) - 5; // ±5% 随机波动
        return Math.max(1, Math.min(30, rank));
    }

    // ==================== 英语水平 ====================

    public String generateEnglishLevel() {
        double r = random.nextDouble();
        if (r < 0.3) {
            return ""; // 无英语
        } else if (r < 0.8) {
            int score = random.nextInt(101) + 400; // CET-4
            return String.format("英语：CET-4 %d分", score);
        } else {
            int score4 = random.nextInt(101) + 400;
            int score6 = random.nextInt(101) + 400;
            return String.format("英语：CET-4 %d分 / CET-6 %d分", score4, score6);
        }
    }

    // ==================== 学生完整画像（一次性生成全部个人数据） ====================

    @Data
    public static class StudentProfile {
        public String name;
        public String gender;
        public double gpa;
        public int rank;
        public String englishLevel;       // "" 或 "CET-4 xxx分" 或 "CET-4 xxx分 / CET-6 xxx分"
        public List<String> selectedCourses;  // 精选的8-10门课程
        public List<String> selectedProjects; // 精选的1-2个项目名
        public List<String> selectedSkills;   // 精选的2-3个技术栈
        public List<String> selectedCerts;    // 精选的2个证书
        public String selfEval;               // 自我评价（3条）
        public int expectedSalaryMin;
        public int expectedSalaryMax;
    }

    /**
     * 一次性生成学生完整画像，保证个人信息和简历内容完全一致
     * 所有随机数只调用一次，结果复用
     */
    public StudentProfile generateStudentProfile(String name, String gender, String majorName) {
        StudentProfile p = new StudentProfile();
        p.name = name;
        p.gender = gender;

        // GPA & 排名
        p.gpa = generateGpa();
        p.rank = calculateRank(p.gpa);

        // 英语水平
        p.englishLevel = generateEnglishLevel();

        // 精选课程（8-10门，随机排列）
        List<String> coursePool = DataConstants.MAJOR_COURSES.get(majorName);
        if (coursePool == null || coursePool.isEmpty()) {
            coursePool = Arrays.asList("专业核心课程A", "专业核心课程B", "专业基础课程", "专业实践课程");
        }
        int numCourses = random.nextInt(3) + 8; // 8-10门
        p.selectedCourses = pickNRandom(coursePool, numCourses);

        // 精选项目（2-3个，SPEC要求）
        List<String> projPool = DataConstants.PROJECTS.get(majorName);
        if (projPool == null || projPool.isEmpty()) {
            p.selectedProjects = Collections.emptyList();
        } else {
            p.selectedProjects = pickNRandom(projPool, Math.min(3, projPool.size()));
        }

        // 精选技术栈（2-3个）
        List<String> skillPool = DataConstants.SKILL_POOLS.get(majorName);
        if (skillPool == null || skillPool.isEmpty()) {
            p.selectedSkills = Arrays.asList("CAD", "Office");
        } else {
            p.selectedSkills = pickNRandom(skillPool, random.nextInt(2) + 2);
        }

        // 精选证书（2个）
        List<String> certPool = DataConstants.CERT_POOLS.get(majorName);
        if (certPool == null || certPool.isEmpty()) {
            certPool = DataConstants.CERT_POOLS.get("default");
        }
        p.selectedCerts = pickNRandom(certPool, Math.min(2, certPool.size()));

        // 自我评价
        p.selfEval = generateSelfEval();

        // 期望薪资
        int[] sal = generateExpectedSalary();
        p.expectedSalaryMin = sal[0];
        p.expectedSalaryMax = sal[1];

        return p;
    }

    // ==================== 自我评价 ====================

    public String generateSelfEval() {
        Object[] templateEntry = DataConstants.SELF_EVAL_TEMPLATES.get(
                random.nextInt(DataConstants.SELF_EVAL_TEMPLATES.size())
        );
        @SuppressWarnings("unchecked")
        List<String> template = (List<String>) templateEntry[1];
        return String.join("\n", template);
    }

    // ==================== 期望薪资 ====================

    public int[] generateExpectedSalary() {
        int salMin = (random.nextInt(15) + 10) * 500; // 5000-12000
        int salMax = salMin + (random.nextInt(10) + 4) * 500;
        return new int[]{salMin, Math.min(salMax, 12000)};
    }

    // ==================== 教育经历 ====================

    /**
     * 基于预生成的 StudentProfile 生成教育经历，保证 GPA/排名/英语/课程 与简历其他部分一致
     */
    public String buildEducationExperience(StudentProfile profile, int enrollYear, int gradYear,
                                          String deptName, String majorName, String className) {
        StringBuilder sb = new StringBuilder();
        sb.append(enrollYear).append("-09 至 ").append(gradYear).append("-07  |  ")
                .append(deptName).append("  |  ")
                .append(majorName).append("  |  ")
                .append(className).append("\n");

        sb.append("GPA: ").append(profile.gpa).append("/4.0（专业前").append(profile.rank).append("%）\n");

        if (!profile.englishLevel.isEmpty()) {
            sb.append(profile.englishLevel).append("\n");
        }

        sb.append("主修课程：").append(String.join("、", profile.selectedCourses));
        return sb.toString();
    }

    // ==================== 项目经验 ====================

    /**
     * 基于预生成的 StudentProfile 生成项目经验，保证项目名与技术栈完全对应
     */
    public String buildProjectExperience(StudentProfile profile, String majorName, int baseYear) {
        if (profile.selectedProjects == null || profile.selectedProjects.isEmpty()) {
            return "";
        }

        String[] projDescs = {
                "进行了系统需求分析与方案设计，完成了主要功能模块的开发与测试。",
                "完成了数据分析与建模工作，建立了预测模型并进行了验证。",
                "进行了工艺设计与参数优化，解决了关键技术问题，达到了预期目标。",
                "完成了整体方案设计与实现，进行了性能测试与评估。",
                "进行了理论研究与实验验证，积累了实践经验。"
        };
        String[] projResps = {
                "负责项目整体规划与实施，完成了主要模块的设计与实现。",
                "担任项目技术骨干，主要负责技术方案设计与核心代码编写。",
                "参与项目全过程，负责实验数据采集与分析整理工作。"
        };

        StringBuilder sb = new StringBuilder();
        int projYearBase = baseYear + random.nextInt(2);

        for (int i = 0; i < profile.selectedProjects.size(); i++) {
            int pyear = projYearBase + i;
            String projName = profile.selectedProjects.get(i);
            // 如果选课列表足够，用它作为本项目的技术栈
            List<String> techPool = profile.selectedCourses.size() >= 3
                    ? profile.selectedCourses
                    : profile.selectedSkills;
            String projTech = String.join("、", pickNRandom(techPool, Math.min(3, techPool.size())));
            String projDesc = projDescs[random.nextInt(projDescs.length)];
            String projResp = projResps[random.nextInt(projResps.length)];

            sb.append(pyear).append("-03 至 ").append(pyear + 1)
                    .append("-06  |  ").append(projName).append("\n")
                    .append("  项目描述：").append(projDesc).append("\n")
                    .append("  技术栈：").append(projTech).append("\n")
                    .append("  ").append(projResp);
            if (i < profile.selectedProjects.size() - 1) {
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }

    // ==================== 证书 ====================

    /**
     * 基于预生成的 StudentProfile 返回证书字符串
     */
    public String buildCerts(StudentProfile profile) {
        return String.join("\n", profile.selectedCerts);
    }

    // ==================== 期望城市（全局随机，无状态） ====================

    public String generateExpectedCity() {
        List<String> allCities = new ArrayList<>();
        for (List<String> cities : DataConstants.PROVINCE_CITIES.values()) {
            allCities.addAll(cities);
        }
        return randomChoice(allCities);
    }

    // ==================== 期望岗位/行业 ====================

    public String[] generateExpectedPositionAndIndustry(String majorName) {
        List<String> positions = DataConstants.EXPECTED_POSITIONS.get(majorName);
        String position = (positions == null || positions.isEmpty())
                ? "技术工程师" : randomChoice(positions);

        List<String> keywords = DataConstants.MAJOR_INDUSTRY_KEYWORDS.get(majorName);
        String industry = (keywords == null || keywords.isEmpty())
                ? "其他" : keywords.get(0);

        return new String[]{position, industry};
    }

    // ==================== 简历生成 ====================

    @Data
    public static class ResumeData {
        public String resumeName;
        public String isDefault;
        public String personalSummary;
        public String educationExperience;
        public String projectExperience;
        public String workExperience;
        public String skillCertificates;
        public String awardsHonors;
        public String selfEvaluation;
        public Integer expectedSalaryMin;
        public Integer expectedSalaryMax;
        public String expectedCity;
        public String expectedPosition;
        public String expectedIndustry;
        public String filePath;
    }

    /**
     * 生成完整简历（就业学生），基于预生成的 StudentProfile
     * 保证姓名、GPA、项目、课程、证书、期望薪资完全一致
     */
    public ResumeData generateFullResume(StudentProfile profile, int enrollYear, int gradYear,
                                       String deptName, String majorName, String className,
                                       String companyCity,
                                       String matchedCompanyName, String matchedJobName,
                                       String matchedCompanyIndustry, String matchedCompanyScale,
                                       String matchedWorkCity) {
        ResumeData r = new ResumeData();
        r.resumeName = profile.name + "的简历";
        r.isDefault = "1";

        // 按专业定制的自我介绍
        List<String> summaries = DataConstants.PERSONAL_SUMMARIES.get(majorName);
        if (summaries == null || summaries.isEmpty()) {
            summaries = Arrays.asList(
                    "本人系统学习了本专业核心课程，具备扎实的理论基础和实践能力。",
                    "在校期间认真学习专业知识，积极参与专业实践，具备独立解决问题的能力。"
            );
        }
        r.personalSummary = randomChoice(summaries);

        // 教育经历（使用预生成的 GPA/排名/英语/课程）
        r.educationExperience = buildEducationExperience(profile, enrollYear, gradYear, deptName, majorName, className);

        // 项目经验（使用预生成的项目名和技术栈）
        r.projectExperience = buildProjectExperience(profile, majorName, enrollYear + 2);

        // 实习经历（引用真实签约公司）
        r.workExperience = buildWorkExperience(profile, gradYear,
                matchedCompanyName, matchedJobName,
                matchedCompanyIndustry, matchedCompanyScale, matchedWorkCity);

        // 学科竞赛与获奖
        r.awardsHonors = buildAwardsHonors(majorName);

        // 证书（使用预生成的证书列表）
        r.skillCertificates = buildCerts(profile);

        // 自我评价（已在 profile 中生成）
        r.selfEvaluation = profile.selfEval;

        r.expectedSalaryMin = profile.expectedSalaryMin;
        r.expectedSalaryMax = profile.expectedSalaryMax;

        // 期望城市优先使用签约公司所在城市
        if (companyCity != null && !companyCity.isEmpty()) {
            r.expectedCity = companyCity;
        } else {
            r.expectedCity = generateExpectedCity();
        }

        // 期望岗位/行业：已就业学生使用真实签约公司和职位
        if (matchedJobName != null && !matchedJobName.isEmpty()) {
            r.expectedPosition = matchedJobName;
        } else {
            String[] pi = generateExpectedPositionAndIndustry(majorName);
            r.expectedPosition = pi[0];
        }

        if (matchedCompanyIndustry != null && !matchedCompanyIndustry.isEmpty()) {
            r.expectedIndustry = matchedCompanyIndustry;
        } else {
            List<String> keywords = DataConstants.MAJOR_INDUSTRY_KEYWORDS.get(majorName);
            r.expectedIndustry = (keywords == null || keywords.isEmpty()) ? "其他" : keywords.get(0);
        }
        r.filePath = "";
        return r;
    }

    /**
     * 生成实习经历：引用真实签约公司的相关信息
     */
    private String buildWorkExperience(StudentProfile profile, int gradYear,
                                      String companyName, String jobName,
                                      String companyIndustry, String companyScale,
                                      String workCity) {
        if (companyName == null || companyName.isEmpty()) {
            return "";
        }
        // 取公司行业关键词（去掉 / 后缀）
        String industry = companyIndustry != null ? companyIndustry.replace("/", "") : "";
        String scale = companyScale != null ? companyScale : "";
        String city = workCity != null ? workCity : "";
        return String.format(
                "%d-03 至 %d-06  |  %s（%s）  |  %s\n" +
                "  岗位：%s\n" +
                "  工作内容：协助完成公司%s相关业务，参与%s项目的技术实施与数据整理工作，\n" +
                "           运用所学专业知识解决实际工作中的问题，积累了宝贵的实践经验。",
                gradYear - 1, gradYear,
                companyName, industry, scale,
                jobName != null && !jobName.isEmpty() ? jobName : "实习生",
                industry, city
        );
    }

    /**
     * 生成学科竞赛与获奖信息（按专业定制）
     */
    private String buildAwardsHonors(String majorName) {
        Map<String, List<String[]>> awards = DataConstants.MAJOR_AWARDS.get(majorName);
        if (awards == null || awards.isEmpty()) {
            List<String> defaultAwards = Arrays.asList(
                    "校级学业奖学金（三等奖）",
                    "校级优秀学生干部"
            );
            return String.join("\n", defaultAwards);
        }
        List<String[]> all = new ArrayList<>();
        for (List<String[]> list : awards.values()) all.addAll(list);
        List<String[]> picked = pickNRandom(all, Math.min(3, all.size()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < picked.size(); i++) {
            String[] a = picked.get(i);
            sb.append(a[0]).append(" | ").append(a[1]);
            if (i < picked.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /** 生成简陋简历（未就业学生，使用XX占位符，不泄露个人信息） */
    public ResumeData generateSimpleResume(String name, int enrollYear, int gradYear,
                                        String deptName, String majorName, String className) {
        ResumeData r = new ResumeData();
        r.resumeName = "XX的简历";
        r.isDefault = "1";
        r.personalSummary = "本人性格开朗，乐观向上。";
        r.educationExperience = String.format(
                "%d-09 至 %d-07  |  %s  |  %s  |  %s\n  （GPA/排名/英语成绩略）",
                enrollYear, gradYear,
                deptName.isEmpty() ? "XX学院" : deptName,
                majorName.isEmpty() ? "XX专业" : majorName,
                className.isEmpty() ? "XX班级" : className
        );
        r.projectExperience = "";
        r.workExperience = "";
        r.skillCertificates = "";
        r.awardsHonors = "";
        r.selfEvaluation = "希望找到一份工作。";
        r.expectedSalaryMin = null;
        r.expectedSalaryMax = null;
        r.expectedCity = "";
        r.expectedPosition = "";
        r.expectedIndustry = "";
        r.filePath = "";
        return r;
    }

    // ==================== 谈心谈话 ====================

    @Data
    public static class ConversationData {
        public String conversationType;
        public String conversationTime;
        public String conversationPlace;
        public String topic;
        public String content;
        public String result;
        public String nextPlan;
    }

    public ConversationData generateConversation(String type, int gradYear, int index, int total) {
        ConversationData c = new ConversationData();
        c.conversationType = type;

        DataConstants.ConversationTemplate tmpl = DataConstants.CONVERSATION_TEMPLATES.get(type);
        c.content = tmpl.getContent();
        c.result = tmpl.getResult();
        c.nextPlan = tmpl.getNextPlan();

        c.conversationTime = generateConversationTime(gradYear, index, total);

        c.conversationPlace = randomChoice(Arrays.asList(
                "辅导员办公室", "线上沟通", "学生宿舍", "学院会议室", "电话访谈"
        ));

        c.topic = switch (type) {
            case "就业指导" -> "就业意向沟通与指导";
            case "心理疏导" -> "求职心态调整";
            case "学业辅导" -> "学业困难帮扶";
            case "生活关怀" -> "生活困难关怀";
            default -> "日常谈心交流";
        };

        return c;
    }

    /** 生成毕业前一年内均匀分布的谈话时间 */
    public String generateConversationTime(int gradYear, int index, int total) {
        int startYear = gradYear - 1;
        int monthsBeforeGrad = 12;
        double monthStep = (double) monthsBeforeGrad / Math.max(total - 1, 1);
        int targetMonth = (int) Math.round(7.0 + index * monthStep);
        int month = ((targetMonth - 1) % 12) + 1;
        int yearOffset = (targetMonth - 1) / 12;
        int year = startYear + yearOffset;
        int day = random.nextInt(16) + 10;
        int hour = random.nextInt(9) + 9;
        return String.format("%04d-%02d-%02d %02d:00:00", year, month, day, hour);
    }

    // ==================== 面试时间 ====================

    public String generateInterviewTime(int gradYear) {
        int month = random.nextInt(4) + 3; // 3-6月
        int day = random.nextInt(25) + 5;
        int hour = random.nextInt(4) + 9; // 9-12时
        int minute = random.nextInt(2) == 0 ? 0 : 30;
        return String.format("%04d-%02d-%02d %02d:%02d:00",
                gradYear, month, day, hour, minute);
    }

    // ==================== 三方协议编号 ====================

    public String generateThreePartyNo(int gradYear, int studentSeq) {
        return String.format("TP%d%06d", gradYear, studentSeq);
    }

    // ==================== 薪资计算 ====================

    public String generateSalary(Integer salMin, Integer salMax) {
        if (salMin == null) salMin = 5000;
        if (salMax == null) salMax = 8000;
        int actual = random.nextInt(salMax - salMin + 1) + salMin;
        return actual + "元/月";
    }

    public String generateProbationSalary(int fullSalary) {
        return (int) (fullSalary * 0.8) + "元/月";
    }

    // ==================== 面试结果 ====================

    public String generateInterviewResult() {
        double r = random.nextDouble();
        if (r < 0.20) return "pending";    // 待定
        if (r < 0.85) return "passed";     // 通过 65%
        return "failed";                      // 未通过 15%
    }

    // ==================== 工具类 ====================

    public static class Pair<T1, T2> {
        public final T1 first;
        public final T2 second;
        public Pair(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }
    }
}
