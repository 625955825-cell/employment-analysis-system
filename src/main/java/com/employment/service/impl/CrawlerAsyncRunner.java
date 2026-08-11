package com.employment.service.impl;

import com.employment.security.SpringContextHolder;
import com.employment.service.CrawlerEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CrawlerAsyncRunner {

    @Async("crawlerTaskExecutor")
    public void runAsync(Long taskId) {
        log.info("异步爬虫任务开始执行: taskId={}", taskId);
        try {
            CrawlerEngineService crawlerEngineService = SpringContextHolder.getBean(CrawlerEngineService.class);
            crawlerEngineService.executeCrawl(taskId);
        } catch (Exception e) {
            log.error("异步爬虫执行失败: taskId={}", taskId, e);
        }
    }
}
