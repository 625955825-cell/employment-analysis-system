package com.employment.service.impl;

import com.employment.init.DataConstants;
import com.employment.model.entity.CrawlerJobRecord;
import com.employment.model.entity.JobPosition;
import com.employment.model.entity.SpiderCollectedData;
import com.employment.model.entity.SpiderTask;
import com.employment.model.entity.SysMajor;
import com.employment.repository.CrawlerJobRecordRepository;
import com.employment.repository.JobPositionRepository;
import com.employment.repository.SpiderCollectedDataRepository;
import com.employment.repository.SpiderTaskRepository;
import com.employment.repository.SysMajorRepository;
import com.employment.service.CrawlerEngineService;
import com.employment.service.extractor.Job51jobExtractor;
import com.employment.service.extractor.JobEducationExtractor;
import com.employment.service.extractor.JobGovExtractor;
import com.employment.service.extractor.JobYingjieshengExtractor;
import com.employment.service.extractor.SiteDataExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerEngineServiceImpl implements CrawlerEngineService {

    private final SpiderTaskRepository spiderTaskRepository;
    private final SpiderCollectedDataRepository collectedDataRepository;
    private final CrawlerJobRecordRepository jobRecordRepository;
    private final SysMajorRepository sysMajorRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SeleniumCrawlerService seleniumCrawlerService;
    private final CrawlerAsyncRunner crawlerAsyncRunner;
    private final JobYingjieshengExtractor jobYingjieshengExtractor;
    private final com.employment.service.RecommendService recommendService;
    private final JobPositionRepository jobPositionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /** 按数据源代码索引的站点提取器 */
    private final Map<String, SiteDataExtractor> extractorMap = new HashMap<>();

    @PostConstruct
    public void initExtractors() {
        extractorMap.put("51job", new Job51jobExtractor());
        extractorMap.put("gov", new JobGovExtractor());
        extractorMap.put("moe", new JobEducationExtractor());
        extractorMap.put("yingjiesheng", jobYingjieshengExtractor);
        log.info("数据提取器初始化完成: {}", extractorMap.keySet());
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String REDIS_CRAWL_PREFIX = "crawl:task:";
    private static final String REDIS_CRAWL_RUNNING = ":running";
    private static final String REDIS_CRAWL_STOPPING = ":stopping";
    private static final String REDIS_CRAWL_PROGRESS = ":progress";

    // 前程无忧 URL 配置
    private static final Map<String, String> SITE_BASE_URLS = new LinkedHashMap<>();
    private static final Map<String, String> SITE_SEARCH_PATHS = new LinkedHashMap<>();
    static {
        SITE_BASE_URLS.put("51job", "https://we.51job.com");
        SITE_BASE_URLS.put("gov", "http://job.mohrss.gov.cn");
        SITE_BASE_URLS.put("moe", "https://bjbys.ncss.cn");
        SITE_SEARCH_PATHS.put("51job", "/api/job/search-pc");
        SITE_SEARCH_PATHS.put("gov", "/cjobs/jobinfolist/listJobinfolist");
        SITE_SEARCH_PATHS.put("moe", "/student/jobs/index.html");
    }

    // 仅保留前程无忧的请求头配置
    private static final Map<String, Map<String, String>> USER_AGENTS = new HashMap<>();
    static {
        Map<String, String> job51Headers = new HashMap<>();
        job51Headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        job51Headers.put("Accept", "text/html");
        job51Headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        job51Headers.put("Referer", "https://www.51job.com/");
        USER_AGENTS.put("51job", job51Headers);

        Map<String, String> govHeaders = new HashMap<>();
        govHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        govHeaders.put("Accept", "text/html,application/xhtml+xml");
        govHeaders.put("Accept-Language", "zh-CN,zh;q=0.9");
        govHeaders.put("Referer", "https://www.job.mohrss.gov.cn/");
        USER_AGENTS.put("gov", govHeaders);

        Map<String, String> moeHeaders = new HashMap<>();
        moeHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        moeHeaders.put("Accept", "text/html,application/xhtml+xml");
        moeHeaders.put("Accept-Language", "zh-CN,zh;q=0.9");
        moeHeaders.put("Referer", "https://bysjy.moe.edu.cn/");
        USER_AGENTS.put("moe", moeHeaders);
    }

    private static final Map<Long, AtomicBoolean> TASK_RUNNING_MAP = new ConcurrentHashMap<>();
    private static final Map<Long, AtomicBoolean> TASK_STOPPING_MAP = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public Map<String, Object> startCrawl(Long taskId) {
        SpiderTask task = spiderTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));

        if ("running".equals(task.getStatus())) {
            throw new RuntimeException("任务已在运行中");
        }

        AtomicBoolean running = TASK_RUNNING_MAP.computeIfAbsent(taskId, k -> new AtomicBoolean(false));
        if (running.get()) {
            throw new RuntimeException("任务已在运行中");
        }
        running.set(true);
        TASK_STOPPING_MAP.put(taskId, new AtomicBoolean(false));

        // 清理旧的 Redis 进度数据
        try {
            String key = REDIS_CRAWL_PREFIX + taskId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("清理旧进度数据失败: {}", e.getMessage());
        }

        task.setStatus("running");
        task.setProgress(0);
        task.setCollectedCount(0);
        task.setSuccessRate(0);
        task.setLastRunTime(LocalDateTime.now().format(FMT));
        task.setLastError(null);
        spiderTaskRepository.save(task);

        persistProgressToRedis(taskId, 0, 0, 0);

        // 通过另一个Bean触发异步执行，绕过Self-Invocation问题
        crawlerAsyncRunner.runAsync(taskId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", "running");
        result.put("message", "爬虫任务已启动");
        return result;
    }

    @Override
    public void executeCrawl(Long taskId) {
        spiderTaskRepository.findById(taskId).ifPresent(task -> {
            AtomicBoolean running = TASK_RUNNING_MAP.get(taskId);
            AtomicBoolean stopping = TASK_STOPPING_MAP.get(taskId);
            if (running == null || stopping == null) return;
            executeCrawlInternal(taskId, task, running, stopping);
        });
    }

    private void executeCrawlInternal(Long taskId, SpiderTask task, AtomicBoolean running, AtomicBoolean stopping) {

        String sourceCode = task.getSourceCode();
        List<String> majors;
        if (task.getSelectedMajors() != null && !task.getSelectedMajors().trim().isEmpty()) {
            majors = Arrays.asList(task.getSelectedMajors().split(","));
        } else {
            majors = getAllMajorNames();
        }
        List<String> keywords = getAllIndustryKeywords();

        int totalPages = task.getDepth() != null ? task.getDepth() : 2;
        int maxPagesPerMajor = totalPages;

        int totalJobs = 0;
        int totalSuccess = 0;

        try {
            for (String major : majors) {
                if (stopping.get()) {
                    log.info("任务 {} 检测到停止信号，提前退出专业: {}", taskId, major);
                    break;
                }
                if (!running.get()) {
                    log.info("任务 {} 已停止，退出专业: {}", taskId, major);
                    break;
                }

                List<String> majorKeywords = getKeywordsForMajor(major);
                for (String keyword : majorKeywords) {
                    if (stopping.get() || !running.get()) break;

                    for (int page = 1; page <= maxPagesPerMajor; page++) {
                        if (stopping.get() || !running.get()) break;

                        String url = buildSearchUrl(sourceCode, keyword, page);
                        CrawlerJobRecord record = createJobRecord(taskId, sourceCode, major, keyword, url, page);

                        try {
                            String html = null;
                            String jsonResponse = null;

                            // 仅使用前程无忧专用爬取方式
                            if ("51job".equals(sourceCode)) {
                                // 优先使用 WebClient 直连 JSON API（绕过 Selenium 反爬）
                                jsonResponse = seleniumCrawlerService.fetchPage51jobViaHttp(keyword, page);
                                if (jsonResponse != null && !jsonResponse.isEmpty()) {
                                    // 将 JSON 转换为 HTML 结构，复用现有 JSoup 解析逻辑
                                    html = seleniumCrawlerService.convert51jobJsonToHtml(jsonResponse, keyword);
                                } else {
                                    log.warn("任务 {} 51job WebClient 请求失败，使用 Selenium 兜底...", taskId);
                                    html = seleniumCrawlerService.fetchPage51job(taskId, keyword, page);
                                }
                            } else if ("gov".equals(sourceCode)) {
                                // 人社部公共招聘网（无反爬）
                                html = seleniumCrawlerService.fetchPageGov(taskId, keyword, page);
                            } else if ("moe".equals(sourceCode)) {
                                // 教育部就业平台（无反爬）
                                html = seleniumCrawlerService.fetchPageEducation(taskId, keyword, page);
                            } else if ("yingjiesheng".equals(sourceCode)) {
                                // 应届生求职网：优先用 Selenium（能绕过反爬，拿到完整 3MB+ HTML）
                                html = seleniumCrawlerService.fetchPageYingjiesheng(taskId, keyword, page);
                                if (html == null || html.length() < 50000) {
                                    log.warn("任务 {} 应届生求职网 Selenium 返回为空或过小({}字节)，尝试 HTTP 兜底...", taskId,
                                            html != null ? html.length() : 0);
                                    String htmlHttp = seleniumCrawlerService.fetchPageYingjieshengViaHttp(keyword, page);
                                    if (htmlHttp != null && htmlHttp.length() > html.length()) {
                                        html = htmlHttp;
                                    }
                                }
                            } else {
                                html = fetchPage(taskId, url, sourceCode);
                            }

                            // 检测是否为 WAF 拦截页面（阿里云 WAF 返回 HTML 而非 JSON）
                            boolean isWafBlocked = (jsonResponse != null)
                                && (jsonResponse.trim().startsWith("<") || jsonResponse.contains("aliyun_waf"));

                            List<SpiderCollectedData> dataList = null;
                            if (html != null && !html.trim().isEmpty()) {
                                dataList = parseJobListings(taskId, html, sourceCode, major, keyword);
                            }

                            // TODO: WAF 真实爬取（当前被阿里云拦截，模拟数据兜底暂时关闭以便调试真实爬虫）
                            // if (dataList == null || dataList.isEmpty()) {
                            //     log.warn("任务 {} 关键词={} 真实数据为空，使用模拟数据兜底", taskId, keyword);
                            //     dataList = generateMockDataForKeyword(sourceCode, major, keyword);
                            // }

                            if (dataList == null) {
                                throw new RuntimeException("任务 " + taskId + " 关键词=" + keyword + " 页面内容为空（WAF拦截或解析异常）");
                            }
                            if (dataList.isEmpty()) {
                                throw new RuntimeException("任务 " + taskId + " 关键词=" + keyword + " 解析结果为空，未找到有效职位数据");
                            }

                            record.setRawHtml(html);
                            record.setStatus("success");
                            jobRecordRepository.save(record);

                            log.info("任务 {} 关键词={} 共 {} 条数据", taskId, keyword, dataList.size());

                            // 两阶段爬取：搜索页解析 → 详情页补充
                            int detailSuccessCount = 0;
                            int detailFailCount = 0;
                            for (SpiderCollectedData data : dataList) {
                                if (stopping.get() || !running.get()) {
                                    break;
                                }

                                // yingjiesheng 详情页 URL 内嵌 property JSON（含薪资、公司名）。
                                // fetchYingjieshengDetail 内部已自动处理登录态维持。
                                if ("yingjiesheng".equals(sourceCode) && data.getDetailUrl() != null && !data.getDetailUrl().isEmpty()) {
                                    try {
                                        // 优先从 URL JSON 获取薪资和公司（无需请求，速度快）
                                        JsonNode propertyJson = JobYingjieshengExtractor.parsePropertyFromUrl(data.getDetailUrl());
                                        if (propertyJson != null) {
                                            String urlJobName = propertyJson.path("jobTitle").asText("");
                                            if (urlJobName.isEmpty()) urlJobName = propertyJson.path("title").asText("");
                                            String urlCompanyName = propertyJson.path("companyName").asText("");
                                            String urlSalary = propertyJson.path("monthSalary").asText("");

                                            if (!urlJobName.isEmpty()) data.setJobName(urlJobName);
                                            if (!urlCompanyName.isEmpty()) data.setCompanyName(urlCompanyName);
                                            if (!urlSalary.isEmpty()) data.setSalary(urlSalary);
                                        }

                                        // 访问详情页，提取城市、学历、经验、职责等
                                        String detailHtml = seleniumCrawlerService.fetchYingjieshengDetail(taskId, data.getDetailUrl());
                                        if (detailHtml != null && !detailHtml.isEmpty()) {
                                            Document detailDoc = Jsoup.parse(detailHtml);
                                            SiteDataExtractor extractor = extractorMap.get(sourceCode);
                                            if (extractor != null) {
                                                SpiderCollectedData detailData = extractor.extractFromDetail(
                                                        detailDoc, sourceCode, major, keyword,
                                                        data.getJobName(), data.getDetailUrl());
                                                if (detailData != null) {
                                                    // HTML 详情页数据补充城市等字段（URL JSON 已有薪资/公司名，不要覆盖）
                                                    if (data.getCity() == null || data.getCity().isEmpty())
                                                        data.setCity(detailData.getCity());
                                                    if (data.getEducation() == null || data.getEducation().isEmpty())
                                                        data.setEducation(detailData.getEducation());
                                                    if (data.getExperience() == null || data.getExperience().isEmpty())
                                                        data.setExperience(detailData.getExperience());
                                                    if (data.getResponsibility() == null || data.getResponsibility().isEmpty())
                                                        data.setResponsibility(detailData.getResponsibility());
                                                    if (data.getSkills() == null || data.getSkills().isEmpty())
                                                        data.setSkills(detailData.getSkills());
                                                    if (detailData.getRawData() != null && !detailData.getRawData().isEmpty()
                                                            && data.getRawData() != null && data.getRawData().startsWith("http"))
                                                        data.setRawData(detailData.getRawData());
                                                    detailSuccessCount++;
                                                    log.debug("任务 {} 详情页成功: {} -> {}", taskId, data.getJobName(), data.getDetailUrl());
                                                } else {
                                                    detailFailCount++;
                                                }
                                            }
                                        } else {
                                            detailFailCount++;
                                        }
                                    } catch (Exception e) {
                                        log.debug("任务 {} 详情页抓取异常 url={}: {}", taskId, data.getDetailUrl(), e.getMessage());
                                        detailFailCount++;
                                    }
                                }

                                try {
                                    // 确保关键字段不为 null，避免数据库约束冲突
                                    if (data.getSourceCode() == null) data.setSourceCode(sourceCode);
                                    if (data.getDataType() == null) data.setDataType("job");
                                    if (data.getMajorName() == null) data.setMajorName(major);
                                    if (data.getIndustryKeyword() == null) data.setIndustryKeyword(keyword);
                                    if (data.getJobName() == null) data.setJobName("未知职位");
                                    if (data.getCompanyName() == null) data.setCompanyName("未知公司");
                                    if (data.getCollectTime() == null) data.setCollectTime(LocalDateTime.now().format(FMT));
                                    if (data.getIsSynced() == null) data.setIsSynced("0");

                                    SpiderCollectedData saved = collectedDataRepository.save(data);
                                    if (saved != null && saved.getId() != null) {
                                        totalSuccess++;
                                        totalJobs++;
                                    } else {
                                        log.warn("任务 {} 保存数据返回 null，DB写入可能失败: job={}, company={}",
                                            taskId, data.getJobName(), data.getCompanyName());
                                    }

                                    int currentMajorIdx = majors.indexOf(major);
                                    int currentKeywordIdx = majorKeywords.indexOf(keyword);
                                    int progress = calculateProgress(page, maxPagesPerMajor, currentMajorIdx, majors.size());
                                    persistProgressToRedis(taskId, progress, totalSuccess,
                                        totalSuccess, major, keyword, page, majorKeywords.size(), maxPagesPerMajor,
                                        currentMajorIdx + 1, majors.size(), currentKeywordIdx + 1, majorKeywords.size());
                                } catch (Exception e) {
                                    log.error("保存采集数据失败 (job={}, company={}): {} - {}",
                                        data.getJobName(), data.getCompanyName(), e.getClass().getSimpleName(), e.getMessage());
                                }
                            }
                            if (detailSuccessCount > 0 || detailFailCount > 0) {
                                log.info("任务 {} 关键词={} 详情页抓取: 成功={}, 失败={}", taskId, keyword, detailSuccessCount, detailFailCount);
                            }
                        } catch (Exception e) {
                            log.error("页面抓取失败 [{}] page={}: {}", url, page, e.getMessage());
                            record.setStatus("failed");
                            record.setErrorMessage(e.getMessage());
                            jobRecordRepository.save(record);
                            task.setStatus("failed");
                            task.setLastError("页面抓取失败: " + e.getMessage());
                            spiderTaskRepository.save(task);
                            return;
                        }
                    }
                }
            }

            task.setCollectedCount(totalSuccess);
            task.setStatus("success");
            task.setProgress(100);
            task.setSuccessRate(totalJobs > 0 ? (int) Math.round((double) totalSuccess / totalJobs * 100) : 0);
            spiderTaskRepository.save(task);

            persistProgressToRedis(taskId, 100, totalSuccess, totalSuccess);

        } catch (Exception e) {
            log.error("爬虫执行异常 taskId={}: {}", taskId, e.getMessage(), e);
            task.setStatus("failed");
            task.setLastError(e.getMessage());
            spiderTaskRepository.save(task);
        } finally {
            running.set(false);
            TASK_RUNNING_MAP.remove(taskId);
            TASK_STOPPING_MAP.remove(taskId);
            persistProgressToRedis(taskId, task.getProgress() != null ? task.getProgress() : 0, totalSuccess, totalSuccess);
            // 关闭浏览器，释放资源
            seleniumCrawlerService.closeBrowser(taskId);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> stopCrawl(Long taskId) {
        AtomicBoolean stopping = TASK_STOPPING_MAP.get(taskId);
        AtomicBoolean running = TASK_RUNNING_MAP.get(taskId);

        if (running == null || !running.get()) {
            SpiderTask task = spiderTaskRepository.findById(taskId).orElse(null);
            if (task != null && !"running".equals(task.getStatus())) {
                throw new RuntimeException("任务当前未在运行");
            }
            throw new RuntimeException("任务未在运行");
        }

        stopping.set(true);
        log.info("任务 {} 已发送停止信号，等待当前页抓取完成...", taskId);

        long start = System.currentTimeMillis();
        while (running.get() && System.currentTimeMillis() - start < 60000) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        SpiderTask task = spiderTaskRepository.findById(taskId).orElse(null);
        if (task != null) {
            int pendingRecords = (int) jobRecordRepository.findByTaskIdAndIsComplete(taskId, "0").size();
            if (pendingRecords > 0) {
                log.info("任务 {} 仍有 {} 条未完成记录，等待完成...", taskId, pendingRecords);
            }
            task.setStatus("paused");
            spiderTaskRepository.save(task);
        }

        seleniumCrawlerService.closeBrowser(taskId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", "paused");
        result.put("message", "爬虫已安全停止（当前页抓取完成后自动退出）");
        return result;
    }

    @Override
    public Map<String, Object> getCrawlProgress(Long taskId) {
        Map<String, Object> redisData = getProgressFromRedis(taskId);
        if (!redisData.isEmpty()) {
            return redisData;
        }

        SpiderTask task = spiderTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", task.getStatus());
        result.put("progress", task.getProgress() != null ? task.getProgress() : 0);
        result.put("collectedCount", task.getCollectedCount() != null ? task.getCollectedCount() : 0);
        result.put("isRunning", isTaskRunning(taskId));
        return result;
    }

    @Override
    public boolean isTaskRunning(Long taskId) {
        AtomicBoolean running = TASK_RUNNING_MAP.get(taskId);
        return running != null && running.get();
    }

    @Override
    @Transactional
    public Map<String, Object> executeEtl(Long taskId) {
        // ===== Phase 1: 批量查询待清洗数据（分页避免OOM）=====
        // 只处理"待清洗"(is_valid=0) 且未同步的数据
        int batchSize = 500;
        int totalPending = collectedDataRepository.findByIsValidAndIsSynced("0", "0").size();
        int offset = 0;
        int totalProcessed = 0;
        int totalCleaned = 0;
        int totalDuplicate = 0;
        int totalPolluted = 0;

        // ===== Phase 2: 构建去重 key 集合（从已有有效数据）=====
        // 预加载全量有效 key，避免逐条查库
        Set<String> existingKeys = new HashSet<>();
        for (SpiderCollectedData d : collectedDataRepository.findByIsValid("1")) {
            if (d.getJobName() != null && d.getCompanyName() != null) {
                existingKeys.add(buildDedupKey(d.getJobName(), d.getCompanyName(),
                        d.getSalary(), d.getCity(), d.getIndustryKeyword()));
            }
        }
        // 已同步职位的去重 key
        List<JobPosition> syncedJobs = jobPositionRepository.findByStatusAndIsDeleted("published", "0");
        for (JobPosition jp : syncedJobs) {
            if (jp.getJobName() != null && jp.getCompanyName() != null) {
                existingKeys.add((jp.getJobName() + "|" + jp.getCompanyName()).toLowerCase());
            }
        }

        // 分批处理，每批 500 条，防止事务超时
        // 记录本次 ETL 新增了哪些专业的数据，用于提示用户需要重训
        Set<String> newDataMajors = new HashSet<>();
        while (true) {
            List<SpiderCollectedData> rawList = collectedDataRepository
                    .findByIsValidAndIsSynced("0", "0");
            if (rawList.isEmpty()) break;

            List<Long> validIds = new ArrayList<>();
            List<Long> invalidIds = new ArrayList<>();

            for (SpiderCollectedData raw : rawList) {
                String key = buildDedupKey(
                        raw.getJobName(), raw.getCompanyName(),
                        raw.getSalary(), raw.getCity(), raw.getIndustryKeyword());
                boolean isDuplicate = key != null && existingKeys.contains(key);
                boolean isValid = !isDuplicate && isValidData(raw);

                if (isDuplicate) {
                    invalidIds.add(raw.getId());
                    totalDuplicate++;
                } else if (!isValid) {
                    invalidIds.add(raw.getId());
                    totalPolluted++;
                } else {
                    normalizeData(raw);
                    validIds.add(raw.getId());
                    if (key != null) existingKeys.add(key);
                    totalCleaned++;
                    // 记录新增了哪些专业
                    if (raw.getMajorName() != null && !raw.getMajorName().isBlank()) {
                        newDataMajors.add(raw.getMajorName().trim());
                    }
                }
            }

            // 批量更新有效数据
            if (!validIds.isEmpty()) {
                entityManager.createQuery(
                        "UPDATE SpiderCollectedData d SET d.isValid = '1' WHERE d.id IN :ids")
                        .setParameter("ids", validIds)
                        .executeUpdate();
            }
            // 批量更新无效数据
            if (!invalidIds.isEmpty()) {
                entityManager.createQuery(
                        "UPDATE SpiderCollectedData d SET d.isValid = '2' WHERE d.id IN :ids")
                        .setParameter("ids", invalidIds)
                        .executeUpdate();
            }

            // 分批 flush + clear，防止内存溢出
            entityManager.flush();
            entityManager.clear();

            totalProcessed += validIds.size() + invalidIds.size();
            log.info("ETL 批次完成: 本批有效={}, 无效={}, 累计已处理={}/{}",
                    validIds.size(), invalidIds.size(), totalProcessed, totalPending);

            // 安全保护：最多循环 50 轮（覆盖 50*500=25000 条），超出则终止
            if (totalProcessed >= totalPending || offset > 50) break;
        }

        // ===== Phase 3: 返回统计结果（从 DB 重新查询保证准确）=====
        long validCount = collectedDataRepository.findByIsValid("1").size();
        long invalidCount = collectedDataRepository.findByIsValid("2").size();
        long syncedCount = collectedDataRepository.findByIsSynced("1").size();

        // 找出本次 ETL 新增了数据的专业（相比之前）
        List<SpiderCollectedData> allValid = collectedDataRepository.findByIsValid("1");
        Set<String> allMajorNames = allValid.stream()
                .filter(d -> d.getMajorName() != null && !d.getMajorName().isBlank())
                .map(d -> d.getMajorName().trim())
                .collect(Collectors.toSet());

        // 找出已训练过的专业
        Set<String> trainedMajorNames = sysMajorRepository.findAll().stream()
                .filter(m -> "trained".equals(m.getModelTrained()))
                .map(SysMajor::getMajorName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 过滤出需要重训的专业（有新数据但模型未训练）
        List<String> needRetrain = allMajorNames.stream()
                .filter(m -> !trainedMajorNames.contains(m))
                .sorted()
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalProcessed", totalProcessed);
        result.put("cleanedCount", totalCleaned);
        result.put("duplicateCount", totalDuplicate);
        result.put("pollutedCount", totalPolluted);
        result.put("validCount", validCount);
        result.put("invalidCount", invalidCount);
        result.put("syncedCount", syncedCount);
        result.put("etlStatus", "completed");
        result.put("newDataMajors", needRetrain);
        result.put("message", String.format("清洗完成：有效 %d 条，重复 %d 条，无效 %d 条%s",
                totalCleaned, totalDuplicate, totalPolluted,
                needRetrain.isEmpty() ? "" : "。以下专业有新数据，请点击「一键批量训练」更新算法池：" + String.join("、", needRetrain)));
        return result;
    }

    @Override
    public Map<String, Object> getTrainingPoolStats() {
        long totalRecords = collectedDataRepository.count();
        // ETL 有效数据：is_valid = "1" — 可供推荐
        List<SpiderCollectedData> valid = collectedDataRepository.findByIsValid("1");
        // 待清洗数据：is_valid = "0"
        long pendingCount = collectedDataRepository.findByIsValid("0").size();
        // 无效数据：is_valid = "2"
        long invalidCount = collectedDataRepository.findByIsValid("2").size();
        // 已同步数据
        long syncedCount = collectedDataRepository.findByIsSynced("1").size();

        Map<String, Long> byIndustry = new LinkedHashMap<>();
        for (SpiderCollectedData d : valid) {
            String ind = d.getIndustry() != null ? d.getIndustry() : "未知";
            byIndustry.put(ind, byIndustry.getOrDefault(ind, 0L) + 1);
        }

        Map<String, Long> byCity = new LinkedHashMap<>();
        for (SpiderCollectedData d : valid) {
            String city = d.getCity() != null ? d.getCity() : "未知";
            byCity.put(city, byCity.getOrDefault(city, 0L) + 1);
        }

        Map<String, Long> bySource = new LinkedHashMap<>();
        for (SpiderCollectedData d : valid) {
            String src = d.getSourceCode() != null ? d.getSourceCode() : "未知";
            bySource.put(src, bySource.getOrDefault(src, 0L) + 1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecords", totalRecords);
        result.put("pendingCount", pendingCount);   // 待清洗
        result.put("validCount", valid.size());    // ETL有效，可供推荐
        result.put("invalidCount", invalidCount);  // ETL无效
        result.put("syncedToJobCount", syncedCount);
        result.put("industryDistribution", byIndustry.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("industry", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList()));
        result.put("cityDistribution", byCity.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("city", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList()));
        result.put("sourceDistribution", bySource.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("source", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Object> getNationalEmploymentStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("crawlTime", LocalDateTime.now().format(FMT));
        result.put("dataSource", "基于爬取+ETL清洗数据统计");

        // ===== 1. 整体数据统计 =====
        List<SpiderCollectedData> allData = collectedDataRepository.findAll();
        List<SpiderCollectedData> validData = collectedDataRepository.findByIsValid("1");  // ETL有效数据
        List<SpiderCollectedData> pendingData = collectedDataRepository.findByIsValid("0"); // 待清洗

        Map<String, Object> overallStats = new LinkedHashMap<>();
        overallStats.put("totalCollected", allData.size());
        overallStats.put("totalValid", validData.size());
        overallStats.put("totalPending", pendingData.size());
        overallStats.put("totalInvalid", allData.size() - validData.size() - pendingData.size());
        // 有效数据覆盖率
        double coverage = allData.size() > 0 ? (validData.size() * 100.0 / allData.size()) : 0;
        overallStats.put("validRate", Math.round(coverage * 10) / 10.0);
        result.put("overall", overallStats);

        // ===== 2. 各专业数据量统计（帮助判断哪些专业数据不足） =====
        List<SysMajor> allMajors = sysMajorRepository.findAll();
        List<Map<String, Object>> majorStats = new ArrayList<>();
        int totalValid = validData.size();
        for (SysMajor major : allMajors) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("majorName", major.getMajorName());
            item.put("majorId", major.getId());

            // 该专业原始采集数量
            long collected = allData.stream()
                    .filter(d -> d.getMajorName() != null && d.getMajorName().contains(major.getMajorName()))
                    .count();
            // 该专业ETL有效数量
            long valid = validData.stream()
                    .filter(d -> d.getMajorName() != null && d.getMajorName().contains(major.getMajorName()))
                    .count();
            // 该专业待清洗数量
            long pending = pendingData.stream()
                    .filter(d -> d.getMajorName() != null && d.getMajorName().contains(major.getMajorName()))
                    .count();

            item.put("collected", collected);
            item.put("valid", valid);
            item.put("pending", pending);
            // 数据充足性评估
            String status;
            if (valid >= 200) {
                status = "充足";
            } else if (valid >= 50) {
                status = "一般";
            } else if (valid > 0) {
                status = "不足";
            } else {
                status = "空白";
            }
            item.put("status", status);
            item.put("validPercent", totalValid > 0 ? Math.round(valid * 100.0 / totalValid * 10) / 10.0 : 0);
            majorStats.add(item);
        }
        // 按有效数据量降序
        majorStats.sort((a, b) -> Long.compare((Long) b.get("valid"), (Long) a.get("valid")));
        result.put("majorStats", majorStats);

        // ===== 3. 城市分布统计 =====
        Map<String, Long> cityDist = validData.stream()
                .filter(d -> d.getCity() != null && !d.getCity().isEmpty())
                .collect(Collectors.groupingBy(SpiderCollectedData::getCity, Collectors.counting()));
        List<Map<String, Object>> cityStats = cityDist.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("city", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("cityStats", cityStats);

        // ===== 4. 行业分布统计 =====
        Map<String, Long> industryDist = validData.stream()
                .filter(d -> d.getIndustry() != null && !d.getIndustry().isEmpty())
                .collect(Collectors.groupingBy(SpiderCollectedData::getIndustry, Collectors.counting()));
        List<Map<String, Object>> industryStats = industryDist.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("industry", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("industryStats", industryStats);

        // ===== 5. 数据时间分布（最近7天采集量趋势） =====
        final LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Map<String, Long> dailyTrend = validData.stream()
                .filter(d -> d.getCollectTime() != null && !d.getCollectTime().isEmpty())
                .filter(d -> {
                    try { return LocalDateTime.parse(d.getCollectTime(), FMT).isAfter(sevenDaysAgo); }
                    catch (Exception e) { return false; }
                })
                .collect(Collectors.groupingBy(
                        d -> {
                            try { return LocalDateTime.parse(d.getCollectTime(), FMT).toLocalDate().toString(); }
                            catch (Exception e) { return d.getCollectTime().substring(0, 10); }
                        },
                        Collectors.counting()
                ));
        List<Map<String, Object>> trendStats = dailyTrend.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("dailyTrend", trendStats);

        // ===== 6. 学历要求分布 =====
        Map<String, Long> eduDist = validData.stream()
                .filter(d -> d.getEducation() != null && !d.getEducation().isEmpty())
                .collect(Collectors.groupingBy(SpiderCollectedData::getEducation, Collectors.counting()));
        List<Map<String, Object>> eduStats = eduDist.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("education", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("educationStats", eduStats);

        return result;
    }

    // ===== ETL 状态查询 =====
    @Override
    public Map<String, Object> getEtlStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pendingCount", collectedDataRepository.findByIsValid("0").size());    // 待清洗
        result.put("validCount", collectedDataRepository.findByIsValid("1").size());       // 有效（可推荐）
        result.put("invalidCount", collectedDataRepository.findByIsValid("2").size());     // 无效
        result.put("syncedCount", collectedDataRepository.findByIsSynced("1").size());      // 已同步至职位
        result.put("totalCount", collectedDataRepository.count());                         // 总记录数
        return result;
    }

    // ===== 一键执行 ETL + 推荐全流程 =====
    @Override
    public Map<String, Object> executeFullPipeline(Long taskId) {
        Map<String, Object> pipelineResult = new LinkedHashMap<>();
        pipelineResult.put("phase", "STARTED");

        // Phase 1: ETL 数据清洗
        Map<String, Object> etlResult;
        try {
            pipelineResult.put("phase", "ETL");
            etlResult = executeEtl(taskId);
            pipelineResult.put("etl", etlResult);

            long validCount = ((Number) etlResult.getOrDefault("validCount", 0)).longValue();
            if (validCount == 0) {
                pipelineResult.put("phase", "ETL_NO_DATA");
                pipelineResult.put("message", "ETL 清洗后无有效数据，跳过推荐");
                return pipelineResult;
            }
        } catch (Exception e) {
            log.error("Pipeline ETL 阶段异常", e);
            pipelineResult.put("phase", "ETL_FAILED");
            pipelineResult.put("etlError", e.getMessage());
            return pipelineResult;
        }

        // Phase 2: 对每个专业触发推荐
        Map<String, Object> recommendResult = new LinkedHashMap<>();
        try {
            // 获取所有开启了推荐的专业
            List<SysMajor> allMajors = sysMajorRepository.findAll();
            List<Long> enabledMajorIds = allMajors.stream()
                    .filter(m -> "1".equals(m.getRecommendEnabled()))
                    .map(SysMajor::getId)
                    .toList();

            int successCount = 0;
            int failCount = 0;
            for (Long majorId : enabledMajorIds) {
                try {
                    recommendService.triggerRecommendForMajor(majorId, 20);
                    successCount++;
                } catch (Exception e) {
                    log.warn("专业 {} 推荐触发失败: {}", majorId, e.getMessage());
                    failCount++;
                }
            }

            recommendResult.put("totalMajors", enabledMajorIds.size());
            recommendResult.put("successCount", successCount);
            recommendResult.put("failCount", failCount);
            pipelineResult.put("recommend", recommendResult);
            pipelineResult.put("phase", "COMPLETED");
            pipelineResult.put("message", "Pipeline 执行完成");

        } catch (Exception e) {
            log.error("Pipeline 推荐阶段异常", e);
            pipelineResult.put("phase", "RECOMMEND_FAILED");
            pipelineResult.put("recommendError", e.getMessage());
        }

        return pipelineResult;
    }

    // ==================== 内部工具方法 =====================

    private String buildSearchUrl(String sourceCode, String keyword, int page) {
        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported", e);
        }
        return switch (sourceCode) {
            case "51job" -> String.format(
                "https://we.51job.com/api/job/search-pc?search_token=%s&page=%d&page_size=50",
                encoded, page);
            case "gov" -> String.format(
                "http://job.mohrss.gov.cn/cjobs/jobinfolist/listJobinfolist?textfield=%s&pageNo=%d",
                encoded, page);
            case "moe" -> String.format(
                "https://bjbys.ncss.cn/student/jobs/index.html?keyword=%s&page=%d",
                encoded, page);
            default -> encoded;
        };
    }

    private String fetchPage(Long taskId, String url, String sourceCode) {
        // 初始化该任务的浏览器实例
        seleniumCrawlerService.initBrowser(taskId);
        // 所有非51job的数据源已移除，此处统一通过 Selenium 访问
        return seleniumCrawlerService.fetchPage(taskId, url);
    }

    /**
     * 使用站点专属提取器解析 HTML 中的职位数据。
     * 每个数据源对应一个独立的提取器，互不干扰，便于精确定位问题。
     */
    private List<SpiderCollectedData> parseJobListings(Long taskId, String html, String sourceCode, String major, String keyword) {
        if (html == null || html.trim().isEmpty()) {
            return new ArrayList<>();
        }

        SiteDataExtractor extractor = extractorMap.get(sourceCode);
        if (extractor == null) {
            log.error("任务 {} 未找到数据源 [{}] 对应的提取器", taskId, sourceCode);
            return new ArrayList<>();
        }

        Document doc = Jsoup.parse(html);
        log.debug("任务 {} 关键词=[{}] 数据源=[{}] HTML长度={}", taskId, keyword, sourceCode, html.length());

        List<SpiderCollectedData> result = extractor.extract(doc, sourceCode, major, keyword);

        log.info("任务 {} 关键词=[{}] 数据源=[{}] 解析到 {} 条有效数据", taskId, keyword, sourceCode, result.size());
        return result;
    }
    
    private String inferIndustry(String keyword, String jobName) {
        if (keyword != null) {
            for (Map.Entry<String, List<String>> entry : DataConstants.MAJOR_INDUSTRY_KEYWORDS.entrySet()) {
                for (String kw : entry.getValue()) {
                    if (kw.contains(keyword) || keyword.contains(kw.replace("/", ""))) {
                        return entry.getKey();
                    }
                }
            }
        }
        if (jobName != null) {
            String lowerJob = jobName.toLowerCase();
            if (lowerJob.contains("java") || lowerJob.contains("python") || lowerJob.contains("前端") || lowerJob.contains("后端")) {
                return "互联网/IT";
            }
            if (lowerJob.contains("机械") || lowerJob.contains("工艺") || lowerJob.contains("制造")) {
                return "制造业";
            }
            if (lowerJob.contains("土木") || lowerJob.contains("建筑") || lowerJob.contains("施工")) {
                return "房地产/建筑";
            }
            if (lowerJob.contains("电气") || lowerJob.contains("自动化") || lowerJob.contains("控制")) {
                return "电子/自动化";
            }
        }
        return "其他";
    }

    private boolean isValidData(SpiderCollectedData data) {
        // 规则1：职位名称为空或过短
        if (data.getJobName() == null || data.getJobName().trim().isEmpty()) return false;
        if (data.getJobName().trim().length() < 2) return false;

        // 规则2：薪资字段为空（最容易出现）
        if (data.getSalary() == null || data.getSalary().trim().isEmpty()) return false;

        // 规则3：薪资字段超长（异常数据）
        if (data.getSalary() != null && data.getSalary().length() > 50) return false;

        // 规则4：公司名称不完整（过短）
        if (data.getCompanyName() == null || data.getCompanyName().trim().isEmpty()) return false;
        if (data.getCompanyName().trim().length() < 2) return false;

        // 规则5：原始数据包含验证码/反爬拦截页面
        String raw = data.getRawData();
        if (raw != null) {
            String rawLower = raw.toLowerCase();
            if (rawLower.contains("验证") || rawLower.contains("验证码") || rawLower.contains("反爬")
                    || rawLower.contains("captcha") || rawLower.contains("robot")
                    || rawLower.contains("access denied") || rawLower.contains("禁止访问")) {
                return false;
            }
        }

        // 规则6：薪资字段包含明显非薪资内容（如长段落描述）
        if (data.getSalary() != null && data.getSalary().length() > 30) {
            // 检查是否真的是薪资（如 8k~15k、8000-12000元/月 等）
            String sal = data.getSalary().trim();
            boolean looksLikeSalary = sal.matches(".*\\d+.*[kK万千~\\-—].*") || sal.matches(".*\\d{3,5}.*");
            if (!looksLikeSalary) return false;
        }

        return true;
    }

    private void normalizeData(SpiderCollectedData data) {
        if (data.getJobName() != null) {
            data.setJobName(data.getJobName().replaceAll("\\s+", " ").trim());
        }
        if (data.getSalary() != null) {
            String sal = data.getSalary().replaceAll("[^0-9kK万月年]", "");
            if (!sal.isEmpty()) {
                data.setSalary(data.getSalary().replaceAll("\\s+", " ").trim());
            }
        }
        if (data.getCity() != null) {
            String city = data.getCity().replaceAll("\\s+", "").trim();
            data.setCity(city);
        }
    }

    /**
     * 构建去重Key：职位名称+公司名+薪资+关联专业，4字段相同视为重复
     * 去掉城市（同一职位不同城市仍算不同机会），保留薪资（薪资差异应视为不同岗位）
     */
    private String buildDedupKey(String jobName, String companyName,
                                 String salary, String city, String industryKeyword) {
        if (jobName == null && companyName == null) return null;
        String j = jobName != null ? jobName.trim().toLowerCase() : "";
        String c = companyName != null ? companyName.trim().toLowerCase() : "";
        String s = salary != null ? salary.trim().toLowerCase() : "";
        String ik = industryKeyword != null ? industryKeyword.trim().toLowerCase() : "";
        return j + "|" + c + "|" + s + "|" + ik;
    }

    private List<String> getAllMajorNames() {
        return new ArrayList<>(DataConstants.MAJOR_INDUSTRY_KEYWORDS.keySet());
    }

    private List<String> getAllIndustryKeywords() {
        Set<String> keywords = new LinkedHashSet<>();
        for (List<String> kws : DataConstants.MAJOR_INDUSTRY_KEYWORDS.values()) {
            keywords.addAll(kws);
        }
        return new ArrayList<>(keywords);
    }

    private List<String> getKeywordsForMajor(String major) {
        List<String> kws = DataConstants.MAJOR_INDUSTRY_KEYWORDS.get(major);
        return kws != null ? kws : Collections.singletonList(major);
    }

    private CrawlerJobRecord createJobRecord(Long taskId, String sourceCode, String major, String keyword, String url, int page) {
        CrawlerJobRecord record = new CrawlerJobRecord();
        record.setTaskId(taskId);
        record.setSourceCode(sourceCode);
        record.setMajorName(major);
        record.setIndustryKeyword(keyword);
        record.setSearchUrl(url);
        record.setPageNum(page);
        record.setStatus("pending");
        record.setIsComplete("0");
        record.setStartedTime(LocalDateTime.now().format(FMT));
        return jobRecordRepository.save(record);
    }

    private int calculateProgress(int currentPage, int totalPages, int currentMajorIdx, int totalMajors) {
        if (totalMajors <= 0 || totalPages <= 0) return 0;
        // 确保 majorIdx 不超出边界
        if (currentMajorIdx < 0) currentMajorIdx = 0;
        if (currentMajorIdx >= totalMajors) return 100;
        // 当前专业在整个任务中的权重：(已完成专业数 + 当前页占比) / 总专业数
        double majorProgress = (double) (currentMajorIdx * totalPages + currentPage) / (totalMajors * totalPages);
        return (int) Math.min(majorProgress * 100, 100);
    }

    private void sleepRandom(int minMs, int maxMs) {
        try {
            Thread.sleep((long) (minMs + Math.random() * (maxMs - minMs)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void persistProgressToRedis(Long taskId, int progress, int collectedCount, int total) {
        persistProgressToRedis(taskId, progress, collectedCount, total, null, null, 0, 0, 0, 0, 0, 0, 0);
    }

    private void persistProgressToRedis(Long taskId, int progress, int collectedCount, int total,
                                         String currentMajor, String currentKeyword, int currentPage,
                                         int totalKeywords, int maxPages,
                                         int majorIndex, int totalMajors, int keywordIndex, int majorKeywordsCount) {
        try {
            String key = REDIS_CRAWL_PREFIX + taskId;
            Map<String, Object> data = new HashMap<>();
            data.put("progress", progress);
            data.put("collectedCount", collectedCount);
            data.put("total", total);
            data.put("timestamp", System.currentTimeMillis());
            // 详细进度信息
            data.put("currentMajor", currentMajor != null ? currentMajor : "");
            data.put("currentKeyword", currentKeyword != null ? currentKeyword : "");
            data.put("currentPage", currentPage);
            data.put("totalKeywords", totalKeywords);
            data.put("maxPages", maxPages);
            data.put("majorIndex", majorIndex);
            data.put("totalMajors", totalMajors);
            data.put("keywordIndex", keywordIndex);
            data.put("majorKeywordsCount", majorKeywordsCount);
            redisTemplate.opsForHash().putAll(key, data);
            redisTemplate.expire(key, Duration.ofHours(2));
        } catch (Exception e) {
            log.debug("Redis写入进度失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> getProgressFromRedis(Long taskId) {
        try {
            String key = REDIS_CRAWL_PREFIX + taskId;
            Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
            if (raw.isEmpty()) return Collections.emptyMap();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskId", taskId);
            result.put("progress", raw.get("progress"));
            result.put("collectedCount", raw.get("collectedCount"));
            result.put("total", raw.get("total"));
            result.put("isRunning", isTaskRunning(taskId));
            // 详细进度信息
            result.put("currentMajor", raw.get("currentMajor") != null ? raw.get("currentMajor") : "");
            result.put("currentKeyword", raw.get("currentKeyword") != null ? raw.get("currentKeyword") : "");
            result.put("currentPage", raw.get("currentPage") != null ? raw.get("currentPage") : 0);
            result.put("totalKeywords", raw.get("totalKeywords") != null ? raw.get("totalKeywords") : 0);
            result.put("maxPages", raw.get("maxPages") != null ? raw.get("maxPages") : 0);
            result.put("majorIndex", raw.get("majorIndex") != null ? raw.get("majorIndex") : 0);
            result.put("totalMajors", raw.get("totalMajors") != null ? raw.get("totalMajors") : 0);
            result.put("keywordIndex", raw.get("keywordIndex") != null ? raw.get("keywordIndex") : 0);
            result.put("majorKeywordsCount", raw.get("majorKeywordsCount") != null ? raw.get("majorKeywordsCount") : 0);
            return result;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
