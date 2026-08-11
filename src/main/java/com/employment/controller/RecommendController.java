package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.exception.BusinessException;
import com.employment.model.entity.RecommendHistory;
import com.employment.model.entity.StudentInfo;
import com.employment.model.entity.SysMajor;
import com.employment.repository.RecommendHistoryRepository;
import com.employment.repository.StudentInfoRepository;
import com.employment.repository.SysMajorRepository;
import com.employment.security.SecurityUtils;
import com.employment.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;
    private final StudentInfoRepository studentInfoRepository;
    private final RecommendHistoryRepository recommendHistoryRepository;
    private final SysMajorRepository sysMajorRepository;
    private final SecurityUtils securityUtils;

    // ==================== 学生端接口 ====================

    /**
     * 获取当前学生的推荐状态（算法是否开启、是否有推荐结果）
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getRecommendStatus() {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        Map<String, Object> status = recommendService.getRecommendStatusForStudent(student.getId());
        return Result.success(status);
    }

    /**
     * 获取当前学生的推荐职位列表
     */
    @GetMapping("/jobs")
    public Result<List<Map<String, Object>>> recommendJobs(
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false, defaultValue = "all") String sourceFilter) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));

        List<Map<String, Object>> results;
        if (resumeId != null) {
            results = recommendService.getStudentRecommendations(student.getId(), resumeId, topN, sourceFilter);
        } else {
            results = recommendService.getStudentRecommendations(student.getId(), topN);
        }
        return Result.success(results);
    }

    /**
     * 学生端：提交推荐反馈
     */
    @PostMapping("/feedback")
    @Transactional
    @OperationLog(module = "智能推荐", content = "提交推荐反馈")
    public Result<Void> submitFeedback(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        Long historyId = data.get("historyId") != null ? ((Number) data.get("historyId")).longValue() : null;
        String feedback = (String) data.get("feedback");
        String reason = (String) data.get("reason");

        if (historyId != null) {
            recommendHistoryRepository.findById(historyId).ifPresent(h -> {
                if (userId.equals(h.getUserId())) {
                    h.setFeedback(feedback);
                    h.setFeedbackReason(reason);
                    recommendHistoryRepository.save(h);
                }
            });
        }
        return Result.success("反馈已提交", null);
    }

    /**
     * 学生端：查看推荐历史记录
     */
    @GetMapping("/history")
    public Result<Page<RecommendHistory>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = securityUtils.getCurrentUserId();
        Page<RecommendHistory> history = recommendHistoryRepository
                .findByUserIdOrderByCreateTimeDesc(userId, PageRequest.of(page - 1, size));
        return Result.success(history);
    }

    // ==================== 数据分析员端接口 ====================

    /**
     * 数据分析员：获取所有专业的推荐状态概览
     */
    @GetMapping("/admin/overview")
    @OperationLog(module = "智能推荐", content = "查看推荐概览")
    public Result<Map<String, Object>> getMajorRecommendStatus() {
        Map<String, Object> result = recommendService.getMajorRecommendStatus();
        return Result.success(result);
    }

    /**
     * 数据分析员：获取某个专业的推荐详情
     */
    @GetMapping("/admin/major/{majorId}")
    public Result<Map<String, Object>> getMajorRecommendDetail(@PathVariable Long majorId) {
        Map<String, Object> result = recommendService.getMajorRecommendDetail(majorId);
        return Result.success(result);
    }

    /**
     * 数据分析员：触发某个专业的推荐（核心接口）
     * 选中专业 → 为该专业下所有学生生成推荐结果
     */
    @PostMapping("/admin/trigger")
    @Transactional
    @OperationLog(module = "智能推荐", content = "触发专业推荐算法")
    public Result<Map<String, Object>> triggerRecommend(@RequestBody Map<String, Object> data) {
        Long majorId = data.get("majorId") != null ? ((Number) data.get("majorId")).longValue() : null;
        Integer topN = data.get("topN") != null ? ((Number) data.get("topN")).intValue() : 20;
        String algorithmType = (String) data.getOrDefault("algorithmType", "multi_factor");

        if (majorId == null) {
            throw new BusinessException(400, "请选择专业");
        }

        long start = System.currentTimeMillis();
        List<Map<String, Object>> results = recommendService.triggerRecommendForMajor(majorId, topN, algorithmType);
        long cost = System.currentTimeMillis() - start;

        Map<String, Object> response = new HashMap<>();
        response.put("total", results.size());
        response.put("costTime", cost + "ms");
        response.put("algorithmType", algorithmType);
        response.put("results", results);
        log.info("推荐算法({})执行完成，专业ID={}，生成{}条推荐，耗时{}ms", algorithmType, majorId, results.size(), cost);
        return Result.success(response);
    }

    /**
     * 数据分析员：为某个专业训练推荐模型
     * 训练完成后，该专业才能开启推荐算法
     */
    @PostMapping("/admin/train/{majorId}")
    @Transactional
    @OperationLog(module = "智能推荐", content = "训练推荐模型")
    public Result<Map<String, Object>> trainRecommendModel(
            @PathVariable Long majorId,
            @RequestBody(required = false) Map<String, Object> data) {
        if (majorId == null) {
            throw new BusinessException(400, "请选择专业");
        }
        String algorithmType = data != null && data.get("algorithmType") != null
                ? String.valueOf(data.get("algorithmType"))
                : "tfidf";
        SysMajor major = sysMajorRepository.findById(majorId)
                .orElseThrow(() -> new BusinessException(404, "专业不存在"));

        long start = System.currentTimeMillis();
        List<Map<String, Object>> results = recommendService.trainRecommendModel(majorId, algorithmType);
        long cost = System.currentTimeMillis() - start;

        Map<String, Object> summary = results.isEmpty() ? new HashMap<>() : new HashMap<>(results.get(0));
        summary.put("total", results.size());
        summary.put("costTime", cost + "ms");
        summary.put("trained", Boolean.TRUE.equals(summary.getOrDefault("trained", Boolean.TRUE)));
        summary.put("algorithmType", summary.getOrDefault("algorithmType", algorithmType));
        summary.put("majorId", major.getId());
        summary.put("majorName", major.getMajorName());
        log.info("[训练推荐] 专业ID={} 算法={} 训练完成，耗时{}ms", majorId, algorithmType, cost);
        return Result.success(summary);
    }

    /**
     * 数据分析员：一键批量训练所有有数据和学生的专业
     */
    @PostMapping("/admin/train-all")
    @OperationLog(module = "智能推荐", content = "一键批量训练所有专业")
    public Result<Map<String, Object>> trainAllMajors(@RequestBody(required = false) Map<String, Object> data) {
        long start = System.currentTimeMillis();
        String algorithmType = data != null && data.get("algorithmType") != null
                ? String.valueOf(data.get("algorithmType"))
                : "tfidf";
        Map<String, Object> result = recommendService.trainAllMajors(algorithmType);
        long cost = System.currentTimeMillis() - start;
        result.put("costTime", cost + "ms");
        result.put("algorithmType", result.getOrDefault("algorithmType", algorithmType));
        log.info("[批量训练] 全部专业训练完成，算法={}，耗时{}ms", algorithmType, cost);
        return Result.success(result);
    }

    /**
     * 数据分析员：一键开启全部已训练模型
     */
    @PostMapping("/admin/enable-all")
    @Transactional
    @OperationLog(module = "智能推荐", content = "一键开启全部已训练模型")
    public Result<Map<String, Object>> enableAllTrainedMajors() {
        long start = System.currentTimeMillis();
        Map<String, Object> result = recommendService.enableAllTrainedMajors();
        long cost = System.currentTimeMillis() - start;
        result.put("costTime", cost + "ms");
        log.info("[批量开启] 全部已训练专业开启完成，耗时{}ms", cost);
        return Result.success(result);
    }

    /**
     * 数据分析员：获取模型训练统计（概览）
     */
    @GetMapping("/admin/training-stats")
    public Result<Map<String, Object>> getModelTrainingStats() {
        Map<String, Object> result = recommendService.getModelTrainingStats();
        return Result.success(result);
    }

    /**
     * 数据分析员：设置某个专业的推荐开关
     */
    @PutMapping("/admin/major/{majorId}/toggle")
    @Transactional
    @OperationLog(module = "智能推荐", content = "切换专业推荐开关")
    public Result<Void> toggleRecommendEnabled(
            @PathVariable Long majorId,
            @RequestBody Map<String, String> body) {
        SysMajor major = sysMajorRepository.findById(majorId)
                .orElseThrow(() -> new BusinessException(404, "专业不存在"));
        major.setRecommendEnabled(body.get("recommendEnabled"));
        sysMajorRepository.save(major);
        log.info("专业[{}]推荐开关设置为: {}", major.getMajorName(), body.get("recommendEnabled"));
        return Result.success("设置成功", null);
    }

    /**
     * 数据分析员：查看推荐结果列表（分页）
     */
    @GetMapping("/admin/results")
    public Result<Page<RecommendHistory>> getRecommendResults(
            @RequestParam(required = false) String recommendType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (recommendType == null || recommendType.isEmpty()) {
            recommendType = "job";
        }
        Page<RecommendHistory> pageResult = recommendHistoryRepository
                .findByRecommendTypeOrderByCreateTimeDesc(recommendType, PageRequest.of(page - 1, size));
        return Result.success(pageResult);
    }

    /**
     * 数据分析员：获取各专业的推荐统计
     */
    @GetMapping("/admin/stats")
    public Result<Map<String, Object>> getRecommendStats() {
        List<RecommendHistory> all = recommendHistoryRepository.findAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecommendations", all.size());
        stats.put("viewedCount", all.stream().filter(h -> "1".equals(h.getIsViewed())).count());
        stats.put("positiveFeedback", all.stream().filter(h -> "positive".equals(h.getFeedback())).count());
        stats.put("negativeFeedback", all.stream().filter(h -> "negative".equals(h.getFeedback())).count());
        return Result.success(stats);
    }

    /**
     * 数据分析员：评估 TF-IDF 模型性能
     */
    @GetMapping("/admin/evaluate")
    @OperationLog(module = "智能推荐", content = "评估推荐模型")
    public Result<Map<String, Object>> evaluateModel() {
        Map<String, Object> result = recommendService.evaluateTfidfModel();
        return Result.success(result);
    }
}
