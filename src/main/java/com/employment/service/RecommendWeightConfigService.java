package com.employment.service;

import com.employment.model.entity.RecommendWeightConfig;

import java.util.List;
import java.util.Map;

public interface RecommendWeightConfigService {

    List<RecommendWeightConfig> getAllWeights();

    Map<String, Integer> getWeightMap();

    List<RecommendWeightConfig> updateWeights(List<RecommendWeightConfig> configs);

    void resetToDefault();

    int getTotalWeight();
}
