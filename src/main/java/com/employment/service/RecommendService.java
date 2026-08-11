package com.employment.service;

import com.employment.model.dto.RecommendResultDTO;
import java.util.List;
import java.util.Map;

public interface RecommendService {

    Map<String, Object> getMajorRecommendStatus();

    Map<String, Object> getMajorRecommendDetail(Long majorId);

    List<Map<String, Object>> triggerRecommendForMajor(Long majorId, int topN);

    List<Map<String, Object>> triggerRecommendForMajor(Long majorId, int topN, String algorithmType);

    /**
     * 为专业训练推荐模型（数据分析师操作）
     * 训练后将 modelTrained 标记为 "trained"
     */
    List<Map<String, Object>> trainRecommendModel(Long majorId, String algorithmType);

    /**
     * 批量训练所有有爬虫数据和学生的专业
     */
    Map<String, Object> trainAllMajors(String algorithmType);

    /**
     * 一键开启全部已训练模型
     */
    Map<String, Object> enableAllTrainedMajors();

    /**
     * 获取模型训练统计（训练进度概览）
     */
    Map<String, Object> getModelTrainingStats();

    Map<String, Object> evaluateTfidfModel();

    List<Map<String, Object>> getStudentRecommendations(Long studentId, int topN);

    List<Map<String, Object>> getStudentRecommendations(Long studentId, Long resumeId, int topN);

    List<Map<String, Object>> getStudentRecommendations(Long studentId, Long resumeId, int topN, String sourceFilter);

    Map<String, Object> getRecommendStatusForStudent(Long studentId);

    void saveRecommendHistory(Long userId, String userType, List<RecommendResultDTO> results, String algorithmType);
}
