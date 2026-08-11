package com.employment.service.impl;

import com.employment.init.DataConstants;
import com.employment.model.entity.SpiderMajorKeyword;
import com.employment.repository.SpiderMajorKeywordRepository;
import com.employment.service.SpiderMajorKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpiderMajorKeywordServiceImpl implements SpiderMajorKeywordService {

    private final SpiderMajorKeywordRepository repository;

    @PostConstruct
    public void syncFromDataConstants() {
        for (Map.Entry<String, List<String>> entry : DataConstants.MAJOR_INDUSTRY_KEYWORDS.entrySet()) {
            String major = entry.getKey();
            for (String keyword : entry.getValue()) {
                if (!repository.existsByMajorNameAndKeyword(major, keyword)) {
                    SpiderMajorKeyword entity = new SpiderMajorKeyword();
                    entity.setMajorName(major);
                    entity.setKeyword(keyword);
                    repository.save(entity);
                }
            }
        }
        log.info("专业关键词初始化同步完成，共 {} 条记录", repository.count());
    }

    @Override
    public List<Map<String, Object>> getMajorKeywordsGrouped() {
        List<SpiderMajorKeyword> all = repository.findAll();
        return all.stream()
                .collect(Collectors.groupingBy(SpiderMajorKeyword::getMajorName, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("major", e.getKey());
                    map.put("count", e.getValue().size());
                    map.put("keywords", e.getValue().stream()
                            .sorted(Comparator.comparingLong(SpiderMajorKeyword::getId))
                            .map(SpiderMajorKeyword::getKeyword)
                            .collect(Collectors.toList()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllMajorNames() {
        return repository.findAll().stream()
                .map(SpiderMajorKeyword::getMajorName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SpiderMajorKeyword addKeyword(String majorName, String keyword) {
        if (repository.existsByMajorNameAndKeyword(majorName, keyword)) {
            throw new IllegalArgumentException("关键词已存在: " + keyword);
        }
        SpiderMajorKeyword entity = new SpiderMajorKeyword();
        entity.setMajorName(majorName);
        entity.setKeyword(keyword);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void deleteKeyword(String majorName, String keyword) {
        repository.deleteByMajorNameAndKeyword(majorName, keyword);
    }

    @Override
    @Transactional
    public List<SpiderMajorKeyword> addKeywords(String majorName, List<String> keywords) {
        List<SpiderMajorKeyword> saved = new ArrayList<>();
        for (String keyword : keywords) {
            String kw = keyword.trim();
            if (kw.isEmpty()) continue;
            if (!repository.existsByMajorNameAndKeyword(majorName, kw)) {
                SpiderMajorKeyword entity = new SpiderMajorKeyword();
                entity.setMajorName(majorName);
                entity.setKeyword(kw);
                saved.add(repository.save(entity));
            }
        }
        return saved;
    }

    @Override
    public long getKeywordCount() {
        return repository.count();
    }
}
