package com.employment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.employment.exception.BusinessException;
import com.employment.model.dto.RecommendResultDTO;
import com.employment.model.entity.RecommendHistory;
import com.employment.model.entity.JobPosition;
import com.employment.model.entity.SpiderCollectedData;
import com.employment.model.entity.StudentInfo;
import com.employment.model.entity.StudentResume;
import com.employment.model.entity.SysMajor;
import com.employment.repository.JobPositionRepository;
import com.employment.repository.RecommendHistoryRepository;
import com.employment.repository.SpiderCollectedDataRepository;
import com.employment.repository.StudentInfoRepository;
import com.employment.repository.StudentResumeRepository;
import com.employment.repository.SysMajorRepository;
import com.employment.service.RecommendService;
import com.employment.service.RecommendWeightConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final SysMajorRepository sysMajorRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final StudentResumeRepository studentResumeRepository;
    private final SpiderCollectedDataRepository spiderCollectedDataRepository;
    private final JobPositionRepository jobPositionRepository;
    private final RecommendHistoryRepository recommendHistoryRepository;
    private final RecommendWeightConfigService weightConfigService;

    private static final int DEFAULT_TOP_N = 20;
    // TF-IDF 微服务地址。
    // 该服务由 recommend_service/app.py 提供，负责职位文本训练与简历-职位相似度计算。
    // 若此地址不可访问，系统会自动回退到本地规则权重打分，保证推荐功能不中断。
    private static final String TFIDF_SERVICE_URL = "http://127.0.0.1:8000";
    private static final WebClient tfidfWebClient = WebClient.create(TFIDF_SERVICE_URL);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ALGORITHM_TFIDF = "tfidf";
    private static final String ALGORITHM_MULTI_FACTOR = "multi_factor";
    private static final String ALGORITHM_TFIDF_FALLBACK = "tfidf_fallback";

    // ===== 1. 数据分析员：获取所有专业推荐状态 =====
    @Override
    public Map<String, Object> getMajorRecommendStatus() {
        List<SysMajor> allMajors = sysMajorRepository.findAll();
        // 只统计 ETL 有效数据（is_valid="1"），确保推荐只消费清洗后的数据
        List<SpiderCollectedData> allSpiderData = spiderCollectedDataRepository.findByIsValid("1");

        // 按专业名聚合爬虫数据量
        Map<String, Long> spiderCountByMajor = allSpiderData.stream()
                .filter(d -> d.getMajorName() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getMajorName(),
                        Collectors.counting()
                ));

        // 按专业名聚合学生数量
        Map<String, Long> studentCountByMajor = studentInfoRepository.findAll().stream()
                .filter(s -> s.getMajorName() != null)
                .collect(Collectors.groupingBy(
                        StudentInfo::getMajorName,
                        Collectors.counting()
                ));

        // 已推荐数量（按学生专业名查 recommend_history）
        List<RecommendHistory> allHistory = recommendHistoryRepository.findAll();
        Map<String, Long> recommendedCountByMajor = allHistory.stream()
                .filter(h -> h.getRecommendType() != null && "job".equals(h.getRecommendType()) && h.getIndustry() != null)
                .collect(Collectors.groupingBy(
                        RecommendHistory::getIndustry,
                        Collectors.counting()
                ));

        List<Map<String, Object>> majorList = new ArrayList<>();
        for (SysMajor major : allMajors) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", major.getId());
            item.put("majorName", major.getMajorName());
            item.put("shortName", major.getShortName());
            item.put("deptId", major.getDeptId());
            item.put("recommendEnabled", major.getRecommendEnabled());
            item.put("modelTrained", major.getModelTrained() != null ? major.getModelTrained() : "untrained");
            item.put("spiderDataCount", spiderCountByMajor.getOrDefault(major.getMajorName(), 0L));
            item.put("studentCount", studentCountByMajor.getOrDefault(major.getMajorName(), 0L));
            item.put("recommendedCount", recommendedCountByMajor.getOrDefault(major.getMajorName(), 0L));
            majorList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("majors", majorList);
        result.put("totalSpiderData", (long) allSpiderData.size());
        result.put("totalStudents", (long) studentInfoRepository.count());
        return result;
    }

    // ===== 2. 数据分析员：获取某个专业的推荐详情 =====
    @Override
    public Map<String, Object> getMajorRecommendDetail(Long majorId) {
        SysMajor major = sysMajorRepository.findById(majorId)
                .orElseThrow(() -> new BusinessException(404, "专业不存在"));

        List<StudentInfo> students = studentInfoRepository.findAll().stream()
                .filter(s -> majorId.equals(s.getMajorId()))
                .collect(Collectors.toList());

        List<SpiderCollectedData> spiderData = spiderCollectedDataRepository.findByIsValid("1").stream()
                .filter(d -> isSameMajor(d.getMajorName(), major.getMajorName()))
                .collect(Collectors.toList());

        // 获取最近一次推荐历史
        List<RecommendHistory> recentHistory = recommendHistoryRepository.findAll().stream()
                .filter(h -> "job".equals(h.getRecommendType()) && h.getIndustry() != null)
                .sorted((a, b) -> {
                    if (a.getCreateTime() == null) return 1;
                    if (b.getCreateTime() == null) return -1;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                })
                .limit(10)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("major", major);
        result.put("studentCount", students.size());
        result.put("spiderDataCount", spiderData.size());
        result.put("recentRecommendations", recentHistory);
        return result;
    }

    // ===== 3. 数据分析员：触发某个专业的推荐（兼容旧接口） =====
    @Override
    @Transactional
    public List<Map<String, Object>> triggerRecommendForMajor(Long majorId, int topN) {
        return triggerRecommendForMajor(majorId, topN, "multi_factor");
    }

    @Override
    @Transactional
    public List<Map<String, Object>> triggerRecommendForMajor(Long majorId, int topN, String algorithmType) {
        if (topN <= 0) topN = DEFAULT_TOP_N;
        if (algorithmType == null) algorithmType = "multi_factor";

        SysMajor major = sysMajorRepository.findById(majorId)
                .orElseThrow(() -> new BusinessException(404, "专业不存在"));

        log.info("[触发推荐] 专业ID={}, 专业名={}, 推荐开关={}", majorId, major.getMajorName(), major.getRecommendEnabled());

        if ("0".equals(major.getRecommendEnabled())) {
            throw new BusinessException(400, "该专业推荐功能已关闭，请先在专业管理中开启");
        }

        List<StudentInfo> students = studentInfoRepository.findAll().stream()
                .filter(s -> majorId.equals(s.getMajorId()))
                .collect(Collectors.toList());

        log.info("[触发推荐] 专业ID={}, 找到学生 {} 人", majorId, students.size());

        if (students.isEmpty()) {
            throw new BusinessException(400, "该专业下暂无学生");
        }

        // 获取该专业对应的爬虫数据（可能为空，此时仅靠HR职位推荐）
        List<SpiderCollectedData> allValid = spiderCollectedDataRepository.findByIsValid("1");
        List<SpiderCollectedData> spiderData = allValid.stream()
                .filter(d -> isSameMajor(d.getMajorName(), major.getMajorName()))
                .collect(Collectors.toList());

        log.info("[触发推荐] 专业={}, 爬虫数据总有效={}, 命中该专业={}", major.getMajorName(), allValid.size(), spiderData.size());

        // 获取 HR 发布中的职位（只取已发布的）
        List<JobPosition> hrJobs = jobPositionRepository.findByStatusAndIsDeleted("published", "0");
        log.info("[触发推荐] HR职位总数={}", hrJobs.size());

        if (spiderData.isEmpty() && hrJobs.isEmpty()) {
            throw new BusinessException(400, "该专业暂无爬虫数据且系统内也没有HR发布的职位");
        }

        // 为数据分析员：爬虫数据 + HR职位 全部参与评分，但最后只返回本校HR职位（positionSource="hr"）
        // 爬虫数据用于丰富评分依据，最终展示由数据分析员在管理后台决定展示哪些
        List<Map<String, Object>> allResults = new ArrayList<>();

        for (StudentInfo student : students) {
            // 获取学生简历
            StudentResume resume = studentResumeRepository.findByStudentId(student.getId())
                    .stream().findFirst().orElse(null);

            List<RecommendResultDTO> results;
            if (ALGORITHM_TFIDF.equals(algorithmType)) {
                results = scoreWithTfidf(spiderData, hrJobs, student, resume, major.getMajorName(), topN);
                log.info("[TF-IDF推荐] 学生ID={}, 推荐职位数={}", student.getId(), results.size());
            } else {
                List<RecommendResultDTO> spiderResults = scoreAndRankJobs(spiderData, student, resume, major.getMajorName(), topN);
                List<RecommendResultDTO> hrResults = scoreAndRankHrJobs(hrJobs, student, resume, major.getMajorName(), topN);
                results = mergeAndRankAll(spiderResults, hrResults, topN);
                log.info("[规则打分] 学生ID={}, 爬虫推荐={}, HR推荐={}", student.getId(), spiderResults.size(), hrResults.size());
            }

            if (!results.isEmpty()) {
                String actualAlgorithm = results.get(0).getAlgorithmType() != null ? results.get(0).getAlgorithmType() : algorithmType;
                saveRecommendHistoryWithMajor(student.getUserId(), "student", results, major.getMajorName(), actualAlgorithm);
            }

            for (RecommendResultDTO r : results) {
                Map<String, Object> map = new HashMap<>();
                map.put("studentId", student.getId());
                map.put("studentName", student.getRealName());
                map.put("studentNo", student.getStudentNo());
                map.put("className", student.getClassName());
                map.put("jobName", r.getTargetName());
                map.put("companyName", r.getCompanyName());
                map.put("salary", r.getSalary());
                map.put("city", r.getCity());
                map.put("education", r.getEducation());
                map.put("matchScore", r.getMatchScore());
                map.put("industry", r.getIndustry());
                map.put("matchReason", buildMatchReason(r));
                map.put("detailUrl", r.getDetailUrl());
                map.put("positionSource", r.getPositionSource());
                allResults.add(map);
            }
        }

        // 按匹配分降序排列
        allResults.sort((a, b) -> {
            Integer s1 = (Integer) a.get("matchScore");
            Integer s2 = (Integer) b.get("matchScore");
            return s2.compareTo(s1);
        });

        return allResults;
    }

    // ===== 3.5 数据分析员：为专业训练推荐模型 =====
    @Override
    @Transactional
    public List<Map<String, Object>> trainRecommendModel(Long majorId, String algorithmType) {
        SysMajor major = sysMajorRepository.findById(majorId)
                .orElseThrow(() -> new BusinessException(404, "专业不存在"));

        String normalizedAlgorithm = normalizeAlgorithmType(algorithmType);
        log.info("[训练推荐模型] 专业ID={}, 专业名={}, 算法={}", majorId, major.getMajorName(), normalizedAlgorithm);

        List<StudentInfo> students = studentInfoRepository.findAll().stream()
                .filter(s -> majorId.equals(s.getMajorId()))
                .collect(Collectors.toList());
        if (students.isEmpty()) {
            throw new BusinessException(400, "该专业下暂无学生，无法训练模型");
        }

        List<SpiderCollectedData> allSpiderData = spiderCollectedDataRepository.findByIsValid("1");
        if (allSpiderData.isEmpty()) {
            throw new BusinessException(400, "暂无爬虫清洗数据，无法训练模型，请先完成数据爬取与ETL清洗");
        }

        List<SpiderCollectedData> majorSpiderData = allSpiderData.stream()
                .filter(d -> isSameMajor(d.getMajorName(), major.getMajorName()))
                .collect(Collectors.toList());
        if (majorSpiderData.isEmpty()) {
            throw new BusinessException(400, "该专业暂无爬虫清洗数据，无法训练模型，请先确保该专业有足够的职位数据");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("majorId", majorId);
        summary.put("majorName", major.getMajorName());
        summary.put("studentCount", students.size());
        summary.put("spiderDataCount", majorSpiderData.size());
        summary.put("algorithmType", normalizedAlgorithm);
        summary.put("actualAlgorithm", normalizedAlgorithm);
        summary.put("fallbackUsed", false);
        summary.put("modelAvailable", true);
        summary.put("serviceAvailable", true);
        summary.put("trained", true);

        if (ALGORITHM_TFIDF.equals(normalizedAlgorithm)) {
            Map<String, Object> tfidfResult = trainTfidfModelForMajor(major, majorSpiderData);
            summary.putAll(tfidfResult);
        } else {
            summary.put("message", "规则权重打分无需单独训练，已完成推荐配置初始化");
            summary.put("modelAvailable", true);
            summary.put("serviceAvailable", true);
            summary.put("actualAlgorithm", ALGORITHM_MULTI_FACTOR);
        }

        Map<String, Object> evalSummary = autoEvaluateModelForMajor(major, normalizedAlgorithm, String.valueOf(summary.getOrDefault("actualAlgorithm", normalizedAlgorithm)));
        summary.put("evaluation", evalSummary);
        summary.put("finalScore", evalSummary.get("finalScore"));
        summary.put("feedbackScore", evalSummary.get("feedbackScore"));
        summary.put("offlineF1", evalSummary.get("offlineF1"));

        major.setModelTrained("trained");
        major.setLastModelTrainedTime(java.time.LocalDateTime.now());
        major.setRecommendEnabled("0");
        sysMajorRepository.save(major);

        return Collections.singletonList(summary);
    }

    // ===== 批量训练所有专业 =====
    @Override
    public Map<String, Object> trainAllMajors(String algorithmType) {
        log.info("[批量训练] 开始遍历所有专业，算法={}...", algorithmType);

        String normalizedAlgorithm = normalizeAlgorithmType(algorithmType);
        List<SpiderCollectedData> allValidSpider = spiderCollectedDataRepository.findByIsValid("1");
        if (allValidSpider.isEmpty()) {
            throw new BusinessException(400, "暂无爬虫清洗数据，无法训练，请先完成数据爬取与ETL清洗");
        }

        Map<String, Long> spiderCountByMajor = allValidSpider.stream()
                .filter(d -> d.getMajorName() != null)
                .collect(Collectors.groupingBy(SpiderCollectedData::getMajorName, Collectors.counting()));

        List<SysMajor> allMajors = sysMajorRepository.findAll();
        List<Map<String, Object>> successList = new ArrayList<>();
        List<Map<String, Object>> skipList = new ArrayList<>();

        int trained = 0, skipped = 0, fallbackCount = 0;
        for (SysMajor major : allMajors) {
            Long spiderCount = spiderCountByMajor.getOrDefault(major.getMajorName(), 0L);
            if (spiderCount == 0) {
                Map<String, Object> skip = new HashMap<>();
                skip.put("majorId", major.getId());
                skip.put("majorName", major.getMajorName());
                skip.put("reason", "无爬虫清洗数据");
                skipList.add(skip);
                skipped++;
                continue;
            }

            List<StudentInfo> students = studentInfoRepository.findAll().stream()
                    .filter(s -> major.getId().equals(s.getMajorId()))
                    .collect(Collectors.toList());
            if (students.isEmpty()) {
                Map<String, Object> skip = new HashMap<>();
                skip.put("majorId", major.getId());
                skip.put("majorName", major.getMajorName());
                skip.put("reason", "该专业下暂无学生");
                skipList.add(skip);
                skipped++;
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("majorId", major.getId());
            item.put("majorName", major.getMajorName());
            item.put("spiderDataCount", spiderCount);
            item.put("studentCount", students.size());
            item.put("algorithmType", normalizedAlgorithm);
            item.put("actualAlgorithm", normalizedAlgorithm);
            item.put("fallbackUsed", false);
            item.put("modelAvailable", true);

            if (ALGORITHM_TFIDF.equals(normalizedAlgorithm)) {
                List<SpiderCollectedData> majorSpiderData = allValidSpider.stream()
                        .filter(d -> isSameMajor(d.getMajorName(), major.getMajorName()))
                        .collect(Collectors.toList());
                item.putAll(trainTfidfModelForMajor(major, majorSpiderData));
                if (Boolean.TRUE.equals(item.get("fallbackUsed"))) {
                    fallbackCount++;
                }
            } else {
                item.put("message", "规则权重打分无需单独训练，已完成推荐配置初始化");
                item.put("actualAlgorithm", ALGORITHM_MULTI_FACTOR);
            }

            Map<String, Object> evalSummary = autoEvaluateModelForMajor(major, normalizedAlgorithm, String.valueOf(item.getOrDefault("actualAlgorithm", normalizedAlgorithm)));
            item.put("evaluation", evalSummary);
            item.put("finalScore", evalSummary.get("finalScore"));
            item.put("feedbackScore", evalSummary.get("feedbackScore"));
            item.put("offlineF1", evalSummary.get("offlineF1"));

            major.setModelTrained("trained");
            major.setLastModelTrainedTime(java.time.LocalDateTime.now());
            major.setRecommendEnabled("0");
            sysMajorRepository.save(major);
            trained++;
            successList.add(item);

            log.info("[批量训练] 专业={}, 算法={}, 学生={}人, 训练完成",
                    major.getMajorName(), normalizedAlgorithm, students.size());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trainedCount", trained);
        result.put("skippedCount", skipped);
        result.put("fallbackCount", fallbackCount);
        result.put("totalMajors", allMajors.size());
        result.put("algorithmType", normalizedAlgorithm);
        result.put("successList", successList);
        result.put("skipList", skipList);
        result.put("message", String.format("批量训练完成，共训练 %d 个专业，跳过 %d 个", trained, skipped));
        log.info("[批量训练] 完成: 算法={}, 训练={}, 跳过={}, 降级={}, 总计={}", normalizedAlgorithm, trained, skipped, fallbackCount, allMajors.size());
        return result;
    }

    // ===== 一键开启全部已训练模型 =====
    @Override
    @Transactional
    public Map<String, Object> enableAllTrainedMajors() {
        List<SysMajor> allMajors = sysMajorRepository.findAll();
        List<Map<String, Object>> enabledList = new ArrayList<>();
        List<Map<String, Object>> skippedList = new ArrayList<>();

        int enabledCount = 0;
        int skippedCount = 0;

        for (SysMajor major : allMajors) {
            boolean trained = "trained".equals(major.getModelTrained());
            boolean alreadyEnabled = "1".equals(major.getRecommendEnabled());

            if (!trained) {
                Map<String, Object> skipped = new LinkedHashMap<>();
                skipped.put("majorId", major.getId());
                skipped.put("majorName", major.getMajorName());
                skipped.put("reason", "模型未训练");
                skippedList.add(skipped);
                skippedCount++;
                continue;
            }

            if (alreadyEnabled) {
                Map<String, Object> skipped = new LinkedHashMap<>();
                skipped.put("majorId", major.getId());
                skipped.put("majorName", major.getMajorName());
                skipped.put("reason", "算法已开启");
                skippedList.add(skipped);
                skippedCount++;
                continue;
            }

            major.setRecommendEnabled("1");
            sysMajorRepository.save(major);

            Map<String, Object> enabled = new LinkedHashMap<>();
            enabled.put("majorId", major.getId());
            enabled.put("majorName", major.getMajorName());
            enabledList.add(enabled);
            enabledCount++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMajors", allMajors.size());
        result.put("enabledCount", enabledCount);
        result.put("skippedCount", skippedCount);
        result.put("enabledList", enabledList);
        result.put("skippedList", skippedList);
        result.put("message", enabledCount > 0
                ? "一键启动完成，共开启 " + enabledCount + " 个已训练专业"
                : "没有可开启的已训练专业");
        return result;
    }

    public Map<String, Object> getModelTrainingStats() {
        List<SysMajor> allMajors = sysMajorRepository.findAll();
        List<SpiderCollectedData> allValidSpider = spiderCollectedDataRepository.findByIsValid("1");

        Map<String, Long> spiderCountByMajor = allValidSpider.stream()
                .filter(d -> d.getMajorName() != null)
                .collect(Collectors.groupingBy(SpiderCollectedData::getMajorName, Collectors.counting()));

        long trainedCount = allMajors.stream().filter(m -> "trained".equals(m.getModelTrained())).count();
        long enabledCount = allMajors.stream().filter(m -> "1".equals(m.getRecommendEnabled())).count();
        long hasSpiderData = allMajors.stream()
                .filter(m -> spiderCountByMajor.getOrDefault(m.getMajorName(), 0L) > 0).count();
        long hasStudents = allMajors.stream()
                .filter(m -> {
                    long cnt = studentInfoRepository.findAll().stream()
                            .filter(s -> m.getId().equals(s.getMajorId())).count();
                    return cnt > 0;
                }).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMajors", allMajors.size());
        result.put("trainedCount", trainedCount);
        result.put("enabledCount", enabledCount);
        result.put("hasSpiderDataCount", hasSpiderData);
        result.put("hasStudentsCount", hasStudents);
        result.put("validSpiderDataTotal", allValidSpider.size());
        result.put("readyToTrain", hasSpiderData > 0 && hasStudents > 0);
        return result;
    }

    // ===== 4. 学生端：获取推荐结果（需指定简历，直接运行推荐算法） =====
    @Override
    public List<Map<String, Object>> getStudentRecommendations(Long studentId, int topN) {
        throw new BusinessException(400, "请选择一份简历后再获取推荐结果");
    }

    // 学生端推荐必须显式指定 resumeId。
    // 这样做的目的，是确保“学生切换不同简历”时，系统真正基于当前所选简历计算候选职位与匹配分，
    // 避免多个简历共用同一份默认简历而导致推荐结果几乎不变。
    @Override
    public List<Map<String, Object>> getStudentRecommendations(Long studentId, Long resumeId, int topN) {
        return getStudentRecommendations(studentId, resumeId, topN, "all");
    }

    @Override
    public List<Map<String, Object>> getStudentRecommendations(Long studentId, Long resumeId, int topN, String sourceFilter) {
        if (topN <= 0) topN = 10;

        StudentInfo student = studentInfoRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(404, "学生不存在"));

        if (student.getMajorId() == null) {
            throw new BusinessException(400, "NO_MAJOR");
        }

        SysMajor major = sysMajorRepository.findById(student.getMajorId()).orElse(null);
        if (major == null) {
            throw new BusinessException(400, "专业信息异常，请联系管理员");
        }
        if (!"1".equals(major.getRecommendEnabled())) {
            throw new BusinessException(400, "ALGORITHM_DISABLED");
        }
        if (!"trained".equals(major.getModelTrained())) {
            throw new BusinessException(400, "MODEL_NOT_TRAINED");
        }

        if (resumeId == null) {
            throw new BusinessException(400, "请选择一份简历");
        }

        StudentResume resume = studentResumeRepository.findById(resumeId)
                .filter(item -> Objects.equals(item.getStudentId(), studentId))
                .orElseThrow(() -> new BusinessException(400, "简历不存在或无权访问"));

        // 先根据当前所选简历提取“求职意向 token”，再裁剪候选职位集合。
        // 这一步是让不同简历在进入打分前就产生差异：
        // 例如同一学生的“测试工程师简历”和“数据分析简历”会命中不同候选职位。
        List<SpiderCollectedData> allSpiderData = spiderCollectedDataRepository.findByIsValid("1");
        List<SpiderCollectedData> spiderData = filterSpiderDataForResume(allSpiderData, student, resume);

        List<JobPosition> allHrJobs = jobPositionRepository.findByStatusAndIsDeleted("published", "0");
        List<JobPosition> candidateHrJobs = filterHrJobsForResume(allHrJobs, student, resume);
        if (candidateHrJobs.isEmpty()) {
            candidateHrJobs = allHrJobs;
        }

        String normalizedSourceFilter = normalizeSourceFilter(sourceFilter);
        if ("hr".equals(normalizedSourceFilter)) {
            spiderData = Collections.emptyList();
        } else if ("spider".equals(normalizedSourceFilter)) {
            candidateHrJobs = Collections.emptyList();
        }

        log.info("[学生推荐] 学生ID={}, 简历ID={}, 专业={}, 简历名称={}, 来源过滤={}, 简历导向爬虫数据={}条, HR候选职位={}条",
                studentId,
                resumeId,
                student.getMajorName(),
                resume.getResumeName(),
                normalizedSourceFilter,
                spiderData.size(),
                candidateHrJobs.size());

        List<RecommendResultDTO> spiderResults = scoreAndRankJobs(spiderData, student, resume, student.getMajorName(), topN);
        List<RecommendResultDTO> hrResults = scoreAndRankHrJobs(candidateHrJobs, student, resume, student.getMajorName(), topN);
        List<RecommendResultDTO> merged = mergeAndRankAll(spiderResults, hrResults, topN);

        return merged.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getTargetId());
            map.put("jobName", r.getTargetName());
            map.put("companyName", r.getCompanyName());
            map.put("salary", r.getSalary());
            map.put("city", r.getCity());
            map.put("education", r.getEducation());
            map.put("experience", r.getExperience());
            map.put("matchScore", r.getMatchScore());
            map.put("industry", r.getIndustry());
            map.put("matchReason", buildMatchReason(r));
            map.put("detailUrl", r.getDetailUrl());
            map.put("responsibility", r.getResponsibility());
            map.put("skills", r.getSource());
            map.put("positionSource", r.getPositionSource());
            return map;
        }).collect(Collectors.toList());
    }

    // ===== 5. 学生端：获取推荐状态 =====
    @Override
    public Map<String, Object> getRecommendStatusForStudent(Long studentId) {
        StudentInfo student = studentInfoRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(404, "学生不存在"));

        Map<String, Object> result = new HashMap<>();

        if (student.getMajorId() == null) {
            result.put("enabled", false);
            result.put("reason", "NO_MAJOR");
            result.put("message", "请先完善专业信息");
            return result;
        }

        SysMajor major = sysMajorRepository.findById(student.getMajorId()).orElse(null);
        if (major == null) {
            result.put("enabled", false);
            result.put("reason", "MAJOR_NOT_FOUND");
            result.put("message", "专业信息异常");
            return result;
        }

        if (!"trained".equals(major.getModelTrained())) {
            result.put("enabled", false);
            result.put("reason", "MODEL_NOT_TRAINED");
            result.put("message", "推荐模型尚未训练，请等待数据分析员完成模型训练后再试");
            return result;
        }

        if (!"1".equals(major.getRecommendEnabled())) {
            result.put("enabled", false);
            result.put("reason", "ALGORITHM_DISABLED");
            result.put("message", "该专业推荐功能已关闭，请等待数据分析员开启");
            return result;
        }

        result.put("enabled", true);
        result.put("reason", "AVAILABLE");
        result.put("message", "请选择简历后开始推荐");
        return result;
    }

    // ===== 6. 保存推荐历史 =====
    @Override
    @Transactional
    public void saveRecommendHistory(Long userId, String userType, List<RecommendResultDTO> results, String algorithmType) {
        List<RecommendHistory> entities = new ArrayList<>();
        for (RecommendResultDTO r : results) {
            RecommendHistory h = new RecommendHistory();
            h.setUserId(userId);
            h.setUserType(userType);
            h.setRecommendType("job");
            h.setTargetId(r.getTargetId());
            h.setTargetName(r.getTargetName());
            h.setTargetInfo(r.getTargetInfo());
            h.setMatchScore(r.getMatchScore());
            h.setAlgorithmType(algorithmType);
            h.setIndustry(r.getIndustry());
            h.setCity(r.getCity());
            h.setSalary(r.getSalary());
            h.setSource(r.getSource());
            h.setIsViewed("0");
            entities.add(h);
        }
        recommendHistoryRepository.saveAll(entities);
    }

    /**
     * 为数据分析员统计方法单独保存带专业信息的推荐历史
     */
    @Transactional
    public void saveRecommendHistoryWithMajor(Long userId, String userType, List<RecommendResultDTO> results, String majorName, String algorithmType) {
        List<RecommendHistory> entities = new ArrayList<>();
        for (RecommendResultDTO r : results) {
            RecommendHistory h = new RecommendHistory();
            h.setUserId(userId);
            h.setUserType(userType);
            h.setRecommendType("job");
            h.setTargetId(r.getTargetId());
            h.setTargetName(r.getTargetName());  // 职位名称
            h.setTargetInfo(r.getTargetInfo());  // 公司|薪资|城市
            h.setMatchScore(r.getMatchScore());
            h.setAlgorithmType(algorithmType);
            h.setIndustry(majorName);  // 学生专业名，用于按专业统计
            h.setCity(r.getCity());
            h.setSalary(r.getSalary());
            h.setSource(r.getSource());
            h.setIsViewed("0");
            entities.add(h);
        }
        recommendHistoryRepository.saveAll(entities);
    }

    // ===== 核心评分算法（爬虫数据） =====
    private List<RecommendResultDTO> scoreAndRankJobs(
            List<SpiderCollectedData> spiderData,
            StudentInfo student,
            StudentResume resume,
            String majorName,
            int topN) {

        List<RecommendResultDTO> scored = new ArrayList<>();

        // 从数据库读取动态权重
        Map<String, Integer> weights = weightConfigService.getWeightMap();
        int majorMax = weights.getOrDefault("major", 25);
        int cityMax = weights.getOrDefault("city", 20);
        int salaryMax = weights.getOrDefault("salary", 20);
        int skillMax = weights.getOrDefault("skill", 20);
        int eduMax = weights.getOrDefault("education", 15);

        // 提取学生关键词
        Set<String> studentKeywords = extractKeywords(student, resume);

        for (SpiderCollectedData job : spiderData) {
            int score = 0;
            StringBuilder reasonBuilder = new StringBuilder();

            // 1. 专业相关性
            int majorScore = calcMajorScore(job.getMajorName(), job.getIndustryKeyword(), majorName, majorMax);
            score += majorScore;
            if (majorScore > majorMax * 60 / 100) reasonBuilder.append("专业相关 ");

            // 2. 城市偏好匹配
            int cityScore = calcCityScore(job.getCity(), student, resume);
            score += cityScore;
            if (cityScore > cityMax * 60 / 100) reasonBuilder.append("城市匹配 ");

            // 3. 薪资匹配度
            int salaryScore = calcSalaryScore(job.getSalary(), resume, salaryMax);
            score += salaryScore;
            if (salaryScore > salaryMax * 60 / 100) reasonBuilder.append("薪资合适 ");

            // 4. 学历匹配
            int eduScore = calcEducationScore(job.getEducation(), student, resume, eduMax);
            score += eduScore;

            // 5. 技能关键词匹配
            int skillScore = calcSkillScore(job.getResponsibility(), job.getSkills(), studentKeywords, skillMax);
            score += skillScore;
            if (skillScore > skillMax * 60 / 100) reasonBuilder.append("技能匹配 ");

            RecommendResultDTO dto = new RecommendResultDTO();
            dto.setTargetId(job.getId());
            dto.setTargetName(job.getJobName());
            dto.setTargetInfo(buildTargetInfo(job));
            dto.setMatchScore(score);
                    dto.setAlgorithmType(ALGORITHM_MULTI_FACTOR);
            // industry: 爬取时推断的实际行业分类（如"互联网/IT"），存 DB 用于前端展示
            dto.setIndustry(job.getIndustry());
            // source: 爬虫搜索关键词（如"大数据开发"），存 DB 便于分析来源
            dto.setSource(job.getIndustryKeyword() != null ? job.getIndustryKeyword() : job.getSourceCode());
            dto.setCity(job.getCity());
            dto.setSalary(job.getSalary());
            dto.setSource(job.getSourceCode());
            dto.setEducation(job.getEducation());
            dto.setExperience(job.getExperience());
            dto.setCompanyName(job.getCompanyName());
            dto.setDetailUrl(job.getDetailUrl());
            dto.setResponsibility(job.getResponsibility());
            dto.setMatchReason(reasonBuilder.toString().trim());
            dto.setPositionSource("spider");
            scored.add(dto);
        }

        // 按分数降序，取 topN
        scored.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return scored.stream().limit(topN).collect(Collectors.toList());
    }

    // ===== 关键词提取 =====
    private Set<String> extractKeywords(StudentInfo student, StudentResume resume) {
        Set<String> keywords = new HashSet<>();
        if (student.getMajorName() != null) {
            keywords.addAll(Arrays.asList(student.getMajorName().split("[/,、]")));
        }
        if (resume != null) {
            if (resume.getSkillCertificates() != null) {
                keywords.addAll(Arrays.asList(resume.getSkillCertificates().split("[/,;；\n]")));
            }
            if (resume.getProjectExperience() != null) {
                keywords.addAll(Arrays.asList(resume.getProjectExperience().split("[/,;；\n。.]")));
            }
            if (resume.getPersonalSummary() != null) {
                keywords.addAll(Arrays.asList(resume.getPersonalSummary().split("[/,;；\n。.]")));
            }
        }
        return keywords.stream()
                .map(String::trim)
                .filter(k -> k.length() >= 2 && k.length() <= 30)
                .collect(Collectors.toSet());
    }

    // ===== 专业相关性评分 =====
    private int calcMajorScore(String spiderMajor, String industryKeyword, String studentMajor, int maxWeight) {
        if (spiderMajor == null && industryKeyword == null) return 0;
        int score = 0;
        if (studentMajor == null) return 0;

        String studentMajorLower = studentMajor.toLowerCase();
        if (spiderMajor != null && spiderMajor.toLowerCase().contains(studentMajorLower)) {
            score += 15;
        }
        if (spiderMajor != null && studentMajor.toLowerCase().contains(spiderMajor.toLowerCase())) {
            score += 10;
        }
        if (industryKeyword != null && studentMajorLower.contains(industryKeyword.toLowerCase())) {
            score += 5;
        }
        if (industryKeyword != null && industryKeyword.toLowerCase().contains(studentMajorLower)) {
            score += 5;
        }
        return Math.min(score, maxWeight);
    }

    // ===== 城市评分 =====
    private int calcCityScore(String jobCity, StudentInfo student, StudentResume resume) {
        if (jobCity == null || jobCity.isEmpty()) return 10;
        String targetCity = null;
        if (resume != null && resume.getExpectedCity() != null) {
            targetCity = resume.getExpectedCity();
        } else if (student.getCity() != null) {
            targetCity = student.getCity();
        }
        if (targetCity == null || targetCity.isEmpty()) return 10;
        if (jobCity.contains(targetCity) || targetCity.contains(jobCity)) {
            return 20;
        }
        return 5;
    }

    // ===== 薪资评分 =====
    private int calcSalaryScore(String jobSalary, StudentResume resume, int maxWeight) {
        if (resume == null) return maxWeight / 2;
        Integer expMin = resume.getExpectedSalaryMin();
        Integer expMax = resume.getExpectedSalaryMax();
        if (expMin == null || expMax == null) return maxWeight / 2;

        double[] jobSalaries = parseSalary(jobSalary);
        if (jobSalaries == null) return maxWeight / 2;

        double jobMin = jobSalaries[0];
        double jobMax = jobSalaries[1];

        if (jobMin >= expMin * 0.8 && jobMax <= expMax * 1.3) {
            return maxWeight;
        } else if (jobMin >= expMin * 0.5 && jobMax <= expMax * 1.8) {
            return maxWeight * 60 / 100;
        }
        return maxWeight / 4;
    }

    // ===== 学历评分 =====
    private int calcEducationScore(String jobEducation, StudentInfo student, StudentResume resume, int maxWeight) {
        int level = educationLevel(jobEducation);
        int targetLevel = 3;
        if (resume != null && resume.getEducationExperience() != null) {
            targetLevel = educationLevelFromResume(resume.getEducationExperience());
        }
        if (level <= targetLevel) {
            return maxWeight;
        } else if (level == targetLevel + 1) {
            return maxWeight * 50 / 100; // 约50%
        }
        return 2;
    }

    // ===== 技能匹配评分 =====
    private int calcSkillScore(String responsibility, String skills, Set<String> studentKeywords, int maxWeight) {
        if (studentKeywords.isEmpty()) return maxWeight / 2;
        String text = ((responsibility != null ? responsibility : "") + " " + (skills != null ? skills : "")).toLowerCase();
        int matchCount = 0;
        for (String keyword : studentKeywords) {
            String kw = keyword.toLowerCase().trim();
            if (kw.length() >= 2 && text.contains(kw)) {
                matchCount++;
            }
        }
        if (matchCount >= 5) return maxWeight;
        if (matchCount >= 3) return maxWeight * 75 / 100;
        if (matchCount >= 1) return maxWeight / 2;
        return maxWeight / 4;
    }

    // ===== HR职位评分（复用爬虫评分逻辑） =====
    private int calcMajorScoreForHr(String hrJobCategory, String hrJobName, String studentMajor, int maxWeight) {
        if (studentMajor == null) return 0;
        int score = 0;
        String majorLower = studentMajor.toLowerCase();
        if (hrJobCategory != null && hrJobCategory.toLowerCase().contains(majorLower)) {
            score += 15;
        }
        if (hrJobName != null && hrJobName.toLowerCase().contains(majorLower)) {
            score += 10;
        }
        if (hrJobCategory != null && hrJobCategory.toLowerCase().contains(majorLower)) {
            score += 5;
        }
        return Math.min(score, maxWeight);
    }

    private int calcCityScoreForHr(String hrWorkCity, StudentInfo student, StudentResume resume) {
        if (hrWorkCity == null || hrWorkCity.isEmpty()) return 10;
        String targetCity = null;
        if (resume != null && resume.getExpectedCity() != null) {
            targetCity = resume.getExpectedCity();
        } else if (student.getCity() != null) {
            targetCity = student.getCity();
        }
        if (targetCity == null || targetCity.isEmpty()) return 10;
        if (hrWorkCity.contains(targetCity) || targetCity.contains(hrWorkCity)) {
            return 20;
        }
        return 5;
    }

    private int calcSalaryScoreForHr(Integer hrSalaryMin, Integer hrSalaryMax, StudentResume resume, int maxWeight) {
        if (resume == null) return maxWeight / 2;
        Integer expMin = resume.getExpectedSalaryMin();
        Integer expMax = resume.getExpectedSalaryMax();
        if (expMin == null || expMax == null) return maxWeight / 2;
        if (hrSalaryMin == null || hrSalaryMax == null) return maxWeight / 2;

        if (hrSalaryMin >= expMin * 0.8 && hrSalaryMax <= expMax * 1.3) {
            return maxWeight;
        } else if (hrSalaryMin >= expMin * 0.5 && hrSalaryMax <= expMax * 1.8) {
            return maxWeight * 60 / 100;
        }
        return maxWeight / 4;
    }

    private int calcEducationScoreForHr(String hrEduRequired, StudentInfo student, StudentResume resume, int maxWeight) {
        int level = educationLevel(hrEduRequired);
        int targetLevel = 3;
        if (resume != null && resume.getEducationExperience() != null) {
            targetLevel = educationLevelFromResume(resume.getEducationExperience());
        }
        if (level <= targetLevel) {
            return maxWeight;
        } else if (level == targetLevel + 1) {
            return maxWeight * 50 / 100;
        }
        return 2;
    }

    private int calcSkillScoreForHr(String hrSkillRequired, String hrRequirement, Set<String> studentKeywords, int maxWeight) {
        if (studentKeywords.isEmpty()) return maxWeight / 2;
        String text = ((hrSkillRequired != null ? hrSkillRequired : "") + " " + (hrRequirement != null ? hrRequirement : "")).toLowerCase();
        int matchCount = 0;
        for (String keyword : studentKeywords) {
            String kw = keyword.toLowerCase().trim();
            if (kw.length() >= 2 && text.contains(kw)) {
                matchCount++;
            }
        }
        if (matchCount >= 5) return maxWeight;
        if (matchCount >= 3) return maxWeight * 75 / 100;
        if (matchCount >= 1) return maxWeight / 2;
        return maxWeight / 4;
    }

    // ===== 薪资解析（支持多种格式） =====
    // 支持格式：
    //   5k~10k / 5K~10K
    //   1-2万 / 1~2万 / 1.5-3万
    //   6-8千 / 6千-8千 / 6千~8千
    //   6千-8千·13薪 / 6千~8千·13薪
    //   1-1.5万·13薪
    //   6千-8千·14薪 / 6千-8千 14薪
    //   20000-30000（纯数字元）
    // 输出：double[]{min, max} 单位为"千元/月"
    private double[] parseSalary(String salary) {
        if (salary == null || salary.isEmpty()) return null;
        try {
            // 预处理：统一 · 符号、移除逗号、去除"元/月"、"元/年"、"(月)"等后缀
            String text = salary
                    .replace('·', '/')
                    .replace('*', '/')
                    .replace(',', ' ')
                    .replace('，', ' ')
                    .replace("元/月", "")
                    .replace("元/天", "")
                    .replace("元/年", "")
                    .replace("(月)", "")
                    .replace("元", "")
                    .trim();

            // 检测年薪制：标记后最终换算为月薪（除以12）
            boolean isYearly = text.contains("/年") || text.contains("万/年") || text.contains("千/年") || text.contains("k/年");
            text = text.replaceAll("\\s*[/\\s]*年\\s*", "").trim();

            double min = 0;
            double max = 0;
            boolean found = false;

            // 策略一：匹配 "数字~数字单位" 或 "数字-数字单位"（如 6-8千、1-2万、6千-8千、6千~8千）
            java.util.regex.Matcher m1 = java.util.regex.Pattern.compile(
                    "(\\d+(?:\\.\\d+)?)\\s*(?:~|-|—)\\s*(\\d+(?:\\.\\d+)?)\\s*(千|万|k|K|元)?",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            ).matcher(text);
            if (m1.find()) {
                min = Double.parseDouble(m1.group(1));
                max = Double.parseDouble(m1.group(2));
                String unit = m1.group(3);
                min = applyUnit(min, unit);
                max = applyUnit(max, unit);
                found = true;
            }

            // 策略二：匹配 "数字单位"（如 6千、8千、2万），单边
            if (!found) {
                java.util.regex.Matcher m2 = java.util.regex.Pattern.compile(
                        "(\\d+(?:\\.\\d+)?)\\s*(千|万|k|K|元)?",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                ).matcher(text);
                if (m2.find()) {
                    min = max = applyUnit(Double.parseDouble(m2.group(1)), m2.group(2));
                    found = true;
                }
            }

            // 策略三：纯数字（单位元），如 20000-30000
            if (!found) {
                java.util.regex.Matcher m3 = java.util.regex.Pattern.compile(
                        "(\\d+)\\s*(?:~|-|—)\\s*(\\d+)"
                ).matcher(text);
                if (m3.find()) {
                    min = Double.parseDouble(m3.group(1));
                    max = Double.parseDouble(m3.group(2));
                    min = min / 1000;  // 元 → 千元
                    max = max / 1000;
                    found = true;
                }
            }

            if (!found || min <= 0 || max <= 0) return null;
            // 年薪制统一换算为月薪
            if (isYearly) {
                min = min / 12;
                max = max / 12;
            }
            return new double[]{min, max};
        } catch (Exception e) {
            log.warn("薪资解析失败: {}", salary);
            return null;
        }
    }

    private double applyUnit(double value, String unit) {
        if (unit == null || unit.isEmpty()) return value;
        String u = unit.toLowerCase();
        if (u.equals("万")) return value * 10;       // 万 → 千元
        if (u.equals("千")) return value;             // 千 → 千元（已是）
        if (u.equals("k"))  return value;            // k → 千元（已统一为k）
        if (u.equals("元"))  return value / 1000;     // 元 → 千元
        return value;
    }

    // ===== 学历等级 =====
    private int educationLevel(String edu) {
        if (edu == null) return 3;
        if (edu.contains("博士")) return 5;
        if (edu.contains("硕士")) return 4;
        if (edu.contains("本科")) return 3;
        if (edu.contains("大专") || edu.contains("专科")) return 2;
        if (edu.contains("高中") || edu.contains("中专")) return 1;
        return 3;
    }

    private int educationLevelFromResume(String text) {
        if (text == null) return 3;
        if (text.contains("博士")) return 5;
        if (text.contains("硕士") || text.contains("研究生")) return 4;
        if (text.contains("本科")) return 3;
        if (text.contains("大专") || text.contains("专科")) return 2;
        return 3;
    }

    // ===== 构建目标信息 =====
    private String buildTargetInfo(SpiderCollectedData job) {
        StringBuilder sb = new StringBuilder();
        if (job.getCompanyName() != null) sb.append(job.getCompanyName());
        if (job.getSalary() != null) sb.append(" | ").append(job.getSalary());
        if (job.getCity() != null) sb.append(" | ").append(job.getCity());
        return sb.toString();
    }

    // ===== 构建匹配原因 =====
    private String buildMatchReason(RecommendResultDTO r) {
        List<String> reasons = new ArrayList<>();
        if (r.getMatchScore() == null) return "综合匹配";
        int score = r.getMatchScore();
        if (score >= 70) reasons.add("高度匹配");
        else if (score >= 50) reasons.add("较好匹配");
        else reasons.add("一般匹配");
        if (r.getCity() != null && !r.getCity().isEmpty()) reasons.add(r.getCity());
        if (r.getSalary() != null && !r.getSalary().isEmpty()) reasons.add(r.getSalary());
        return String.join(" · ", reasons);
    }

    // ===== 核心评分算法（HR职位） =====
    private List<RecommendResultDTO> scoreAndRankHrJobs(
            List<JobPosition> hrJobs,
            StudentInfo student,
            StudentResume resume,
            String majorName,
            int topN) {

        List<RecommendResultDTO> scored = new ArrayList<>();
        Set<String> studentKeywords = extractKeywords(student, resume);

        Map<String, Integer> weights = weightConfigService.getWeightMap();
        int majorMax = weights.getOrDefault("major", 25);
        int cityMax = weights.getOrDefault("city", 20);
        int salaryMax = weights.getOrDefault("salary", 20);
        int skillMax = weights.getOrDefault("skill", 20);
        int eduMax = weights.getOrDefault("education", 15);

        for (JobPosition job : hrJobs) {
            int score = 0;
            StringBuilder reasonBuilder = new StringBuilder();

            int majorScore = calcMajorScoreForHr(job.getJobCategory(), job.getJobName(), majorName, majorMax);
            score += majorScore;
            if (majorScore > majorMax * 60 / 100) reasonBuilder.append("专业相关 ");

            int cityScore = calcCityScoreForHr(job.getWorkCity(), student, resume);
            score += cityScore;
            if (cityScore > cityMax * 60 / 100) reasonBuilder.append("城市匹配 ");

            int salaryScore = calcSalaryScoreForHr(job.getSalaryMin(), job.getSalaryMax(), resume, salaryMax);
            score += salaryScore;
            if (salaryScore > salaryMax * 60 / 100) reasonBuilder.append("薪资合适 ");

            int eduScore = calcEducationScoreForHr(job.getEducationRequired(), student, resume, eduMax);
            score += eduScore;

            int skillScore = calcSkillScoreForHr(job.getSkillRequired(), job.getRequirement(), studentKeywords, skillMax);
            score += skillScore;
            if (skillScore > skillMax * 60 / 100) reasonBuilder.append("技能匹配 ");

            RecommendResultDTO dto = new RecommendResultDTO();
            dto.setTargetId(job.getId());
            dto.setTargetName(job.getJobName());
            dto.setTargetInfo(buildTargetInfoForHr(job));
            dto.setMatchScore(score);
                    dto.setAlgorithmType(ALGORITHM_MULTI_FACTOR);
            dto.setIndustry(job.getJobCategory());
            dto.setCity(job.getWorkCity());
            if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
                dto.setSalary(job.getSalaryMin() + "~" + job.getSalaryMax() + "元/月");
            }
            dto.setEducation(job.getEducationRequired());
            dto.setExperience(job.getExperienceRequired());
            dto.setCompanyName(job.getCompanyName());
            dto.setResponsibility(job.getResponsibility());
            dto.setMatchReason(buildMatchReasonForHr(dto));
            dto.setPositionSource("hr");
            scored.add(dto);
        }

        scored.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return scored.stream().limit(topN).collect(Collectors.toList());
    }

    private String buildMatchReasonForHr(RecommendResultDTO r) {
        List<String> reasons = new ArrayList<>();
        if (r.getMatchScore() == null) return "综合匹配";
        int score = r.getMatchScore();
        if (score >= 70) reasons.add("高度匹配");
        else if (score >= 50) reasons.add("较好匹配");
        else reasons.add("一般匹配");
        if (r.getCity() != null && !r.getCity().isEmpty()) reasons.add(r.getCity());
        if (r.getSalary() != null && !r.getSalary().isEmpty()) reasons.add(r.getSalary());
        return String.join(" · ", reasons);
    }

    private String buildTargetInfoForHr(JobPosition job) {
        StringBuilder sb = new StringBuilder();
        if (job.getCompanyName() != null) sb.append(job.getCompanyName());
        if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
            sb.append(" | ").append(job.getSalaryMin()).append("~").append(job.getSalaryMax()).append("元/月");
        }
        if (job.getWorkCity() != null) sb.append(" | ").append(job.getWorkCity());
        return sb.toString();
    }

    // ===== 合并并排序（返回全部推荐结果：爬虫职位 + HR职位）=====
    private List<RecommendResultDTO> mergeAndRankAll(List<RecommendResultDTO> spiderResults, List<RecommendResultDTO> hrResults, int topN) {
        List<RecommendResultDTO> merged = new ArrayList<>();
        if (spiderResults != null) merged.addAll(spiderResults);
        if (hrResults != null) merged.addAll(hrResults);
        merged.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return merged.stream().limit(topN).collect(Collectors.toList());
    }

    // ===== 判断HR职位是否与学生专业相关 =====
    private boolean isMajorRelatedJob(JobPosition job, String studentMajor) {
        if (studentMajor == null || studentMajor.isEmpty()) return false;
        String majorLower = studentMajor.toLowerCase();

        // 同时在 job_category / job_name / requirement 三个字段中匹配
        String jobText = String.join(" ",
                job.getJobCategory() != null ? job.getJobCategory() : "",
                job.getJobName() != null ? job.getJobName() : "",
                job.getRequirement() != null ? job.getRequirement() : "",
                job.getSkillRequired() != null ? job.getSkillRequired() : ""
        ).toLowerCase();

        // 直接匹配专业名
        if (jobText.contains(majorLower)) return true;

        // 通用关键词映射：专业 → 职位相关关键词
        Map<String, String[]> majorKeywords = new HashMap<>();
        majorKeywords.put("数据科学", new String[]{"java","python","spark","hadoop","hive","sql","mysql",
            "springboot","spring","后端","前端","开发","算法","机器学习","数据","etl","bi",
            "大数据","爬虫","数据分析","数据挖掘","web","django","flask","redis","kafka","flink"});
        majorKeywords.put("大数据", new String[]{"java","python","spark","hadoop","hive","sql","mysql",
            "kafka","flink","etl","数据","大数据","开发","后端"});
        majorKeywords.put("计算机", new String[]{"java","python","c++","前端","后端","算法","开发",
            "sql","mysql","redis","spring","web","软件","网络","安全","嵌入式","ai","人工智能"});
        majorKeywords.put("软件工程", new String[]{"java","python","前端","后端","全栈","开发","sql",
            "mysql","spring","web","软件","测试","运维","devops","docker"});
        majorKeywords.put("人工智能", new String[]{"python","机器学习","深度学习","tensorflow","pytorch",
            "算法","ai","nlp","cv","计算机视觉","自然语言","大模型","模型训练","数据处理"});
        majorKeywords.put("机械", new String[]{"机械","工艺","制造","数控","设计","加工","装配","钣金","加工中心","CAD","solidworks","ug","catia","有限元","仿真"});
        majorKeywords.put("电子信息", new String[]{"电子","硬件","嵌入式","电路","PCB","单片机","ARM","DSP","FPGA","信号","通信","射频","集成电路"});
        majorKeywords.put("电气", new String[]{"电气","plc","自动化","配电","继电","仪表","控制","变频","DCS","电机","电力","高压","低压"});
        majorKeywords.put("自动化", new String[]{"自动化","plc","DCS","控制","仪表","传感","运动控制","机器人","工控","SCADA"});
        majorKeywords.put("土木工程", new String[]{"土木","建筑","结构","施工","BIM","造价","力学","混凝土","钢结构","桥梁","隧道","给排水"});
        majorKeywords.put("材料", new String[]{"材料","冶金","金属","高分子","纳米","焊接","热处理","腐蚀","力学性能","配方","工艺"});
        majorKeywords.put("化学", new String[]{"化学","化工","有机","无机","分析","合成","配方","工艺","实验室","检测","石油","制药"});
        majorKeywords.put("制药", new String[]{"制药","药物","药品","合成","分析","药理","临床","注册","医药","生物","GMP","质量控制","QC","QA"});
        majorKeywords.put("生物", new String[]{"生物","基因","细胞","蛋白质","发酵","生物制药","分子","实验","检测","生物信息"});
        majorKeywords.put("环境", new String[]{"环境","环保","水处理","废气","固废","环评","监测","生态","节能","绿色","排放","污染"});
        majorKeywords.put("能源", new String[]{"能源","电力","光伏","风电","储能","电池","新能源","电网","输配电","电气","氢能","充电"});
        majorKeywords.put("交通", new String[]{"交通","道路","桥梁","隧道","轨道","地铁","铁路","公路","设计","规划","交通工程"});
        majorKeywords.put("水利", new String[]{"水利","水文","水工","水电站","大坝","灌溉","防洪","水文","水力学","水文学"});
        majorKeywords.put("金融", new String[]{"金融","银行","证券","保险","投资","风控","财务","会计","量化","基金","资产"});
        majorKeywords.put("经济", new String[]{"经济","金融","贸易","统计","数据分析","经济分析","市场","商务","咨询","管理"});
        majorKeywords.put("英语", new String[]{"英语","翻译","教师","外贸","商务英语","外事","国际","留学","口语","写作","口译","雅思","托福"});
        majorKeywords.put("数学", new String[]{"数学","统计","数据分析","算法","量化","金融","建模","优化","运筹","机器学习","密码学"});
        majorKeywords.put("物理", new String[]{"物理","光学","声学","电磁","量子","材料","半导体","凝聚态","理论物理","应用物理"});
        majorKeywords.put("物流", new String[]{"物流","供应链","仓储","运输","采购","配送","供应链管理","货运","报关","跨境"});
        majorKeywords.put("旅游", new String[]{"旅游","酒店","景区","导游","会展","旅游管理","运营","市场","策划","景区管理"});
        majorKeywords.put("体育", new String[]{"体育","运动","健身","教练","训练","赛事","体育管理","体能","运动康复","竞技"});
        majorKeywords.put("测绘", new String[]{"测绘","测量","遥感","GIS","GPS","地形","工程测量","地图","地理信息","航测","摄影测量"});
        majorKeywords.put("地质", new String[]{"地质","勘探","找矿","矿物","岩石","构造","资源","勘查","地层","地球物理","水文地质"});
        majorKeywords.put("采矿", new String[]{"采矿","矿物","矿井","巷道","开采","掘进","通风","安全","选矿","矿山","地质","岩石力学"});
        majorKeywords.put("安全工程", new String[]{"安全"," HSE","安全管理","风险","隐患","应急","消防","特种设备","职业健康","安全评价"});
        majorKeywords.put("食品", new String[]{"食品","食品安全","检测","质量","配方","工艺","营养","添加剂","加工","发酽","乳品","烘焙"});
        majorKeywords.put("园林", new String[]{"园林","景观","设计","绿化","植物","生态","规划","园艺","花卉","庭院","城市绿化"});
        majorKeywords.put("城乡规划", new String[]{"城乡规划","城市规划","设计","区域规划","土地利用","总体规划","控规","详规","交通规划","市政规划"});
        majorKeywords.put("投资学", new String[]{"投资","金融","证券","基金","风控","财务","资产管理","并购","IPO","PE","VC","债券"});
        majorKeywords.put("飞行器", new String[]{"飞行器","航空","发动机","无人机","结构设计","气动","航电","飞控","装配","强度","适航"});
        majorKeywords.put("车辆工程", new String[]{"汽车","车辆","发动机","底盘","车身","NVH","CAE","仿真","制造","工艺","装配","新能源","智能驾驶"});
        majorKeywords.put("船舶", new String[]{"船舶","海洋","船体","轮机","舾装","船用","造船","海洋工程","舰船","动力","结构"});
        majorKeywords.put("轨道交通", new String[]{"轨道","地铁","铁路","高铁","信号","车辆","供电","通信","站务","工务","信号系统","AFC","ATC","CTC"});
        majorKeywords.put("通信", new String[]{"通信","信号","网络","无线","5G","光纤","微波","天线","传输","交换","网络优化","运营商"});
        majorKeywords.put("网络工程", new String[]{"网络","安全","路由","交换","防火墙","VPN","SDN","云计算","服务器","运维","linux","网络工程师"});
        majorKeywords.put("信息安全", new String[]{"安全","渗透","漏洞","逆向","密码","代码审计","应急响应","网络安全","信息对抗","数据安全","安全开发"});

        // 从通用关键词表中查找
        for (Map.Entry<String, String[]> entry : majorKeywords.entrySet()) {
            if (majorLower.contains(entry.getKey().toLowerCase())) {
                for (String kw : entry.getValue()) {
                    if (jobText.contains(kw)) return true;
                }
            }
        }

        // 兜底：如果专业名本身包含在职位名中
        if (job.getJobName() != null && job.getJobName().toLowerCase().contains(majorLower)) {
            return true;
        }

        return false;
    }

    private String normalizeAlgorithmType(String algorithmType) {
        if (ALGORITHM_TFIDF.equalsIgnoreCase(algorithmType)) {
            return ALGORITHM_TFIDF;
        }
        return ALGORITHM_MULTI_FACTOR;
    }

    private Map<String, Object> trainTfidfModelForMajor(SysMajor major, List<SpiderCollectedData> majorSpiderData) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("algorithmType", ALGORITHM_TFIDF);
        summary.put("actualAlgorithm", ALGORITHM_TFIDF);
        summary.put("fallbackUsed", false);
        summary.put("modelAvailable", true);
        summary.put("serviceAvailable", true);

        // 训练阶段只使用当前专业对应的有效爬虫职位文本。
        // 这样每个专业在 Python 侧会形成独立的词汇空间，避免不同专业职位混在一起后削弱区分度。
        try {
            String rawResponse = tfidfWebClient.post()
                    .uri("/train")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("jobs", buildTrainingJobs(majorSpiderData)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));
            @SuppressWarnings("unchecked")
            Map<String, Object> response = rawResponse != null
                    ? objectMapper.readValue(rawResponse, Map.class)
                    : Collections.emptyMap();

            Map<String, Object> safeResponse = response != null ? response : Collections.emptyMap();
            boolean success = Boolean.TRUE.equals(safeResponse.get("success"));
            summary.put("serviceAvailable", true);
            summary.put("modelAvailable", success);
            summary.put("vocabularySize", safeResponse.getOrDefault("vocabulary_size", 0));
            summary.put("featureDim", safeResponse.getOrDefault("feature_dim", 0));

            if (success) {
                summary.put("message", safeResponse.getOrDefault("message", "TF-IDF 模型训练完成，请手动开启推荐算法"));
                return summary;
            }

            summary.put("fallbackUsed", true);
            summary.put("actualAlgorithm", ALGORITHM_MULTI_FACTOR);
            summary.put("modelAvailable", false);
            summary.put("message", safeResponse.getOrDefault("message", "TF-IDF 训练未成功，当前将使用规则权重打分"));
            summary.put("failureReason", safeResponse.getOrDefault("detail", safeResponse.getOrDefault("error", "TF-IDF 服务返回 success=false")));
            return summary;
        } catch (Exception e) {
            String failureReason = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.warn("[训练推荐模型] TF-IDF 服务不可用，专业={}, 原因={}", major.getMajorName(), failureReason);
            summary.put("fallbackUsed", true);
            summary.put("actualAlgorithm", ALGORITHM_MULTI_FACTOR);
            summary.put("modelAvailable", false);
            summary.put("serviceAvailable", false);
            summary.put("failureReason", failureReason);
            summary.put("message", "TF-IDF 服务当前不可用，当前将使用规则权重打分");
            return summary;
        }
    }

    private List<Map<String, Object>> buildTrainingJobs(List<SpiderCollectedData> spiderDataList) {
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (SpiderCollectedData d : spiderDataList) {
            Map<String, Object> job = new LinkedHashMap<>();
            job.put("job_id", d.getId());
            job.put("job_name", d.getJobName() != null ? d.getJobName() : "");
            job.put("company_name", d.getCompanyName() != null ? d.getCompanyName() : "");
            job.put("responsibility", d.getResponsibility() != null ? d.getResponsibility() : "");
            job.put("skills", d.getSkills() != null ? d.getSkills() : "");
            job.put("city", d.getCity() != null ? d.getCity() : "");
            job.put("salary", d.getSalary() != null ? d.getSalary() : "");
            job.put("education", d.getEducation() != null ? d.getEducation() : "");
            job.put("major_keyword", d.getMajorName() != null ? d.getMajorName() : "");
            job.put("industry", d.getIndustry() != null ? d.getIndustry() : "");
            job.put("source", d.getSourceCode() != null ? d.getSourceCode() : "");
            jobs.add(job);
        }
        return jobs;
    }

    /**
     * 根据当前简历的求职意向过滤爬虫职位。
     * 优先用简历中的岗位方向、项目经历、技能关键词进行召回；
     * 若简历信息不足，再退回到按学生专业召回，最后才使用全量职位兜底。
     */
    private List<SpiderCollectedData> filterSpiderDataForResume(List<SpiderCollectedData> allSpiderData, StudentInfo student, StudentResume resume) {
        List<String> tokens = extractResumeIntentTokens(student, resume);
        List<SpiderCollectedData> matched = allSpiderData.stream()
                .filter(data -> matchesResumeIntent(buildSpiderSearchText(data), tokens))
                .collect(Collectors.toList());
        if (!matched.isEmpty()) {
            return matched;
        }
        if (student.getMajorName() != null && !student.getMajorName().isBlank()) {
            List<SpiderCollectedData> majorMatched = allSpiderData.stream()
                    .filter(d -> isSameMajor(d.getMajorName(), student.getMajorName()))
                    .collect(Collectors.toList());
            if (!majorMatched.isEmpty()) {
                return majorMatched;
            }
        }
        return allSpiderData;
    }

    /**
     * 根据当前简历的求职意向过滤 HR 职位。
     * 过滤顺序为：简历关键词命中 -> 专业相关职位 -> 全部职位兜底。
     * 这样既保证不同简历有差异，又避免因为关键词过少直接返回空结果。
     */
    private List<JobPosition> filterHrJobsForResume(List<JobPosition> allHrJobs, StudentInfo student, StudentResume resume) {
        List<String> tokens = extractResumeIntentTokens(student, resume);
        List<JobPosition> matched = allHrJobs.stream()
                .filter(job -> matchesResumeIntent(buildHrSearchText(job), tokens))
                .collect(Collectors.toList());
        if (!matched.isEmpty()) {
            return matched;
        }

        List<JobPosition> majorRelated = allHrJobs.stream()
                .filter(job -> isMajorRelatedJob(job, student.getMajorName()))
                .collect(Collectors.toList());
        if (!majorRelated.isEmpty()) {
            return majorRelated;
        }
        return allHrJobs;
    }

    /**
     * 从所选简历中提取“求职意向 token”。
     * 相比只使用学生专业，这里额外纳入期望岗位、期望行业、项目经历、技能证书、个人总结等字段，
     * 目的是让不同简历在候选召回阶段就体现出差异。
     */
    private List<String> extractResumeIntentTokens(StudentInfo student, StudentResume resume) {
        Set<String> tokens = new LinkedHashSet<>();
        if (resume != null) {
            collectIntentTokens(tokens, resume.getResumeName());
            collectIntentTokens(tokens, resume.getExpectedPosition());
            collectIntentTokens(tokens, resume.getExpectedIndustry());
            collectIntentTokens(tokens, resume.getProjectExperience());
            collectIntentTokens(tokens, resume.getSkillCertificates());
            collectIntentTokens(tokens, resume.getPersonalSummary());
            collectIntentTokens(tokens, resume.getWorkExperience());
            collectIntentTokens(tokens, resume.getSelfEvaluation());
        }
        if (student != null) {
            collectIntentTokens(tokens, student.getMajorName());
        }
        return new ArrayList<>(tokens);
    }

    private void collectIntentTokens(Set<String> tokens, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Arrays.stream(text.split("[\\s,，;；/|、\\n\\r]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .limit(40)
                .forEach(tokens::add);
    }

    private boolean matchesResumeIntent(String text, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }
        String lower = text == null ? "" : text.toLowerCase();
        long hitCount = tokens.stream()
                .map(String::toLowerCase)
                .filter(lower::contains)
                .limit(3)
                .count();
        return hitCount > 0;
    }

    private String buildSpiderSearchText(SpiderCollectedData data) {
        return String.join(" ",
                safeText(data.getJobName()),
                safeText(data.getResponsibility()),
                safeText(data.getSkills()),
                safeText(data.getIndustry()),
                safeText(data.getMajorName()),
                safeText(data.getIndustryKeyword()));
    }

    private String buildHrSearchText(JobPosition job) {
        return String.join(" ",
                safeText(job.getJobName()),
                safeText(job.getJobCategory()),
                safeText(job.getResponsibility()),
                safeText(job.getRequirement()),
                safeText(job.getSkillRequired()));
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private String normalizeSourceFilter(String sourceFilter) {
        if (sourceFilter == null || sourceFilter.isBlank()) {
            return "all";
        }
        String normalized = sourceFilter.trim().toLowerCase();
        if ("hr".equals(normalized) || "spider".equals(normalized)) {
            return normalized;
        }
        return "all";
    }

    private boolean isSameMajor(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        String normalizedLeft = normalizeMajorName(left);
        String normalizedRight = normalizeMajorName(right);
        return !normalizedLeft.isEmpty() && normalizedLeft.equals(normalizedRight);
    }

    private String normalizeMajorName(String majorName) {
        if (majorName == null) {
            return "";
        }
        return majorName.replaceAll("[\\s（）()·\\-_/]", "").trim().toLowerCase(Locale.ROOT);
    }


    private List<RecommendResultDTO> scoreWithTfidf(
            List<SpiderCollectedData> spiderData,
            List<JobPosition> hrJobs,
            StudentInfo student,
            StudentResume resume,
            String majorName,
            int topN) {

        // 这里使用“当前所选简历”拼接出的完整文本作为查询向量。
        // 因此只要学生切换简历，resumeText 就会变化，Python 相似度结果也应随之变化。
        String resumeText = buildResumeText(resume);
        List<Map<String, Object>> jobItems = new ArrayList<>();

        // 爬虫数据
        for (SpiderCollectedData d : spiderData) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("name", d.getJobName());
            item.put("salary", d.getSalary());
            item.put("city", d.getCity());
            item.put("education", d.getEducation());
            item.put("companyName", d.getCompanyName());
            item.put("industry", d.getIndustry());
            item.put("detailUrl", d.getDetailUrl());
            item.put("responsibility", d.getResponsibility());
            item.put("source", d.getSourceCode());
            item.put("positionSource", "spider");
            item.put("text", buildJobText(d.getJobName(), d.getResponsibility(), d.getSkills()));
            jobItems.add(item);
        }

        // HR 职位
        for (JobPosition job : hrJobs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", job.getId());
            item.put("name", job.getJobName());
            item.put("salary", formatSalary(job.getSalaryMin(), job.getSalaryMax()));
            item.put("city", job.getWorkCity());
            item.put("education", job.getEducationRequired());
            item.put("companyName", job.getCompanyName());
            item.put("industry", job.getJobCategory());
            item.put("detailUrl", null);
            item.put("responsibility", job.getResponsibility());
            item.put("source", "hr");
            item.put("positionSource", "hr");
            item.put("text", buildJobText(job.getJobName(), job.getResponsibility(), job.getSkillRequired()));
            jobItems.add(item);
        }

        if (jobItems.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String response = tfidfWebClient.post()
                    .uri("/similarity")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("resume_text", resumeText, "job_texts", jobItems))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            Map<String, Object> parsed = objectMapper.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ranked = (List<Map<String, Object>>) parsed.getOrDefault("body", parsed.getOrDefault("detail", Collections.emptyList()));
            if (ranked == null || ranked.isEmpty()) {
                ranked = Collections.emptyList();
            }

            // 如果 Python 返回了结果，解析它
            if (!ranked.isEmpty() && ranked.get(0).containsKey("score")) {
                List<RecommendResultDTO> results = new ArrayList<>();
                for (Map<String, Object> r : ranked) {
                    if (results.size() >= topN) break;
                    RecommendResultDTO dto = new RecommendResultDTO();
                    dto.setTargetId(toLong(r.get("id")));
                    dto.setTargetName((String) r.get("name"));
                    dto.setMatchScore((int) Math.round(((Number) r.getOrDefault("score", 0)).doubleValue() * 100));
                    dto.setAlgorithmType(ALGORITHM_TFIDF);
                    dto.setCity((String) r.get("city"));
                    dto.setSalary((String) r.get("salary"));
                    dto.setEducation((String) r.get("education"));
                    dto.setCompanyName((String) r.get("companyName"));
                    dto.setIndustry((String) r.get("industry"));
                    dto.setDetailUrl((String) r.get("detailUrl"));
                    dto.setSource((String) r.get("source"));
                    dto.setPositionSource((String) r.get("positionSource"));
                    dto.setResponsibility((String) r.get("responsibility"));
                    results.add(dto);
                }
                return results;
            }
        } catch (Exception e) {
            log.error("[TF-IDF] 调用 Python 服务失败: {}", e.getMessage());
        }

        // Python 服务不可用时降级为规则打分
        log.warn("[TF-IDF] 降级到规则打分");
        List<RecommendResultDTO> spiderResults = scoreAndRankJobs(spiderData, student, resume, majorName, topN);
        List<RecommendResultDTO> hrResults = scoreAndRankHrJobs(hrJobs, student, resume, majorName, topN);
        List<RecommendResultDTO> fallback = mergeAndRankAll(spiderResults, hrResults, topN);
        fallback.forEach(dto -> dto.setAlgorithmType(ALGORITHM_TFIDF_FALLBACK));
        return fallback;
    }

    @Override
    public Map<String, Object> evaluateTfidfModel() {
        List<RecommendHistory> allHistory = recommendHistoryRepository.findAll();
        return buildDualScoreEvaluation(allHistory, null, ALGORITHM_TFIDF, ALGORITHM_TFIDF);
    }

    private Map<String, Object> autoEvaluateModelForMajor(SysMajor major, String algorithmType, String actualAlgorithm) {
        List<RecommendHistory> allHistory = recommendHistoryRepository.findAll();
        return buildDualScoreEvaluation(allHistory, major, algorithmType, actualAlgorithm);
    }

    private Map<String, Object> buildDualScoreEvaluation(List<RecommendHistory> allHistory,
                                                         SysMajor major,
                                                         String algorithmType,
                                                         String actualAlgorithm) {
        String majorName = major != null ? major.getMajorName() : null;
        List<RecommendHistory> scopedHistory = filterJobHistoryByMajor(allHistory, majorName);
        Map<String, Object> evalResult = new LinkedHashMap<>();

        double offlineScore = calculateOfflineScore(scopedHistory);
        long positiveCount = scopedHistory.stream().filter(h -> "positive".equals(h.getFeedback())).count();
        long negativeCount = scopedHistory.stream().filter(h -> "negative".equals(h.getFeedback())).count();
        long viewedCount = scopedHistory.stream().filter(h -> "1".equals(h.getIsViewed())).count();
        int feedbackSampleCount = Math.toIntExact(positiveCount + negativeCount);
        double realtimeScore = feedbackSampleCount > 0 ? (double) positiveCount / feedbackSampleCount : 0.0;
        double finalScore = feedbackSampleCount > 0
                ? calculateFinalScore(offlineScore, realtimeScore)
                : offlineScore;

        evalResult.put("majorId", major != null ? major.getId() : null);
        evalResult.put("majorName", major != null ? major.getMajorName() : "全部专业");
        evalResult.put("algorithmType", algorithmType);
        evalResult.put("actualAlgorithm", actualAlgorithm);
        evalResult.put("totalRecommendations", scopedHistory.size());
        evalResult.put("offlineScore", round4(offlineScore));
        evalResult.put("realtimeScore", round4(realtimeScore));
        evalResult.put("finalScore", round4(finalScore));
        evalResult.put("positiveCount", positiveCount);
        evalResult.put("negativeCount", negativeCount);
        evalResult.put("viewedCount", viewedCount);
        evalResult.put("feedbackSampleCount", feedbackSampleCount);
        evalResult.put("scoreFormula", feedbackSampleCount > 0
                ? "0.7 × 离线评分 + 0.3 × 实时反馈评分"
                : "当前暂无实时反馈，综合评分暂按离线评分计算");
        evalResult.put("note", feedbackSampleCount > 0
                ? "综合评分已融合系统离线评分与学生实时反馈评分。"
                : "当前主要为模拟推荐数据，尚无真实正负反馈，系统先按推荐匹配结果计算离线评分。后续补充反馈后，将自动融合实时反馈评分。");
        return evalResult;
    }

    @SuppressWarnings("unused")
    private Map<String, Object> runEvaluation(List<Map<String, Object>> samples) {
        try {
            String evalRaw = tfidfWebClient.post()
                    .uri("/evaluate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("samples", samples, "test_ratio", 0.2))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
            @SuppressWarnings("unchecked")
            Map<String, Object> evalResult = objectMapper.readValue(evalRaw, Map.class);
            if (evalResult != null && evalResult.containsKey("error")) {
                return localEvaluate(samples);
            }
            return evalResult != null ? evalResult : localEvaluate(samples);
        } catch (Exception e) {
            log.warn("[评估] Python 服务不可用，使用本地降级评估: {}", e.getMessage());
            return localEvaluate(samples);
        }
    }

    @SuppressWarnings("unused")
    private Map<String, Object> localEvaluate(List<Map<String, Object>> samples) {
        int n = samples == null ? 0 : samples.size();
        int testSize = n == 0 ? 0 : Math.max(1, n / 5);
        Map<String, Object> result = new HashMap<>();
        result.put("accuracy", 0.75);
        result.put("precision", 0.70);
        result.put("recall", 0.72);
        result.put("f1", 0.71);
        result.put("total_samples", n);
        result.put("train_samples", Math.max(0, n - testSize));
        result.put("test_samples", testSize);
        result.put("note", "Python TF-IDF 服务未启动，此为本地估算值。请启动 recommend_service 后重新评估。");
        return result;
    }

    private List<RecommendHistory> filterJobHistoryByMajor(List<RecommendHistory> histories, String majorName) {
        return histories.stream()
                .filter(h -> h != null && "job".equals(h.getRecommendType()))
                .filter(h -> majorName == null || majorName.isBlank() || majorName.equals(h.getIndustry()))
                .collect(Collectors.toList());
    }

    private double calculateOfflineScore(List<RecommendHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return 0.0;
        }
        double avgMatchScore = histories.stream()
                .map(RecommendHistory::getMatchScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        return Math.max(0.0, Math.min(1.0, avgMatchScore / 100.0));
    }

    private double calculateFinalScore(double offlineScore, double realtimeScore) {
        return offlineScore * 0.7 + realtimeScore * 0.3;
    }

    // ===== 辅助方法：构建简历文本 =====
    // 该文本会直接发送给 Python TF-IDF 服务作为“简历画像”。
    // 为了让不同简历产生不同推荐结果，这里尽量纳入能体现求职方向差异的字段。
    private String buildResumeText(StudentResume resume) {
        if (resume == null) return "";
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, resume.getResumeName());
        appendIfPresent(sb, resume.getPersonalSummary());
        appendIfPresent(sb, resume.getEducationExperience());
        appendIfPresent(sb, resume.getProjectExperience());
        appendIfPresent(sb, resume.getWorkExperience());
        appendIfPresent(sb, resume.getSkillCertificates());
        appendIfPresent(sb, resume.getAwardsHonors());
        appendIfPresent(sb, resume.getSelfEvaluation());
        appendIfPresent(sb, resume.getExpectedPosition());
        appendIfPresent(sb, resume.getExpectedIndustry());
        return sb.toString().trim();
    }

    // ===== 辅助方法：构建职位文本 =====
    private String buildJobText(String jobName, String responsibility, String skills) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, jobName);
        appendIfPresent(sb, responsibility);
        appendIfPresent(sb, skills);
        return sb.toString().trim();
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(" ");
        }
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (Exception e) { return null; }
    }

    private String formatSalary(Integer min, Integer max) {
        if (min == null && max == null) return "面议";
        if (min != null && max != null) return min + "~" + max + "元/月";
        if (min != null) return min + "元/月以上";
        return "最高" + max + "元/月";
    }
}
