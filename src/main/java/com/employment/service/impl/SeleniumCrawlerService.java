package com.employment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import reactor.util.retry.Retry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Selenium + Chrome Headless 的浏览器自动化爬虫服务。
 * 绕过所有 JS 渲染和反爬拦截，返回真实页面内容。
 */
@Slf4j
@Service
public class SeleniumCrawlerService {

    /** 每个任务 ID 独享一个 WebDriver 实例，避免跨任务干扰 */
    private static final Map<Long, WebDriver> TASK_DRIVER_MAP = new ConcurrentHashMap<>();

    /** 默认等待页面稳定的时间（秒） */
    private static final int DEFAULT_WAIT_SECONDS = 8;

    private static final Random RANDOM = new Random();

    /** 随机 User-Agent 池 */
    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0"
    );

    /**
     * 为指定任务初始化 Chrome Headless 浏览器。
     * 同一任务 ID 不会重复创建浏览器。
     */
    public void initBrowser(Long taskId) {
        if (TASK_DRIVER_MAP.containsKey(taskId)) {
            log.debug("任务 {} 浏览器已存在，跳过初始化", taskId);
            return;
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = buildChromeOptions();

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().window().setSize(new Dimension(1920, 1080));

        // 浏览器启动后立即注入反检测脚本，后续所有页面自动生效
        try {
            injectStealthViaCdp(driver);
        } catch (Exception e) {
            log.debug("浏览器启动时CDP注入失败: {}", e.getMessage());
        }

        TASK_DRIVER_MAP.put(taskId, driver);
        log.info("任务 {} Chrome 浏览器已启动 (headless)", taskId);
    }

    /**
     * 通过 CDP 在浏览器级别注入 stealth 脚本（一次注入，所有页面生效）
     */
    private void injectStealthViaCdp(WebDriver driver) {
        Map<String, Object> stealthScript = new HashMap<>();
        stealthScript.put("source",
            "Object.defineProperty(navigator, 'webdriver', {get: () => undefined, configurable: true, enumerable: true});" +
            "Object.defineProperty(navigator, 'chrome', {get: () => ({runtime: {}}), configurable: true, enumerable: true});" +
            "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5], configurable: true, enumerable: true});" +
            "Object.defineProperty(navigator, 'languages', {get: () => new Array('zh-CN', 'zh', 'en'), configurable: true, enumerable: true});" +
            "const _origQuery = navigator.permissions.query;" +
            "navigator.permissions.query = (parameters) => (" +
            "  parameters.name === 'notifications' ? Promise.resolve({ state: Notification.permission }) : _origQuery(parameters)" +
            ");" +
            "window.chrome = window.chrome || {};" +
            "window.chrome.runtime = window.chrome.runtime || {};" +
            "window.chrome.runtime.connect = function(){return {id:1,postMessage:function(){},onMessage:{addListener:function(){}}}};" +
            "window.chrome.runtime.connectLocal = function(){return {id:1,postMessage:function(){},onMessage:{addListener:function(){}}}};" +
            "HTMLCanvasElement.prototype.getContext = (function(old) {" +
            "  return function(type, attrs) {" +
            "    var ctx = old.call(this, type, attrs);" +
            "    if (ctx && (type === 'webgl' || type === 'webgl2')) {" +
            "      var _getParameter = ctx.getParameter.bind(ctx);" +
            "      ctx.getParameter = function(p) {" +
            "        if (p === 37445) return 'Intel Inc.';" +
            "        if (p === 37446) return 'Intel Iris OpenGL Engine';" +
            "        return _getParameter(p);" +
            "      };" +
            "    }" +
            "    return ctx;" +
            "  };" +
            "})(HTMLCanvasElement.prototype.getContext);"
        );
        // Selenium 4.x 中 executeCdpCommand 在 ChromeDriver 上，需要强转
        if (driver instanceof ChromeDriver) {
            ((ChromeDriver) driver).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", stealthScript);
            log.debug("CDP stealth 脚本已注入浏览器");
        } else {
            log.debug("当前 driver 不支持 CDP，使用 JS 注入降级");
            injectAntiDetectJSFallback(driver);
        }
    }

    // ===================== 政府公共就业平台爬取（无反爬） =====================

    /**
     * 中国公共招聘网（job.mohrss.gov.cn）- 无反爬，数据真实权威
     * 正确路径：/cjobs/jobinfolist/listJobinfolist?textfield=关键词&pageNo=1
     * 注意：必须用 HTTP，HTTPS 会超时
     */
    public String fetchPageGov(Long taskId, String keyword, int page) {
            initBrowser(taskId);
        WebDriver driver = TASK_DRIVER_MAP.get(taskId);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                // 中国公共招聘网正确搜索接口（SearchAPI 已下线，改用主站搜索路径）
                String encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
                String url = String.format(
                    "http://job.mohrss.gov.cn/cjobs/jobinfolist/listJobinfolist?textfield=%s&pageNo=%d",
                    encoded, page);

                log.debug("任务 {} 中国公共招聘网 第{}次尝试，关键词={}，页码={}", taskId, attempt, keyword, page);
                driver.get(url);

                injectAntiDetectJS(driver);
                sleep(3000 + RANDOM.nextInt(2000));

                String html = driver.getPageSource();
                if (isBlockedPage(html, url)) {
                    log.warn("任务 {} 中国公共招聘网 第{}次被拦截，换用备用方式...", taskId, attempt);
                sleep(3000);
                    continue;
                }

                log.info("任务 {} 中国公共招聘网 爬取成功 page={}, length={}", taskId, page, html.length());
                return html;

            } catch (Exception e) {
                log.warn("任务 {} 中国公共招聘网 第{}次异常: {}", taskId, attempt, e.getMessage());
                sleep(2000);
            }
        }

        return null;
    }

    /**
     * 国家大学生就业服务平台（bjbys.ncss.cn）- 教育部官方平台
     * 注意：bysjy.moe.edu.cn 已停用，现已迁移到 bjbys.ncss.cn
     */
    public String fetchPageEducation(Long taskId, String keyword, int page) {
        initBrowser(taskId);
        WebDriver driver = TASK_DRIVER_MAP.get(taskId);

        try {
            String encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
            String url = String.format(
                "https://bjbys.ncss.cn/student/jobs/index.html?keyword=%s&page=%d",
                encoded, page);

            log.debug("任务 {} 国家大学生就业平台 关键词={}，页码={}", taskId, keyword, page);
            driver.get(url);

            injectAntiDetectJS(driver);
            sleep(4000 + RANDOM.nextInt(2000));

            String html = driver.getPageSource();
            log.info("任务 {} 国家大学生就业平台 爬取成功 page={}, length={}", taskId, page, html.length());
            return html;

        } catch (Exception e) {
            log.warn("任务 {} 国家大学生就业平台爬取异常: {}", taskId, e.getMessage());
            return null;
        }
    }

    /**
     * 应届生求职网（yingjiesheng.com）爬取方法
     * 特点：爬虫友好，普通HTTP请求即可获取数据
     * URL: https://s.yingjiesheng.com/search.php?word={关键词}&start={偏移量}
     * 分页：每页20条，start=0,20,40,60...
     */
    public String fetchPageYingjiesheng(Long taskId, String keyword, int page) {
        initBrowser(taskId);
        WebDriver driver = TASK_DRIVER_MAP.get(taskId);

        // 确保已登录（首次手动登录后 Cookie 自动持久化）
        ensureYingjieshengLogin(taskId);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
                // 新版 q.yingjiesheng.com/jobs/search/{keyword}
                String url = "https://q.yingjiesheng.com/jobs/search/" + encoded;

                log.debug("任务 {} 应届生求职网 第{}次尝试，关键词={}，页码={}", taskId, attempt, keyword, page);
                driver.get(url);

                injectAntiDetectJS(driver);
                sleep(2000 + RANDOM.nextInt(1000));

                String html = driver.getPageSource();
                if (isBlockedPage(html, url)) {
                    log.warn("任务 {} 应届生求职网 第{}次被拦截，换用备用方式...", taskId, attempt);
                    sleep(3000);
                    continue;
                }

                log.info("任务 {} 应届生求职网 爬取成功 page={}, length={}", taskId, page, html.length());
                return html;

            } catch (Exception e) {
                log.warn("任务 {} 应届生求职网 第{}次异常: {}", taskId, attempt, e.getMessage());
                sleep(2000);
            }
        }

        return null;
    }

    /**
     * 应届生求职网 HTTP 直接请求（无Selenium，更快）
     */
    public String fetchPageYingjieshengViaHttp(String keyword, int page) {
        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported", e);
        }

        try {
            WebClient webClient = WebClient.builder()
                .baseUrl("https://q.yingjiesheng.com")
                .defaultHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .defaultHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .defaultHeader("Referer", "https://q.yingjiesheng.com/")
                .build();

            // 新版 q.yingjiesheng.com/jobs/search/{keyword}
            String response = webClient.get()
                .uri("/jobs/search/{keyword}", encoded)
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                    .filter(this::isRetryableHttpError))
                .block(Duration.ofSeconds(30));

            log.debug("应届生求职网(q) HTTP 返回长度={}, page={}", response != null ? response.length() : 0, page);
            return response;

        } catch (WebClientResponseException wce) {
            log.warn("应届生求职网 HTTP 请求失败 status={}: {}", wce.getStatusCode(), wce.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("应届生求职网 HTTP 请求异常: {}", e.getMessage());
            return null;
        }
    }

    // 前程无忧 专用爬取方式 end

    /**
     * 爬取 yingjiesheng.com 详情页。
     * 使用现有浏览器实例，直接导航到详情页 URL。
     */
    public String fetchYingjieshengDetail(Long taskId, String detailUrl) {
        WebDriver driver = TASK_DRIVER_MAP.get(taskId);
        if (driver == null) {
            log.warn("任务 {} 浏览器未初始化，先执行 initBrowser", taskId);
            initBrowser(taskId);
            driver = TASK_DRIVER_MAP.get(taskId);
        }

        // 确保已登录（首次手动登录后 Cookie 自动持久化）
        ensureYingjieshengLogin(taskId);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                driver.get(detailUrl);

                // 等待页面完全加载（与 fetchPage 保持一致）
                waitForPageReady(driver);
                injectAntiDetectJS(driver);

                // 滚动页面触发懒加载内容
                scrollToLoad(taskId, 3);
                sleep(800 + RANDOM.nextInt(500));

                String html = driver.getPageSource();
                String currentUrl = driver.getCurrentUrl();
                if (isBlockedPage(driver, html, currentUrl)) {
                    // 如果是滑块验证页，截图保存并提示用户手动操作
                    if (html.contains("别离开") && html.contains("进行验证")) {
                        byte[] screenshot = takeScreenshot(taskId);
                        if (screenshot != null) {
                            java.nio.file.Files.write(
                                    java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"),
                                            "waf_captcha_task" + taskId + "_attempt" + attempt + ".png"),
                                    screenshot);
                            log.warn("任务 {} 遇到阿里云滑块验证页！请切换到 Chrome 窗口手动完成验证，完成后爬虫将自动继续。截图已保存: waf_captcha_task{}_attempt{}.png",
                                    taskId, taskId, attempt);
                        } else {
                            log.warn("任务 {} 遇到阿里云滑块验证页，请手动在 Chrome 窗口中完成验证", taskId);
                        }
                        // 等待用户在浏览器中完成验证（最多等5分钟，每5秒检查一次）
                        waitForVerificationDone(driver, taskId, detailUrl);
                    } else {
                        log.warn("任务 {} 详情页第{}次被拦截: {}", taskId, attempt, detailUrl);
                        sleep(3000 + RANDOM.nextInt(2000));
                    }
                    continue;
                }

                log.debug("任务 {} 详情页爬取成功 url={}, length={}", taskId, detailUrl, html.length());
                return html;

            } catch (Exception e) {
                log.warn("任务 {} 详情页第{}次异常: {}", taskId, attempt, e.getMessage());
                sleep(2000);
            }
        }
        log.warn("任务 {} 详情页多次被拦截，返回 null: {}", taskId, detailUrl);
        return null;
    }

    public String fetchPage51job(Long taskId, String keyword, int page) {
        WebDriver driver = TASK_DRIVER_MAP.get(taskId);
        if (driver == null) {
            log.warn("任务 {} 浏览器未初始化，先执行 initBrowser", taskId);
            initBrowser(taskId);
            driver = TASK_DRIVER_MAP.get(taskId);
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                log.debug("任务 {} fetchPage51job 第{}次尝试，关键词={}，页码={}", taskId, attempt, keyword, page);

                // 尝试两种 URL：1) 搜索结果页（JS渲染）2) API直链
                String[] urls = {
                    build51jobSearchUrl(keyword, page),  // 搜索结果页
                    "https://we.51job.com/api/job/search-pc?search_token=" +
                        java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) +
                        "&page=" + page + "&page_size=50"
                };

                String html = null;
                for (String searchUrl : urls) {
                    log.debug("任务 {} 尝试 URL: {}", taskId, searchUrl);
                    driver.get(searchUrl);

                    // 注入反检测脚本（每次页面加载后）
                    injectAntiDetectJS(driver);

                    // 等待页面基本加载
                waitForPageReady(driver);
                    sleep(2000 + RANDOM.nextInt(1000));

                    // 模拟真人滚动操作（触发懒加载）
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    for (int scroll = 0; scroll < 3; scroll++) {
                        js.executeScript("window.scrollBy(0, 300);");
                        sleep(500 + RANDOM.nextInt(500));
                        js.executeScript("window.scrollBy(0, -100);");
                        sleep(300 + RANDOM.nextInt(300));
                    }

                    // 额外等待 JS 完全渲染
                    sleep(3000 + RANDOM.nextInt(2000));

                    html = driver.getPageSource();
                    String currentUrl = driver.getCurrentUrl();

                    // 如果被拦截，跳过这个 URL
                    if (isBlockedPage(driver, html, currentUrl)) {
                        log.warn("任务 {} URL被拦截，尝试下一个: {}", taskId, searchUrl);
                        closeAlertIfPresent(driver);
                        sleep(2000 + RANDOM.nextInt(1000));
                        continue;
                    }

                    // 检查是否是 WAF 拦截页
                    if (html != null && (html.contains("aliyun_waf") || html.length() < 5000)) {
                        log.warn("任务 {} 收到 WAF 拦截页，尝试下一个 URL...", taskId);
                        continue;
                    }

                    // 成功获取有效页面
                    if (html != null && html.length() >= 5000) {
                        log.info("任务 {} 51job 爬取成功 URL={}, length={}", taskId, searchUrl, html.length());
                        return html;
                    }
                }

                // 所有 URL 都失败了
                log.warn("任务 {} 51job 第{}次尝试全部 URL 失败", taskId, attempt);
                if (attempt < 3) sleep(5000 + RANDOM.nextInt(3000));

            } catch (Exception e) {
                log.warn("任务 {} 51job 抓取异常第{}次: {}", taskId, attempt, e.getMessage());
                if (attempt >= 3) return null;
                sleep(3000);
            }
        }
        return null;
    }

    // ===================== 51job WebClient 直连方案（绕过 Selenium 反爬） =====================

    /**
     * 通过 Spring WebClient 直连 51job JSON API，返回原始 JSON 字符串。
     * 相比 Selenium 方案：无需浏览器，速度快，不触发反爬检测。
     */
    public String fetchPage51jobViaHttp(String keyword, int page) {
        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported", e);
        }

        try {
            WebClient webClient = WebClient.builder()
                .baseUrl("https://we.51job.com")
                .defaultHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "application/json, text/plain, */*")
                .defaultHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .defaultHeader("Referer", "https://we.51job.com/pc/search?jobarea=000000%2C00&searchkeyword=" + encoded)
                .defaultHeader("Origin", "https://we.51job.com")
                .build();

            String response = webClient.get()
                .uri("/api/job/search-pc?search_token={token}&page={page}&page_size=50", encoded, page)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                    .filter(this::isRetryableHttpError))
                .block(Duration.ofSeconds(30));

            log.debug("51job HTTP API 返回长度={}, page={}", response != null ? response.length() : 0, page);
            return response;

        } catch (WebClientResponseException wce) {
            log.warn("51job HTTP 请求失败 status={}: {}", wce.getStatusCode(), wce.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("51job HTTP 请求异常: {}", e.getMessage());
            return null;
        }
    }

    private boolean isRetryableHttpError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wce) {
            int status = wce.getStatusCode().value();
            return status == 429 || status == 502 || status == 503 || status == 504;
        }
        return false;
    }

    /**
     * 将 51job JSON API 返回的字符串转为兼容的 HTML 结构，
     * 方便现有 parseJobListings 方法复用相同的 JSoup 解析逻辑。
     */
    public String convert51jobJsonToHtml(String jsonResponse, String keyword) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return "";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            JsonNode jobArray;
            if (root.has("resultbody") && root.get("resultbody").has("job")) {
                jobArray = root.get("resultbody").get("job");
            } else if (root.has("job")) {
                jobArray = root.get("job");
            } else if (root.has("data") && root.get("data").has("job")) {
                jobArray = root.get("data").get("job");
            } else {
                if (root.isArray()) {
                    jobArray = root;
                } else {
                    log.warn("51job JSON 结构未知，无法定位职位数组");
                    return "";
                }
            }

            if (!jobArray.isArray()) {
                log.warn("51job 职位数据不是数组格式");
                return "";
            }

            StringBuilder html = new StringBuilder();
            html.append("<div id='joblist'>");

            for (JsonNode job : jobArray) {
                String jobName = safeText(job, "job_name", "jobname", "title");
                String companyName = safeText(job, "company_name", "companyname", "company");
                String salary = safeText(job, "providesalary_text", "salary", "salary_text");
                String city = safeText(job, "work_area", "workarea", "city");
                String experience = safeText(job, "workyear_text", "workyear", "experience");
                String education = safeText(job, "degreefrom_text", "degreefrom", "education", "degree_from");
                String companyScale = safeText(job, "companysize_text", "companysize", "scale", "company_size");
                String companyType = safeText(job, "companytype_text", "companytype", "industry_type");
                String jobHref = safeText(job, "job_href", "url", "link");
                String companyHref = safeText(job, "company_href", "company_url", "company_link");
                String publishTime = safeText(job, "updatedate", "updatedate_text", "pub_date", "publish_time");
                String industry = safeText(job, "industry", "industry_type");

                html.append("<div class='j_joblist'>");
                html.append("<div class='joblist-box'>");
                if (jobName != null) {
                    html.append("<a class='t1 job-title'>");
                    if (jobHref != null) html.append("' href='").append(escapeHtml(jobHref));
                    html.append("'>").append(escapeHtml(jobName)).append("</a>");
                }
                if (companyName != null) {
                    html.append("<a class='t2 company'>");
                    if (companyHref != null) html.append("' href='").append(escapeHtml(companyHref));
                    html.append("'>").append(escapeHtml(companyName)).append("</a>");
                }
                if (salary != null) html.append("<span class='t4 salary'>").append(escapeHtml(salary)).append("</span>");
                if (city != null) html.append("<span class='t3 city'>").append(escapeHtml(city)).append("</span>");
                if (experience != null || education != null) {
                    html.append("<div class='d'>");
                    if (experience != null) html.append(escapeHtml(experience));
                    if (experience != null && education != null) html.append(" | ");
                    if (education != null) html.append(escapeHtml(education));
                    html.append("</div>");
                }
                if (companyScale != null) html.append("<span class='sspan'>").append(escapeHtml(companyScale)).append("</span>");
                if (companyType != null) html.append("<span class='company-type'>").append(escapeHtml(companyType)).append("</span>");
                if (publishTime != null) html.append("<span class='time'>").append(escapeHtml(publishTime)).append("</span>");
                if (industry != null) html.append("<input type='hidden' class='industry' value='").append(escapeHtml(industry)).append("'/>");
                html.append("</div>");
                html.append("</div>\n");
            }

            html.append("</div>");
            return html.toString();

        } catch (Exception e) {
            log.error("解析 51job JSON 失败: {}", e.getMessage());
            return "";
        }
    }

    private String safeText(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode n = node.path(name);
            if (!n.isMissingNode() && !n.isNull()) {
                String text = n.asText();
                if (text != null && !text.trim().isEmpty()) return text.trim();
            }
        }
        return null;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    /**
     * 将 51job 首页的城市选择器设置为"全国"（避免只爬到本地城市数据）
     */
    private void selectAllChina(WebDriver driver) {
        try {
            // 尝试多种方式找到城市选择器
            String[] citySelectors = {
                ".ewm-city .ti, .top_city, #work_position_input",
                "span[class*='city']", "input[class*='city']",
                ".dw_city", "#jobarea", ".job-area"
            };
            for (String selector : citySelectors) {
                try {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    for (WebElement el : elements) {
                        if (el.isDisplayed() && el.isEnabled()) {
                            // 点击展开城市选择
                            el.click();
                            sleep(1000);
                            log.debug("任务 {} 已点击城市选择器: {}", getCurrentTaskId(driver), selector);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 在展开的城市列表中找"全国"或"全部"选项
            sleep(800);
            String[] allChinaSelectors = {
                "li[data-id='000000'",     // 全国城市代码
                "span:contains('全国')",
                "a:contains('全国')",
                "li:contains('全国')",
                "[title='全国']",
                "//li[contains(text(),'全国')]"  // XPath
            };
            for (String sel : allChinaSelectors) {
                try {
                    List<WebElement> items;
                    if (sel.startsWith("//")) {
                        items = driver.findElements(By.xpath(sel));
                    } else {
                        items = driver.findElements(By.cssSelector(sel));
                    }
                    for (WebElement item : items) {
                        if (item.isDisplayed()) {
                            item.click();
                            log.debug("任务 {} 已选择全国", getCurrentTaskId(driver));
                            sleep(500);
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            }
            log.debug("任务 {} 未找到全国城市选项，可能已是全国或需要其他交互", getCurrentTaskId(driver));
            } catch (Exception e) {
            log.debug("选择全国城市失败: {}", e.getMessage());
            }
    }

    private Long getCurrentTaskId(WebDriver driver) {
        for (Map.Entry<Long, WebDriver> entry : TASK_DRIVER_MAP.entrySet()) {
            if (entry.getValue().equals(driver)) return entry.getKey();
        }
        return null;
    }

    private String build51jobSearchUrl(String keyword, int page) {
        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported", e);
        }
        return String.format("https://search.51job.com/list/000000,000000,0000,00,9,99,%s,2,%d.html", encoded, page);
    }

    /**
     * 访问指定 URL，等待页面 JS 渲染完成，返回完整 HTML。
     * 自动重试 2 次，遇到拦截页面自动关闭并重试。
     */
    public String fetchPage(Long taskId, String url) {
        WebDriver driver = TASK_DRIVER_MAP.get(taskId);
        if (driver == null) {
            log.warn("任务 {} 浏览器未初始化，先执行 initBrowser", taskId);
            initBrowser(taskId);
            driver = TASK_DRIVER_MAP.get(taskId);
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                driver.get(url);
                waitForPageReady(driver);

                // 注入 JS 隐藏 Selenium 自动化特征
                injectAntiDetectJS(driver);

                String html = driver.getPageSource();
                String currentUrl = driver.getCurrentUrl();

                // 检测是否被反爬拦截
                if (isBlockedPage(driver, html, currentUrl)) {
                    log.warn("任务 {} 访问 {} 被拦截（第{}次尝试），关闭并重试", taskId, url, attempt);
                    closeAlertIfPresent(driver);
                    if (attempt < 3) {
                        sleep(2000);
                    }
                    continue;
                }

                log.debug("任务 {} 抓取成功: {} (length={})", taskId, url, html.length());
                return html;

            } catch (WebDriverException e) {
                log.warn("任务 {} 抓取异常 [{}] 第{}次: {}", taskId, url, attempt, e.getMessage());
                if (attempt >= 3) {
                    return null;
                }
                sleep(3000);
            }
        }

        return null;
    }

    /**
     * 执行滚动加载（对付懒加载列表），滚动到底部等待数据加载。
     */
    public void scrollToLoad(Long taskId, int scrollCount) {
        WebDriver driver = TASK_DRIVER_MAP.get(taskId);
        if (driver == null) return;

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (int i = 0; i < scrollCount; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                sleep(800 + RANDOM.nextInt(400));
                js.executeScript("window.scrollTo(0, document.body.scrollHeight / 2);");
                sleep(400 + RANDOM.nextInt(200));
            }
            log.debug("任务 {} 滚动{}次完成", taskId, scrollCount);
        } catch (Exception e) {
            log.debug("任务 {} 滚动加载失败: {}", taskId, e.getMessage());
        }
    }

    /**
     * 截图（用于调试观察拦截页面）
     */
    public byte[] takeScreenshot(Long taskId) {
        WebDriver driver = TASK_DRIVER_MAP.get(taskId);
        if (driver == null) return null;
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 关闭任务对应的浏览器，释放资源。
     */
    public void closeBrowser(Long taskId) {
        WebDriver driver = TASK_DRIVER_MAP.remove(taskId);
        if (driver != null) {
            try {
                driver.quit();
                log.info("任务 {} Chrome 浏览器已关闭", taskId);
            } catch (Exception e) {
                log.debug("任务 {} 关闭浏览器异常: {}", taskId, e.getMessage());
            }
        }
    }

    /** 关闭所有浏览器（服务销毁时调用） */
    public void closeAllBrowsers() {
        for (Map.Entry<Long, WebDriver> entry : new HashMap<>(TASK_DRIVER_MAP).entrySet()) {
            try {
                entry.getValue().quit();
            } catch (Exception ignored) {}
        }
        TASK_DRIVER_MAP.clear();
        log.info("所有 Chrome 浏览器已关闭");
    }

    // ===================== 私有工具方法 =====================

    private ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        // 使用有界面 Chrome（非 headless），大幅降低被 WAF 拦截的概率
        // 服务器环境建议改为 --headless=new，但成功率会降低
        // options.addArguments("--headless=new");

        // 常规浏览器尺寸和状态
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");

        // 移除 --disable-gpu 和 --no-sandbox，让 Chrome 表现得更像普通用户
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        // 禁用自动化控制特征标记
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-browser-infobars");
        options.addArguments("--disable-setuid-sandbox");

        // 无痕模式（隔离用户配置）
        options.addArguments("--incognito");

        // 禁用 webdriver 相关标记
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        // 随机化 User-Agent
        options.addArguments("--user-agent=" + USER_AGENTS.get(RANDOM.nextInt(USER_AGENTS.size())));

        // 禁用 logging 减少特征暴露
        options.addArguments("--disable-logging");
        options.addArguments("--log-level=3");

        // 浏览器偏好设置
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        // 语言设置
        options.addArguments("--lang=zh-CN");

        return options;
    }

    private void waitForPageReady(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
            wait.withMessage("等待页面加载超时");

            // 等待 document.readyState 变为 complete
            wait.until(webDriver -> {
                JavascriptExecutor js = (JavascriptExecutor) webDriver;
                String state = (String) js.executeScript("return document.readyState");
                return "complete".equals(state);
            });

            // 额外等待一点时间让 AJAX 数据渲染
            sleep(1500 + RANDOM.nextInt(1000));

        } catch (TimeoutException e) {
            log.debug("页面加载等待超时，继续处理");
        }
    }

    private boolean isBlockedPage(String html, String url) {
        if (html == null || html.length() < 200) return true;
        String lowerHtml = html.toLowerCase();
        // 明确的安全验证/拦截特征
        if (lowerHtml.contains("aliyun_waf") || lowerHtml.contains("_waf_is_mob")) return true;
        if (lowerHtml.contains("access denied") || lowerHtml.contains("403 forbidden")) return true;
        if (lowerHtml.contains("安全验证") || lowerHtml.contains("人机验证")) return true;
        if (lowerHtml.contains("captcha") && lowerHtml.contains("验证")) return true;
        // 阿里云滑块验证页特征（内容不少，关键词判断）
        if (html.contains("别离开") && html.contains("进行验证")) return true;
        if (html.contains("TraceID") && html.contains("aliyun")) return true;
        if (html.contains("waf") && html.contains("拖拽")) return true;
        return false;
    }

    private boolean isBlockedPage(WebDriver driver, String html, String currentUrl) {
        if (html == null || html.length() < 200) return true;

        String lowerHtml = html.toLowerCase();
        String lowerUrl = currentUrl.toLowerCase();

        // URL 出现明确的拦截跳转特征
        if (lowerUrl.contains("captcha") || lowerUrl.contains("verify")
                || lowerUrl.contains("challenge") || lowerUrl.contains("blocked")) {
            return true;
        }

        // 阿里云 WAF 拦截页面特征
        if (lowerHtml.contains("aliyun_waf")) return true;
        if (lowerHtml.contains("_waf_is_mob")) return true;
        if (lowerHtml.contains("wait.aliyun")) return true;
        // 阿里云滑块验证页特征（内容不少，靠关键词判断）
        if (html.contains("别离开") && html.contains("进行验证")) return true;
        if (html.contains("TraceID") && lowerHtml.contains("aliyun")) return true;
        if (lowerHtml.contains("waf") && html.contains("拖拽")) return true;

        // 明确的拦截/拒绝特征
        if (lowerHtml.contains("access denied") || lowerHtml.contains("403 forbidden")) return true;
        if (lowerHtml.contains("安全验证") || lowerHtml.contains("人机验证")) return true;
        if (lowerHtml.contains("captcha")) return true;

        // 内容极少的页面（可能是加载失败或空拦截页）
        if (html.length() < 5000) return true;

        return false;
    }

    private void closeAlertIfPresent(WebDriver driver) {
        try {
            driver.switchTo().alert().dismiss();
        } catch (WebDriverException ignored) {}
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 使用 CDP (Chrome DevTools Protocol) 在页面加载前注入完整的反检测脚本，
     * 等效于 puppeteer-extra-plugin-stealth 插件的效果
     */
    private void injectAntiDetectJS(WebDriver driver) {
        try {
            Map<String, Object> stealthScript = new HashMap<>();
            stealthScript.put("source",
                // ===== 基础反 Selenium 检测 =====
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined, configurable: true, enumerable: true});" +
                "Object.defineProperty(navigator, 'chrome', {get: () => ({runtime: {}}), configurable: true, enumerable: true});" +
                "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5], configurable: true, enumerable: true});" +
                "Object.defineProperty(navigator, 'languages', {get: () => new Array('zh-CN', 'zh', 'en'), configurable: true, enumerable: true});" +

                // ===== 伪装 permissions API =====
                "const _origQuery = navigator.permissions.query;" +
                "navigator.permissions.query = (parameters) => (" +
                "  parameters.name === 'notifications' ? Promise.resolve({ state: Notification.permission }) : _origQuery(parameters)" +
                ");" +

                // ===== 伪装 Chrome runtime（阻止 WAF 检测 Chrome 对象） =====
                "window.chrome = window.chrome || {};" +
                "window.chrome.runtime = window.chrome.runtime || {};" +
                "window.chrome.runtime.connect = function(){return {id:1,postMessage:function(){},onMessage:{addListener:function(){}}}};" +
                "window.chrome.runtime.connectLocal = function(){return {id:1,postMessage:function(){},onMessage:{addListener:function(){}}}};" +
                "window.chrome.runtime.sendMessage = function(){};" +
                "window.chrome.runtime.sendNativeMessage = function(){};" +

                // ===== WebGL 指纹伪装（阻止 Canvas 指纹检测） =====
                "HTMLCanvasElement.prototype.getContext = (function(old) {" +
                "  return function(type, attrs) {" +
                "    var ctx = old.call(this, type, attrs);" +
                "    if (ctx && (type === 'webgl' || type === 'webgl2')) {" +
                "      var _getParameter = ctx.getParameter.bind(ctx);" +
                "      ctx.getParameter = function(p) {" +
                "        if (p === 37445) return 'Intel Inc.';" +
                "        if (p === 37446) return 'Intel Iris OpenGL Engine';" +
                "        if (p === 37447) return '2.1 INTEL-8.4.1';" +
                "        return _getParameter(p);" +
                "      };" +
                "      ctx.getExtension = function() { return null; };" +
                "    }" +
                "    return ctx;" +
                "  };" +
                "})(HTMLCanvasElement.prototype.getContext);" +

                // ===== 伪装 WebRTC / Network Information =====
                "Object.defineProperty(navigator, 'connection', {get: () => ({effectiveType:'4g', rtt:50, downlink:10, saveData:false}), configurable:true, enumerable:true});" +
                "Object.defineProperty(navigator, 'hardwareConcurrency', {get: () => 8, configurable:true, enumerable:true});" +
                "Object.defineProperty(navigator, 'deviceMemory', {get: () => 8, configurable:true, enumerable:true});" +

                // ===== 伪装 navigator.vendor =====
                "Object.defineProperty(navigator, 'vendor', {get: () => 'Google Inc.', configurable:true, enumerable:true});" +

                // ===== 隐藏 Selenium/ChromeDriver 全局变量（阻止 WAF 检测自动化标记） =====
                "window.__webdriver_evaluate = false; window.__selenium_evaluate = false;" +
                "window.__webdriver_script_function = false; window.__webdriver_script_func = false;" +
                "window.__webdriver_script_fn = false; window.__fxdriver_evaluate = false;" +
                "window.__driver_unwrapped = false; window.__webdriver_unwrapped = false;" +
                "window.__driver_evaluate = false; window.__selenium_unwrapped = false;" +
                "window.__fxdriver_unwrapped = false; window.webdriver = undefined;" +
                "window.cdc_ = undefined; window.$cdc_ = undefined; window.__chrome_for_testing = undefined;" +
                "delete window.cdc_; delete window.$cdc_;" +

                // ===== 伪装 Automation Controlled 特征（阻止 WAF 检测 headless） =====
                "Object.defineProperty(navigator, 'webdriver', {get: () => false, configurable: true});" +
                "Object.defineProperty(navigator, 'automation', {get: () => undefined, configurable: true});" +
                "Object.defineProperty(document, 'webdriver', {get: () => false, configurable: true});" +

                // ===== 伪装 iframe 检测（51job WAF 常用） =====
                "if (window.top !== window.self) { try { Object.defineProperty(window, 'top', {get: () => window}); } catch(e){} }" +

                // ===== 伪装 Notification permission =====
                "Object.defineProperty(Notification, 'permission', {get: () => 'default', configurable: true});" +

                // ===== 禁用 WAF 检测脚本可能读取的属性 =====
                "Object.defineProperty(document, 'hidden', {get: () => false, configurable: true});" +
                "Object.defineProperty(document, 'visibilityState', {get: () => 'visible', configurable: true});" +
                "Object.defineProperty(document, 'hasFocus', {get: () => true, configurable: true});"
            );

            if (driver instanceof ChromeDriver) {
                try {
                    ((ChromeDriver) driver).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", stealthScript);
                    log.debug("CDP stealth 增强脚本注入成功");
                } catch (Exception cdpEx) {
                    log.debug("CDP 注入失败，降级为 JS 注入: {}", cdpEx.getMessage());
                    injectAntiDetectJSFallback(driver);
                }
            } else {
                injectAntiDetectJSFallback(driver);
            }

        } catch (Exception e) {
            log.debug("注入反检测JS失败: {}", e.getMessage());
        }
    }

    /**
     * JS 注入降级方案（当 CDP 不可用时）
     */
    private void injectAntiDetectJSFallback(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined, configurable: true, enumerable: true});" +
                "Object.defineProperty(navigator, 'chrome', {get: () => ({runtime: {}}), configurable: true, enumerable: true});" +
                "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5], configurable: true, enumerable: true});" +
                "Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN', 'zh', 'en'], configurable: true, enumerable: true});" +
                "Object.defineProperty(navigator, 'connection', {get: () => ({effectiveType:'4g', rtt:50, downlink:10}), configurable: true, enumerable: true});" +
                "Object.defineProperty(navigator, 'hardwareConcurrency', {get: () => 8, configurable: true, enumerable: true});" +
                "if (!window.chrome) window.chrome = {};" +
                "window.chrome.runtime = window.chrome.runtime || {};" +
                "window.chrome.runtime.connect = function(){return {id:1,postMessage:function(){},onMessage:{addListener:function(){}}}};" +
                "window.cdc_ = undefined; window.$cdc_ = undefined;" +
                "delete window.cdc_; delete window.$cdc_;" +
                "window.__webdriver_evaluate = false; window.__selenium_evaluate = false;" +
                "Object.defineProperty(document, 'hidden', {get: () => false, configurable: true});" +
                "                Object.defineProperty(document, 'visibilityState', {get: () => 'visible', configurable: true});"
            );
        } catch (Exception e) {
            log.debug("JS 注入降级方案失败: {}", e.getMessage());
        }
    }

    /**
     * 等待用户在浏览器中完成滑块验证。
     * 每5秒检查一次页面内容是否已不再是验证页，最多等待5分钟。
     * @return true=验证通过，false=超时
     */
    private boolean waitForVerificationDone(WebDriver driver, Long taskId, String detailUrl) {
        int maxWaitCycles = 60; // 60 * 5s = 5分钟
        for (int i = 0; i < maxWaitCycles; i++) {
            sleep(5000);
            try {
                String html = driver.getPageSource();
                if (html == null) continue;
                // 验证页消失，说明用户已通过验证
                if (!html.contains("别离开") && !html.contains("进行验证")) {
                    log.info("任务 {} 滑块验证已完成，继续爬取", taskId);
                    return true;
                }
                log.debug("任务 {} 等待验证完成... ({}/{})", taskId, i + 1, maxWaitCycles);
            } catch (Exception e) {
                log.debug("任务 {} 检查验证状态异常: {}", taskId, e.getMessage());
            }
        }
        log.warn("任务 {} 滑块验证等待超时（5分钟），跳过此URL: {}", taskId, detailUrl);
        return false;
    }

    // ===================== 登录态维持（Cookie 持久化） =====================

    /** yingjiesheng.com 的登录态 Cookie 文件 */
    private static final String COOKIE_FILE = System.getProperty("java.io.tmpdir")
            + File.separator + "yingjiesheng_cookies.json";

    /**
     * 确保浏览器已登录 yingjiesheng.com（已暂时禁用，加速爬取）
     * 如需恢复，只需还原此方法体即可
     */
    private void ensureYingjieshengLogin(Long taskId) {
        // 已禁用登录，跳过。如需恢复，把方法体内容换回来即可。
    }

    /** 检测页面是否需要登录（出现登录入口或用户信息不存在） */
    private boolean isLoginRequired(String html) {
        if (html == null || html.length() < 200) return true;
        String lower = html.toLowerCase();
        // 检查未登录特征（登录按钮、注册入口存在但用户信息缺失）
        if (lower.contains("登录") && lower.contains("注册")) {
            // 有登录注册按钮 = 未登录
            return true;
        }
        return false;
    }

    /** 打开登录页面，等待用户在浏览器中手动登录，完成后保存 Cookie */
    private void openLoginAndWait(WebDriver driver, Long taskId) {
        log.info("任务 {} 正在打开 yingjiesheng.com 登录页面，请手动登录...", taskId);
        driver.get("https://www.yingjiesheng.com/user/login.php");
        sleep(3000);

        // 截图让用户知道要做什么
        try {
            byte[] screenshot = takeScreenshot(taskId);
            if (screenshot != null) {
                Files.write(Paths.get(System.getProperty("java.io.tmpdir"),
                        "yingjiesheng_login_task" + taskId + ".png"), screenshot);
                log.info("任务 {} 登录截图已保存: yingjiesheng_login_task{}.png", taskId, taskId);
            }
        } catch (Exception ignored) {}

        // 每5秒检查是否登录成功（出现用户信息或登出按钮）
        int maxWait = 60; // 最多等5分钟
        for (int i = 0; i < maxWait; i++) {
            sleep(5000);
            try {
                String html = driver.getPageSource();
                if (html == null) continue;
                // 登录成功的特征：出现用户名、用户中心、登出按钮等
                if (html.contains("用户中心") || html.contains("我的简历") || html.contains("退出")
                        || (!html.contains("登录") && html.contains("首页"))) {
                    log.info("任务 {} 检测到登录成功，保存 Cookie...", taskId);
                    saveCookies(driver, "https://www.yingjiesheng.com");
                    log.info("任务 {} Cookie 已保存，后续爬取无需再次登录", taskId);
                    return;
                }
                // 检查是否还在登录页
                if (html.contains("登录") && html.contains("密码") && !html.contains("用户中心")) {
                    log.debug("任务 {} 等待手动登录中 ({}/{})", taskId, i + 1, maxWait);
                }
            } catch (Exception e) {
                log.debug("任务 {} 检查登录状态异常: {}", taskId, e.getMessage());
            }
        }
        log.warn("任务 {} 等待登录超时（5分钟），将使用无登录态继续爬取", taskId);
    }

    /** 从文件加载 Cookie 并注入浏览器 */
    private boolean loadCookies(WebDriver driver, String domain) {
        try {
            Path path = Paths.get(COOKIE_FILE);
            if (!Files.exists(path)) return false;

            String json = Files.readString(path);
            List cookies = new ObjectMapper().readValue(json, List.class);

            driver.get(domain);
            sleep(1000);

            for (Object c : cookies) {
                Map<?, ?> cookie = (Map<?, ?>) c;
                String name = String.valueOf(cookie.get("name"));
                String value = String.valueOf(cookie.get("value"));
                String domainVal = cookie.containsKey("domain") ? String.valueOf(cookie.get("domain")) : null;
                String pathVal = cookie.containsKey("path") ? String.valueOf(cookie.get("path")) : "/";
                Number expiry = cookie.containsKey("expiry") ? (Number) cookie.get("expiry") : null;

                org.openqa.selenium.Cookie.Builder builder = new org.openqa.selenium.Cookie.Builder(name, value).path(pathVal);
                if (domainVal != null && !domainVal.isEmpty()) builder.domain(domainVal);
                if (expiry != null) builder.expiresOn(new Date(expiry.longValue() * 1000));

                driver.manage().addCookie(builder.build());
            }
            log.debug("已加载 {} 个 Cookie from {}", cookies.size(), COOKIE_FILE);
            return true;
        } catch (Exception e) {
            log.debug("加载 Cookie 失败: {}", e.getMessage());
            return false;
        }
    }

    /** 将当前浏览器 Cookie 保存到文件 */
    private void saveCookies(WebDriver driver, String domain) {
        try {
            Set<org.openqa.selenium.Cookie> cookies = driver.manage().getCookies();
            List<Map<String, Object>> serializable = new ArrayList<>();
            for (org.openqa.selenium.Cookie c : cookies) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("name", c.getName());
                map.put("value", c.getValue());
                map.put("domain", c.getDomain());
                map.put("path", c.getPath());
                if (c.getExpiry() != null) map.put("expiry", c.getExpiry().getTime() / 1000);
                serializable.add(map);
            }
            Files.writeString(Paths.get(COOKIE_FILE), new ObjectMapper().writeValueAsString(serializable));
            log.info("已保存 {} 个 Cookie 到 {}", serializable.size(), COOKIE_FILE);
        } catch (Exception e) {
            log.warn("保存 Cookie 失败: {}", e.getMessage());
        }
    }
}
