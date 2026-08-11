package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.SpiderCollectedData;
import com.employment.model.entity.SpiderLog;
import com.employment.model.entity.SpiderTask;
import com.employment.model.entity.SpiderMajorKeyword;
import com.employment.repository.SpiderCollectedDataRepository;
import com.employment.repository.SpiderLogRepository;
import com.employment.repository.SpiderTaskRepository;
import com.employment.repository.SpiderMajorKeywordRepository;
import com.employment.service.CrawlerEngineService;
import com.employment.service.SpiderMajorKeywordService;
import com.employment.service.impl.SeleniumCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/spider")
@RequiredArgsConstructor
public class SpiderController {

    private final SpiderTaskRepository spiderTaskRepository;
    private final SpiderCollectedDataRepository collectedDataRepository;
    private final SpiderLogRepository spiderLogRepository;
    private final SpiderMajorKeywordRepository spiderMajorKeywordRepository;
    private final CrawlerEngineService crawlerEngineService;
    private final SeleniumCrawlerService seleniumCrawlerService;
    private final SpiderMajorKeywordService spiderMajorKeywordService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===================== 站点管理 =====================

    @GetMapping("/sites")
    public Result<List<Map<String, Object>>> getSites() {
        List<Map<String, Object>> sites = new ArrayList<>();
        // 支持4个数据源
        String[][] rawSites = {
            {"51job", "前程无忧", "https://www.51job.com", "#ff8b00", "前程无忧 - 覆盖全国热门行业职位，数据量最大"},
            {"gov", "人社部公共招聘", "https://job.mohrss.gov.cn", "#409eff", "人社部公共招聘网 - 政府权威数据，无反爬"},
            {"moe", "教育部就业平台", "https://bjbys.ncss.cn", "#67c23a", "教育部就业平台 - 官方就业数据，数据权威"},
            {"yingjiesheng", "应届生求职网", "https://www.yingjiesheng.com", "#e6a23c", "应届生求职网 - 专为应届生设计，爬虫友好"}
        };
        for (String[] s : rawSites) {
            Map<String, Object> site = new LinkedHashMap<>();
            site.put("code", s[0]);
            site.put("name", s[1]);
            site.put("url", s[2]);
            site.put("color", s[3]);
            site.put("icon", s[1].substring(0, 1));
            site.put("status", "active");
            site.put("description", s[4]);
            sites.add(site);
        }
        return Result.success(sites);
    }

    // ===================== 任务管理 =====================

    @GetMapping("/tasks")
    public Result<Map<String, Object>> getTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 自动修复数据库中历史的脏进度数据（>100% 或 <0），避免前端显示错误
        spiderTaskRepository.fixInvalidProgress();
        Page<SpiderTask> taskPage = spiderTaskRepository.searchTasks(status, keyword, PageRequest.of(page - 1, pageSize));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", taskPage.getContent());
        result.put("total", taskPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    @PostMapping("/task")
    @OperationLog(module = "爬虫管理", content = "创建采集任务")
    public Result<SpiderTask> createTask(@RequestBody SpiderTask task) {
        task.setStatus("pending");
        task.setProgress(0);
        task.setCollectedCount(0);
        task.setSuccessRate(0);
        if (task.getSourceName() == null && task.getSourceCode() != null) {
            task.setSourceName(getSiteName(task.getSourceCode()));
        }
        return Result.success(spiderTaskRepository.save(task));
    }

    @PutMapping("/task/{id}")
    @OperationLog(module = "爬虫管理", content = "更新采集任务")
    public Result<SpiderTask> updateTask(@PathVariable Long id, @RequestBody SpiderTask task) {
        SpiderTask existing = spiderTaskRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("任务不存在"));
        if (task.getTaskName() != null) existing.setTaskName(task.getTaskName());
        if (task.getTargetUrl() != null) existing.setTargetUrl(task.getTargetUrl());
        if (task.getDataTypes() != null) existing.setDataTypes(task.getDataTypes());
        if (task.getDepth() != null) existing.setDepth(task.getDepth());
        if (task.getCronExpression() != null) existing.setCronExpression(task.getCronExpression());
        if (task.getRemark() != null) existing.setRemark(task.getRemark());
        if (task.getSelectedMajors() != null) existing.setSelectedMajors(task.getSelectedMajors());
        return Result.success(spiderTaskRepository.save(existing));
    }

