package com.employment.service;

import java.util.Map;

public interface CrawlerEngineService {

    Map<String, Object> startCrawl(Long taskId);

    void executeCrawl(Long taskId);

    Map<String, Object> stopCrawl(Long taskId);

    Map<String, Object> getCrawlProgress(Long taskId);

    boolean isTaskRunning(Long taskId);

    Map<String, Object> executeEtl(Long taskId);

    Map<String, Object> getTrainingPoolStats();

    Map<String, Object> getNationalEmploymentStats();

    /** ETL 状态查询：待清洗/有效/无效/已同步四条计数 */
    Map<String, Object> getEtlStatus();

    /** 一键执行 ETL + 推荐全流程（爬虫完成后调用） */
    Map<String, Object> executeFullPipeline(Long taskId);
}
