package com.employment.service;

import com.employment.model.entity.SpiderMajorKeyword;

import java.util.List;
import java.util.Map;

public interface SpiderMajorKeywordService {

    List<Map<String, Object>> getMajorKeywordsGrouped();

    List<String> getAllMajorNames();

    SpiderMajorKeyword addKeyword(String majorName, String keyword);

    void deleteKeyword(String majorName, String keyword);

    List<SpiderMajorKeyword> addKeywords(String majorName, List<String> keywords);

    long getKeywordCount();
}