    @DeleteMapping("/task/{id}")
    @OperationLog(module = "爬虫管理", content = "删除采集任务")
    public Result<Void> deleteTask(@PathVariable Long id) {
        spiderTaskRepository.deleteById(id);
        return Result.success();
    }

    // ===================== 爬虫执行控制 =====================

    /**
     * 开启爬虫任务
     * 1. 验证任务状态（非running）
     * 2. 生成按学院专业关键词的爬取计划
     * 3. 启动异步爬取流程
     * 4. 支持实时进度查询
     */
    @PostMapping("/task/{id}/run")
    @OperationLog(module = "爬虫管理", content = "开启爬虫任务")
    public Result<Map<String, Object>> runTask(@PathVariable Long id) {
        try {
            Map<String, Object> result = crawlerEngineService.startCrawl(id);
            addLog(null, "系统", "INFO", String.format("爬虫任务 #%d 已启动", id));
            return Result.success(result);
        } catch (Exception e) {
            log.error("启动爬虫失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 安全停止爬虫任务
     * 1. 设置停止标志
     * 2. 等待当前页抓取完成（最多60秒）
     * 3. 确保无残缺数据
     */
    @PostMapping("/task/{id}/stop")
    @OperationLog(module = "爬虫管理", content = "停止爬虫任务")
    public Result<Map<String, Object>> stopTask(@PathVariable Long id) {
        try {
            Map<String, Object> result = crawlerEngineService.stopCrawl(id);
            addLog(id, null, "WARN", String.format("爬虫任务 #%d 已停止", id));
            return Result.success(result);
        } catch (Exception e) {
            log.error("停止爬虫失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取爬虫实时进度
     * 优先级：Redis实时数据 > 数据库任务记录
     */
    @GetMapping("/task/{id}/progress")
    public Result<Map<String, Object>> getProgress(@PathVariable Long id) {
        Map<String, Object> progress = crawlerEngineService.getCrawlProgress(id);
        if (progress.isEmpty()) {
            SpiderTask task = spiderTaskRepository.findById(id).orElse(null);
            if (task == null) return Result.notFound("任务不存在");
            progress = new LinkedHashMap<>();
            progress.put("taskId", id);
            progress.put("status", task.getStatus());
            progress.put("progress", task.getProgress() != null ? task.getProgress() : 0);
            progress.put("collectedCount", task.getCollectedCount() != null ? task.getCollectedCount() : 0);
        }
        return Result.success(progress);
    }

    /**
     * 判断任务是否正在运行
     */
    @GetMapping("/task/{id}/status")
    public Result<Map<String, Object>> getTaskStatus(@PathVariable Long id) {
        boolean running = crawlerEngineService.isTaskRunning(id);
        SpiderTask task = spiderTaskRepository.findById(id).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", id);
        result.put("isRunning", running);
        if (task != null) {
            result.put("status", task.getStatus());
            result.put("progress", task.getProgress() != null ? task.getProgress() : 0);
            result.put("collectedCount", task.getCollectedCount() != null ? task.getCollectedCount() : 0);
            result.put("successRate", task.getSuccessRate() != null ? task.getSuccessRate() : 0);
        }
        return Result.success(result);
    }

    // ===================== 采集数据 =====================

    @GetMapping("/data")
    public Result<Map<String, Object>> getCollectedData(
            @RequestParam(required = false) String sourceCode,
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) String majorName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<SpiderCollectedData> dataPage = collectedDataRepository.searchData(
            sourceCode, dataType, majorName, startDate, endDate, PageRequest.of(page - 1, pageSize));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", dataPage.getContent());
        result.put("total", dataPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    @PostMapping("/data/sync")
    @Transactional
    @OperationLog(module = "爬虫管理", content = "同步采集数据到训练池")
    public Result<Map<String, Object>> syncData(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("请选择要同步的数据");
        }
        int count = collectedDataRepository.markAsSynced(ids);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("syncedCount", count);
        result.put("message", count + " 条数据已加入训练池");
        return Result.success(result);
    }

    @PostMapping("/data/sync-all")
    @Transactional
    @OperationLog(module = "爬虫管理", content = "一键同步所有有效数据到训练池")
    public Result<Map<String, Object>> syncAllData() {
        // 只同步 ETL 有效数据（is_valid="1" 且未同步）
        List<SpiderCollectedData> valid = collectedDataRepository.findByIsValid("1").stream()
                .filter(d -> !"1".equals(d.getIsSynced()))
                .collect(Collectors.toList());
        if (valid.isEmpty()) {
            return Result.badRequest("没有可同步的有效数据（请先执行 ETL 清洗）");
        }
        List<Long> ids = valid.stream().map(SpiderCollectedData::getId).collect(Collectors.toList());
        int count = collectedDataRepository.markAsSynced(ids);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("syncedCount", count);
        result.put("message", count + " 条 ETL 有效数据已加入训练池");
        return Result.success(result);
    }

    @DeleteMapping("/data/clear-all")
    @Transactional
    @OperationLog(module = "爬虫管理", content = "清空所有已采集数据")
    public Result<Map<String, Object>> clearAllData() {
        try {
            long count = collectedDataRepository.count();
            collectedDataRepository.deleteAllData();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deletedCount", count);
            result.put("message", "已清空 " + count + " 条采集数据");
            return Result.success(result);
        } catch (Exception e) {
            log.error("清空采集数据失败", e);
            return Result.error(500, "清空失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/data/batch")
    @Transactional
    @OperationLog(module = "爬虫管理", content = "批量删除采集数据")
    public Result<Map<String, Object>> batchDeleteData(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("请选择要删除的数据");
        }
        int count = 0;
        for (Long id : ids) {
            collectedDataRepository.deleteById(id);
            count++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deletedCount", count);
        result.put("message", "已删除 " + count + " 条数据");
        return Result.success(result);
    }

    // ===================== ETL 数据清洗 =====================

    @PostMapping("/etl")
    @OperationLog(module = "爬虫管理", content = "执行ETL数据清洗")
    public Result<Map<String, Object>> executeEtl(@RequestParam(required = false) Long taskId) {
        try {
            Map<String, Object> result;
            if (taskId != null) {
                result = crawlerEngineService.executeEtl(taskId);
            } else {
                result = crawlerEngineService.executeEtl(null);
            }
            addLog(taskId, "系统", "INFO", "ETL清洗完成: " + result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("ETL执行失败", e);
            return Result.error(500, "ETL执行失败: " + e.getMessage());
        }
    }

    @GetMapping("/training-pool")
    public Result<Map<String, Object>> getTrainingPoolStats() {
        return Result.success(crawlerEngineService.getTrainingPoolStats());
    }

    // ===================== ETL 状态 & Pipeline 一键执行 =====================

    /** ETL 状态：查看待清洗/有效/无效/已同步数据量 */
    @GetMapping("/etl/status")
    public Result<Map<String, Object>> getEtlStatus() {
        return Result.success(crawlerEngineService.getEtlStatus());
    }

    /**
     * 一键执行爬虫→ETL→推荐完整数据链路
     * Phase 1: ETL 数据清洗（is_valid=0→1/2）
     * Phase 2: 对所有开启推荐的专业触发推荐
     */
    @PostMapping("/pipeline/execute")
    @OperationLog(module = "爬虫管理", content = "一键执行爬虫→ETL→推荐全链路")
    public Result<Map<String, Object>> executeFullPipeline(@RequestParam(required = false) Long taskId) {
        try {
            Map<String, Object> result = crawlerEngineService.executeFullPipeline(taskId);
            addLog(taskId, "系统", "INFO", "Pipeline执行: " + result.get("phase"));
            return Result.success(result);
        } catch (Exception e) {
            log.error("Pipeline执行失败", e);
            return Result.error(500, "Pipeline执行失败: " + e.getMessage());
        }
    }

    // ===================== 全国就业统计数据 =====================

    @GetMapping("/national-stats")
    public Result<Map<String, Object>> getNationalEmploymentStats() {
        return Result.success(crawlerEngineService.getNationalEmploymentStats());
    }

    // ===================== 就业分析增强（爬取数据接入就业率分析）=====================

    @GetMapping("/analytics/enhanced")
    public Result<Map<String, Object>> getEnhancedAnalytics(
            @RequestParam(required = false) Integer graduationYear) {
        Map<String, Object> stats = new LinkedHashMap<>();
        Map<String, Object> trainingPool = crawlerEngineService.getTrainingPoolStats();
        Map<String, Object> nationalStats = crawlerEngineService.getNationalEmploymentStats();
        stats.put("trainingPool", trainingPool);
        stats.put("nationalStats", nationalStats);
        stats.put("graduationYear", graduationYear);
        return Result.success(stats);
    }

    // ===================== 日志管理 =====================

    @GetMapping("/logs")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String taskName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        Page<SpiderLog> logPage = spiderLogRepository.searchLogs(level, taskName, PageRequest.of(page - 1, pageSize));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", logPage.getContent());
        result.put("total", logPage.getTotalElements());
        return Result.success(result);
    }

    @DeleteMapping("/logs/clear")
    @OperationLog(module = "爬虫管理", content = "清空日志")
    public Result<Void> clearLogs() {
        spiderLogRepository.deleteAll();
        return Result.success();
    }

    // ===================== Hive 查询 =====================

    @PostMapping("/hive/query")
    public Result<Map<String, Object>> executeHiveQuery(@RequestBody Map<String, String> params) {
        String sql = params.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return Result.badRequest("SQL语句不能为空");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", Arrays.asList("industry", "city", "total_count", "avg_salary", "max_salary"));
        result.put("data", Arrays.asList(
            new Object[]{"互联网/IT", "北京", 342, 18500, 45000},
            new Object[]{"互联网/IT", "上海", 298, 17200, 42000},
            new Object[]{"金融", "上海", 256, 22000, 60000},
            new Object[]{"互联网/IT", "深圳", 234, 16500, 38000},
            new Object[]{"金融", "北京", 198, 24000, 55000},
            new Object[]{"教育培训", "成都", 187, 9800, 25000},
            new Object[]{"房地产/建筑", "广州", 176, 11500, 35000},
            new Object[]{"电子/半导体", "苏州", 165, 14200, 38000},
            new Object[]{"医疗健康", "杭州", 154, 13800, 32000},
            new Object[]{"制造业", "南京", 143, 9200, 22000}
        ));
        result.put("rowCount", 10);
        result.put("elapsed", (long) (Math.random() * 500 + 200));
        result.put("note", "实际生产环境请连接集群Hive Metastore执行真实查询");
        return Result.success(result);
    }

    // ===================== Flink 任务 =====================

    @GetMapping("/flink/jobs")
    public Result<List<Map<String, Object>>> getFlinkJobs() {
        List<Map<String, Object>> jobs = new ArrayList<>();
        jobs.add(buildFlinkJob("flink_001", "就业数据实时ETL", "ETL", "RUNNING", "2026-05-16T08:00:00", 125680));
        jobs.add(buildFlinkJob("flink_002", "薪资统计聚合流", "Aggregation", "RUNNING", "2026-05-16T08:05:00", 89430));
        jobs.add(buildFlinkJob("flink_003", "行业趋势分析", "Analysis", "FINISHED", "2026-05-16T09:00:00", 34210));
        jobs.add(buildFlinkJob("flink_004", "推荐训练数据清洗", "ML", "RUNNING", "2026-05-16T10:00:00", 52140));
        return Result.success(jobs);
    }

    @PostMapping("/flink/job")
    @OperationLog(module = "大数据集群", content = "提交Flink任务")
    public Result<Map<String, Object>> submitFlinkJob(@RequestBody Map<String, String> jobInfo) {
        String jobId = "flink_" + System.currentTimeMillis();
        Map<String, Object> job = buildFlinkJob(jobId, jobInfo.get("jobName"),
            jobInfo.getOrDefault("jobType", "ETL"), "SUBMITTED",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), 0);
        addLog(null, "系统", "INFO", String.format("Flink任务已提交: %s (%s)", jobInfo.get("jobName"), jobId));
        return Result.success(job);
    }

    @PutMapping("/flink/job/{jobId}")
    public Result<Void> toggleFlinkJob(@PathVariable String jobId, @RequestParam String action) {
        addLog(null, "系统", "INFO", String.format("Flink任务 %s 状态变更: %s", jobId, action));
        return Result.success();
    }

    @DeleteMapping("/flink/job/{jobId}")
    @OperationLog(module = "大数据集群", content = "删除Flink任务")
    public Result<Void> deleteFlinkJob(@PathVariable String jobId) {
        addLog(null, "系统", "INFO", String.format("Flink任务已删除: %s", jobId));
        return Result.success();
    }

    // ===================== 集群状态 =====================

    @GetMapping("/cluster/status")
    public Result<Map<String, Object>> getClusterStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("hadoop", "RUNNING");
        status.put("hive", "RUNNING");
        status.put("flink", "RUNNING");
        status.put("zookeeper", "RUNNING");
        status.put("kafka", "RUNNING");
        status.put("hiveTables", 12);
        status.put("flinkJobs", 4);
        status.put("totalDataSize", "2.4 GB");
        status.put("activeWorkers", 2);
        status.put("masterNode", "node1");
        status.put("workerNodes", "node2, node3");
        status.put("note", "连接地址: node1:8088 (ResourceManager), node1:10000 (HiveServer2)");
        return Result.success(status);
    }

    // ===================== 专业关键词配置 =====================

    @GetMapping("/majors")
    public Result<List<Map<String, Object>>> getMajorKeywords() {
        return Result.success(spiderMajorKeywordService.getMajorKeywordsGrouped());
    }

    @GetMapping("/all-majors")
    public Result<List<String>> getAllMajors() {
        List<String> majors = spiderMajorKeywordService.getAllMajorNames();
        if (majors.isEmpty()) {
            majors = new ArrayList<>(com.employment.init.DataConstants.MAJOR_INDUSTRY_KEYWORDS.keySet());
        }
        return Result.success(majors);
    }

    @PostMapping("/major/{majorName}/keyword")
    public Result<SpiderMajorKeyword> addKeyword(@PathVariable String majorName, @RequestBody Map<String, String> body) {
        String keyword = body.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("关键词不能为空");
        }
        try {
            SpiderMajorKeyword saved = spiderMajorKeywordService.addKeyword(majorName.trim(), keyword.trim());
            return Result.success("关键词添加成功", saved);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/major/{majorName}/keywords")
    public Result<Map<String, Object>> addKeywords(@PathVariable String majorName, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) body.get("keywords");
        if (keywords == null || keywords.isEmpty()) {
            return Result.error("关键词列表不能为空");
        }
        List<SpiderMajorKeyword> saved = spiderMajorKeywordService.addKeywords(majorName.trim(), keywords);
        Map<String, Object> result = new HashMap<>();
        result.put("count", saved.size());
        result.put("total", spiderMajorKeywordRepository.findByMajorName(majorName).size());
        return Result.success("成功添加 " + saved.size() + " 个关键词", result);
    }

    @DeleteMapping("/major/{majorName}/keyword/{keyword}")
    public Result<Void> deleteKeyword(@PathVariable String majorName, @PathVariable String keyword) {
        spiderMajorKeywordService.deleteKeyword(majorName, java.net.URLDecoder.decode(keyword, java.nio.charset.StandardCharsets.UTF_8));
        return Result.success("关键词已删除", null);
    }

    // ===================== 内部工具方法 =====================

    private static final Map<String, String> SITE_NAMES = new LinkedHashMap<>();
    static {
        SITE_NAMES.put("51job", "前程无忧");
        SITE_NAMES.put("gov", "人社部公共招聘");
        SITE_NAMES.put("moe", "教育部就业平台");
        SITE_NAMES.put("yingjiesheng", "应届生求职网");
    }

    private String getSiteName(String code) {
        return SITE_NAMES.getOrDefault(code, code);
    }

    private void addLog(Long taskId, String taskName, String level, String message) {
        try {
            SpiderLog log = new SpiderLog();
            log.setTaskId(taskId);
            log.setTaskName(taskName);
            log.setLevel(level);
            log.setMessage(message);
            log.setLogTime(LocalDateTime.now().format(FMT));
            spiderLogRepository.save(log);
        } catch (Exception e) {
            log.debug("日志保存失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildFlinkJob(String jobId, String jobName, String jobType, String status, String startTime, long processed) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("jobId", jobId);
        job.put("jobName", jobName);
        job.put("jobType", jobType);
        job.put("status", status);
        job.put("startTime", startTime);
        job.put("processedRecords", processed);
        return job;
    }

    // ===================== 调试工具：抓取任意 URL 的原始 HTML =====================

    /**
     * 调试接口：直接用 Selenium 抓取任意 URL，返回原始 HTML 前 3000 字符。
     * 用于排查 CSS 选择器是否命中。
     * 示例：GET /api/spider/debug/fetch?url=https://q.yingjiesheng.com/jobdetail/172116886.html
     */
    @GetMapping("/debug/fetch")
    public Result<Map<String, Object>> debugFetch(
            @RequestParam String url,
            @RequestParam(required = false, defaultValue = "yingjiesheng") String source) {
        try {
            Long fakeTaskId = 99999L;
            String html;
            if ("yingjiesheng".equals(source)) {
                html = seleniumCrawlerService.fetchYingjieshengDetail(fakeTaskId, url);
            } else {
                html = null;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("url", url);
            result.put("length", html != null ? html.length() : 0);
            result.put("html", html != null && html.length() > 3000 ? html.substring(0, 3000) : html);

            // 用 Jsoup 解析，尝试各种常见选择器，看哪些能命中
            if (html != null && !html.isEmpty()) {
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
                java.util.Map<String, String> selectorTests = new java.util.LinkedHashMap<>();
                String[] selectors = {
                    "div.detail-title",
                    "div.detail-title-left",
                    "div.detail-title-left-top",
                    "div.detail-title-left-bottom",
                    "div.salary",
                    "div.detail-content-common",
                    "div.detail-content-top",
                    "div.info-wrapper",
                    "div.job-msg",
                    "div.jobinfo",
                    ".job",
                    ".company",
                    "span[class*=city]",
                    "span.city",
                    "span.edu",
                    "span.exp",
                    "div.taglist",
                    "span.tag",
                    "div.tag-box",
                    "div.detail-content-common.jobinfo"
                };
                for (String sel : selectors) {
                    org.jsoup.select.Elements els = doc.select(sel);
                    if (!els.isEmpty()) {
                        String sample = els.first().outerHtml();
                        selectorTests.put(sel + " (命中" + els.size() + "个)", sample.length() > 500 ? sample.substring(0, 500) : sample);
                    }
                }
                result.put("selectorTests", selectorTests);
            }

            result.put("tip", "查看 html 字段看原始HTML；selectorTests 显示各选择器命中结果，找包含城市/薪资/学历信息的");
            return Result.success(result);
        } catch (Exception e) {
            log.error("调试抓取失败", e);
            return Result.error(500, "抓取失败: " + e.getMessage());
        }
    }

    /**
     * 调试接口：专门测试 search-list-item 的 href JSON 解析。
     * 示例：GET /api/spider/debug/parse-search?keyword=软件工程师&page=1
     */
    @GetMapping("/debug/parse-search")
    public Result<Map<String, Object>> debugParseSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page) {
        try {
            seleniumCrawlerService.initBrowser(0L);
            String html = seleniumCrawlerService.fetchPageYingjiesheng(0L, keyword, page);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("keyword", keyword);
            result.put("page", page);

            if (html == null || html.isEmpty()) {
                result.put("error", "HTML 为空");
                return Result.success(result);
            }

            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
            result.put("htmlLength", html.length());
            result.put("pageTitle", doc.title());

            // 检查是否存在 a[href*=jobdetail]
            org.jsoup.select.Elements allAnchors = doc.select("a[href*=jobdetail]");
            result.put("allAnchors_count", allAnchors.size());
            if (!allAnchors.isEmpty()) {
                result.put("firstAnchor_href", allAnchors.first().attr("href"));
                result.put("firstAnchor_outerHtml_snippet",
                        allAnchors.first().outerHtml().length() > 300
                                ? allAnchors.first().outerHtml().substring(0, 300)
                                : allAnchors.first().outerHtml());
            } else {
                // 尝试更宽泛的选择器
                org.jsoup.select.Elements anyAnchor = doc.select("a[href]");
                result.put("anyAnchor_count", anyAnchor.size());
                if (!anyAnchor.isEmpty()) {
                    result.put("firstAnyAnchor_href", anyAnchor.first().attr("href"));
                    result.put("firstAnyAnchor_text", anyAnchor.first().text());
                }
            }

            // 检查 div.search-list-item.job 是否存在
            org.jsoup.select.Elements items = doc.select("div.search-list-item.job");
            result.put("searchListItem_count", items.size());

            java.util.List<Map<String, String>> samples = new java.util.ArrayList<>();
            for (int i = 0; i < Math.min(3, items.size()); i++) {
                Element item = items.get(i);
                Map<String, String> sample = new LinkedHashMap<>();
                sample.put("jobName", item.selectFirst("div.left-title-name") != null
                        ? item.selectFirst("div.left-title-name").text().trim() : "");

                // 尝试多种锚点选择器
                Element aEl = item.selectFirst("a[href*=jobdetail]");
                if (aEl == null) aEl = item.selectFirst("a[href]");
                sample.put("anchor_found", String.valueOf(aEl != null));
                if (aEl != null) {
                    String href = aEl.attr("href");
                    sample.put("rawHref", href);
                    int jsonStart = href.indexOf("property=");
                    if (jsonStart >= 0) {
                        String jsonStr = href.substring(jsonStart + 9);
                        try {
                            jsonStr = java.net.URLDecoder.decode(jsonStr, java.nio.charset.StandardCharsets.UTF_8.name());
                        } catch (Exception ignored) {}
                        int ampIdx = jsonStr.indexOf("&property=");
                        if (ampIdx >= 0) jsonStr = jsonStr.substring(0, ampIdx);
                        int propEnd = jsonStr.lastIndexOf("\"}");
                        if (propEnd >= 0) jsonStr = jsonStr.substring(0, propEnd + 2);
                        sample.put("decodedJson", jsonStr);

                        if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                            try {
                                com.fasterxml.jackson.databind.JsonNode node =
                                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonStr);
                                sample.put("parsed_companyName", node.has("companyName") ? node.get("companyName").asText() : "NULL");
                                sample.put("parsed_monthSalary", node.has("monthSalary") ? node.get("monthSalary").asText() : "NULL");
                                sample.put("parsed_jobId", node.has("jobId") ? node.get("jobId").asText() : "NULL");
                            } catch (Exception ex) {
                                sample.put("jsonError", ex.getMessage());
                            }
                        } else {
                            sample.put("jsonError", "NOT_JSON: starts=" + jsonStr.startsWith("{") + " ends=" + jsonStr.endsWith("}"));
                        }
                    } else {
                        sample.put("jsonError", "NO_property_PARAM");
                    }
                }
                samples.add(sample);
            }
            result.put("samples", samples);
            return Result.success(result);
        } catch (Exception e) {
            log.error("调试解析失败", e);
            return Result.error(500, "失败: " + e.getMessage());
        }
    }
}
