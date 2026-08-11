package com.employment.service.impl;

import com.employment.model.entity.RecommendWeightConfig;
import com.employment.repository.RecommendWeightConfigRepository;
import com.employment.service.RecommendWeightConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendWeightConfigServiceImpl implements RecommendWeightConfigService {

    private final RecommendWeightConfigRepository repository;

    private static final Map<String, Integer> DEFAULT_WEIGHTS = new LinkedHashMap<>();
    static {
        // 默认权重做了“弱专业、强技能”的调整：
        // 在学生简历内容较丰富时，系统会更重视技能、项目与求职意向，
        // 减少仅凭专业名把职位过度推向“大数据/计算机”方向的情况。
        DEFAULT_WEIGHTS.put("major", 15);
        DEFAULT_WEIGHTS.put("city", 20);
        DEFAULT_WEIGHTS.put("salary", 20);
        DEFAULT_WEIGHTS.put("skill", 30);
        DEFAULT_WEIGHTS.put("education", 15);
    }

    private static final Map<String, String> DEFAULT_NAMES = new LinkedHashMap<>();
    static {
        DEFAULT_NAMES.put("major", "专业相关性");
        DEFAULT_NAMES.put("city", "城市偏好");
        DEFAULT_NAMES.put("salary", "薪资匹配度");
        DEFAULT_NAMES.put("skill", "技能关键词");
        DEFAULT_NAMES.put("education", "学历匹配");
    }

    private static final Map<String, String> DEFAULT_DESCRIPTIONS = new LinkedHashMap<>();
    static {
        DEFAULT_DESCRIPTIONS.put("major", "职位专业要求/行业关键词与学生专业方向的相关程度，作为基础约束而非唯一依据");
        DEFAULT_DESCRIPTIONS.put("city", "职位城市与学生期望城市或历史投递城市一致");
        DEFAULT_DESCRIPTIONS.put("salary", "职位薪资落在学生简历填写的期望薪资区间内");
        DEFAULT_DESCRIPTIONS.put("skill", "职位描述/技能要求中匹配学生简历的技能/项目/证书关键词数量，是区分不同简历的核心维度");
        DEFAULT_DESCRIPTIONS.put("education", "职位学历要求低于或等于学生最高学历");
    }

    @PostConstruct
    public void initDefaultData() {
        for (Map.Entry<String, Integer> entry : DEFAULT_WEIGHTS.entrySet()) {
            String key = entry.getKey();
            if (repository.findByWeightKey(key).isEmpty()) {
                RecommendWeightConfig config = new RecommendWeightConfig();
                config.setWeightKey(key);
                config.setWeightName(DEFAULT_NAMES.get(key));
                config.setWeightValue(entry.getValue());
                config.setDescription(DEFAULT_DESCRIPTIONS.get(key));
                config.setEnabled("1");
                repository.save(config);
                log.info("初始化权重配置: {} = {}分", key, entry.getValue());
            }
        }
    }

    @Override
    public List<RecommendWeightConfig> getAllWeights() {
        return repository.findAllByOrderByIdAsc();
    }

    @Override
    public Map<String, Integer> getWeightMap() {
        Map<String, Integer> map = new HashMap<>();
        for (RecommendWeightConfig config : repository.findAllByOrderByIdAsc()) {
            map.put(config.getWeightKey(), config.getWeightValue());
        }
        return map;
    }

    @Override
    @Transactional
    public List<RecommendWeightConfig> updateWeights(List<RecommendWeightConfig> configs) {
        List<RecommendWeightConfig> saved = new ArrayList<>();
        for (RecommendWeightConfig config : configs) {
            repository.findByWeightKey(config.getWeightKey()).ifPresent(existing -> {
                existing.setWeightValue(config.getWeightValue());
                existing.setEnabled(config.getEnabled() != null ? config.getEnabled() : "1");
                repository.save(existing);
                saved.add(existing);
            });
        }
        return saved;
    }

    @Override
    @Transactional
    public void resetToDefault() {
        for (Map.Entry<String, Integer> entry : DEFAULT_WEIGHTS.entrySet()) {
            String key = entry.getKey();
            repository.findByWeightKey(key).ifPresent(existing -> {
                existing.setWeightValue(entry.getValue());
                existing.setEnabled("1");
                repository.save(existing);
            });
        }
        log.info("推荐权重已重置为默认值");
    }

    @Override
    public int getTotalWeight() {
        return repository.findAllByOrderByIdAsc().stream()
                .mapToInt(RecommendWeightConfig::getWeightValue)
                .sum();
    }
}
