package com.employment.controller;

import com.employment.common.Result;
import com.employment.model.entity.RecommendWeightConfig;
import com.employment.service.RecommendWeightConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data/weight")
@RequiredArgsConstructor
public class RecommendWeightConfigController {

    private final RecommendWeightConfigService weightConfigService;

    @GetMapping("/list")
    public Result<List<RecommendWeightConfig>> list() {
        return Result.success(weightConfigService.getAllWeights());
    }

    @GetMapping("/map")
    public Result<Map<String, Integer>> map() {
        return Result.success(weightConfigService.getWeightMap());
    }

    @GetMapping("/total")
    public Result<Map<String, Object>> total() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", weightConfigService.getTotalWeight());
        data.put("isValid", weightConfigService.getTotalWeight() == 100);
        return Result.success(data);
    }

    @PostMapping("/update")
    public Result<List<RecommendWeightConfig>> update(@RequestBody List<RecommendWeightConfig> configs) {
        int total = configs.stream().mapToInt(RecommendWeightConfig::getWeightValue).sum();
        if (total != 100) {
            return Result.error("权重总分必须等于100分，当前为 " + total + " 分");
        }
        List<RecommendWeightConfig> saved = weightConfigService.updateWeights(configs);
        return Result.success("权重更新成功", saved);
    }

    @PostMapping("/reset")
    public Result<Void> reset() {
        weightConfigService.resetToDefault();
        return Result.success("已恢复默认权重", null);
    }
}
