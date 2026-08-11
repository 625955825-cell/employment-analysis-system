package com.employment.service.extractor;

import com.employment.model.entity.SpiderCollectedData;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 应届生求职网（yingjiesheng.com）职位数据提取器。
 *
 * HTML 结构说明：
 * - 搜索结果页以列表形式展示，每条职位包含：
 *   <h2> 职位名称
 *   <span class="company"> 公司名称
 *   <span class="city"> 城市
 *   <span class="salary"> 薪资（可能为空）
 *   <span class="date"> 发布日期
 * - 分页：?start=0 (第1页), ?start=20 (第2页)，每页20条
 *
 * 特点：
 * 1. 爬虫友好，普通HTTP请求即可获取数据
 * 2. 专为应届生设计，数据精准匹配主题
 * 3. 数据结构清晰，解析简单
 * 4. 无强反爬机制
 *
 * 训练推荐算法所需字段：jobName, companyName, salary(could be null), city, education, experience, companyScale, industry
 */
@Slf4j
@Component
public class JobYingjieshengExtractor implements SiteDataExtractor {

    @Override
    public String getSourceCode() {
        return "yingjiesheng";
    }

    @Override
    public List<SpiderCollectedData> extract(Document doc, String sourceCode, String major, String keyword) {
        List<SpiderCollectedData> result = new ArrayList<>();

        // ===== 新版搜索列表结构 (2025/2026 应届生网站实际结构) =====
        // 页面结构：多个 <div class="search-list-item job"> 后跟一个同级的 <a data-v-0d978154 href="...?property={...}">
        // 每对 job div + a 构成一条职位，anchor 是 job div 的兄弟元素（不在内部）
        // 同时有 <div class="search-list-item job"> 里嵌套的 <span class="left-tag-item"> 含城市/经验/学历
        int anchorCount = doc.select("a[data-v-0d978154]").size();
        Elements jobItems = doc.select("div.search-list-item.job");
        int itemCount = jobItems.size();
        if (itemCount > 0 && anchorCount > 0) {
            // 从页面收集所有锚点（与 jobItems 按 index 配对）
            List<Element> anchors = doc.select("a[data-v-0d978154]").stream().collect(java.util.stream.Collectors.toList());
            log.debug("使用新版搜索结构：{} 个job项 + {} 个anchor", itemCount, anchorCount);
            for (int i = 0; i < Math.min(itemCount, anchors.size()); i++) {
                SpiderCollectedData data = parseNewSearchItem(jobItems.get(i), anchors.get(i), sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) {
                log.info("YingjieshengExtractor(新版): 成功提取 {} 条数据", result.size());
                return result;
            }
        }

        // ===== 旧版 .j_joblist 结构 (备选) =====
        Elements items = doc.select(".j_joblist .j_job, div.j_joblist div.j_job");
        if (!items.isEmpty()) {
            log.debug("使用结构 .j_joblist .j_job，命中 {} 条", items.size());
            for (Element job : items) {
                SpiderCollectedData data = parseJobItem(job, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案2：表格行结构 - 按li或tr解析
        items = doc.select("ul.searchList li, .joblist li, div.job-list li");
        if (!items.isEmpty()) {
            log.debug("使用列表结构 ul.searchList li，命中 {} 条", items.size());
            for (Element li : items) {
                SpiderCollectedData data = parseListItem(li, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案3：h2 标题 + 公司名模式 (s.yingjiesheng.com 结构)
        items = doc.select("h2, h3");
        if (!items.isEmpty()) {
            log.debug("使用标题结构 h2/h3，命中 {} 个", items.size());
            for (Element h2 : items) {
                // 查找关联的公司名和城市信息
                SpiderCollectedData data = parseByTitle(h2, doc, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案4：通用链接列表结构
        items = doc.select("a[href*=/job/], a[href*=company]");
        if (!items.isEmpty()) {
            log.debug("使用链接结构 a[href]，命中 {} 个", items.size());
            for (Element a : items) {
                SpiderCollectedData data = parseByLink(a, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案5：容错处理 - 遍历所有可能包含职位信息的div
        items = doc.select("div.job_item, div.job-info, div.result-item");
        if (!items.isEmpty()) {
            log.debug("使用通用卡片结构 div.job_item，命中 {} 条", items.size());
            for (Element item : items) {
                SpiderCollectedData data = parseCardItem(item, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案6：文本模式 - 尝试从文本中提取城市和薪资
        String text = doc.body().text();
        if (text.contains("招聘") && text.contains("元")) {
            log.debug("使用文本模式提取数据");
            result.addAll(parseTextPattern(text, sourceCode, major, keyword));
        }

        if (result.isEmpty()) {
            log.warn("YingjieshengExtractor: 未识别到任何职位数据，请检查HTML结构");
        } else {
            log.info("YingjieshengExtractor: 成功提取 {} 条数据", result.size());
        }

        return result;
    }

    // ---- 正则预编译（类加载时只执行一次）----
    private static final Pattern PAT_CITY = Pattern.compile("([^\\-\\d\\s]{2,10}?)-\\w+[区市县镇]|[^\\-\\d\\s]{2,6}(?=-\\d)|([^\\-\\d\\s]{2,8}?)(?=\\d{4,})");
    private static final Pattern PAT_SALARY = Pattern.compile("(\\d+\\.?\\d*[万万]?-?\\d*\\.?\\d*[千k]?(?:\\.[1-9])?[元]?|面议|薪资面议)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_EDU = Pattern.compile("(博士|硕士|本科|大专|高中|中技|中专|学历不限|无需经验)");
    private static final Pattern PAT_EXP = Pattern.compile("(在校学生|无工作经验|无工作经历|\\d[\\-至~至～]\\d+年|\\d+年以上?|无需经验)");
    private static final Pattern PAT_COMPANY_SCALE = Pattern.compile("(\\d+-\\d+[人万]|[1-9][0-9]{2,4}[人万]|10000[人万]|\\d+人以下)");
    // 用于详情页正文提取城市的正则：匹配 [城市] 或 城市- 或 城市\n
    private static final Pattern PAT_CITY_DETAIL = Pattern.compile(
            "\\[([^\\]]{2,10}?)\\]|([\\u4e00-\\u9fa5]{2,8}?)-\\w+[区市县镇省]|工作地点：([\\u4e00-\\u9fa5]{2,10}?)");

    /**
     * 新版搜索列表解析：job div（div.search-list-item.job）和对应的锚点（a[data-v-0d978154]）是同级的兄弟元素，
     * 不在同一个父容器下，按 index 配对。
     * job div 含: left-title-name（职位名）、left-tag-item（城市/经验/学历）
     * anchor 含: href="...?property={JSON}"（含 jobId, companyName, monthSalary）
     */
    private SpiderCollectedData parseNewSearchItem(Element jobItem, Element anchor,
            String sourceCode, String major, String keyword) {
        try {
            // 1. 职位名：从 job div 取
            String jobName = "";
            Element titleEl = jobItem.selectFirst("div.left-title-name");
            if (titleEl != null) {
                jobName = titleEl.text().trim();
            }
            if (jobName.isEmpty()) {
                return null;
            }

            // 2. 从 anchor href JSON 中解析 companyName + salary
            String companyName = "";
            String salary = "";
            String detailUrl = "";
            if (anchor != null) {
                String href = anchor.attr("href");
                if (href != null && href.contains("property=")) {
                    detailUrl = href;
                    String jsonStr = href.substring(href.indexOf("property=") + 9);
                    try {
                        jsonStr = java.net.URLDecoder.decode(jsonStr, java.nio.charset.StandardCharsets.UTF_8.name());
                    } catch (Exception ignored) {}
                    int ampIdx = jsonStr.indexOf("&property=");
                    if (ampIdx >= 0) jsonStr = jsonStr.substring(0, ampIdx);
                    // 清理末尾不完整的 JSON
                    int lastQuoteBrace = jsonStr.lastIndexOf("\"}");
                    if (lastQuoteBrace >= 0) jsonStr = jsonStr.substring(0, lastQuoteBrace + 2);

                    if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                        JsonNode node = new ObjectMapper().readTree(jsonStr);
                        companyName = nullSafe(node, "companyName");
                        salary = nullSafe(node, "monthSalary");
                    }
                }
            }

            // 3. 从 left-tag-item 解析城市 / 经验 / 学历
            String city = "";
            String experience = "";
            String education = "";
            Elements tags = jobItem.select("span.left-tag-item");
            for (int i = 0; i < tags.size(); i++) {
                String tag = tags.get(i).text().trim();
                if (tag.isEmpty()) continue;
                if (i == 0) {
                    city = tag.contains("-") ? tag.substring(0, tag.indexOf("-")) : tag;
                } else if (i == 1) {
                    experience = tag;
                } else if (i == 2) {
                    education = tag;
                }
            }

            return buildSpiderData(sourceCode, major, keyword, jobName, companyName,
                    salary, city, experience, education, "", "", detailUrl, jobItem);

        } catch (Exception e) {
            log.debug("解析 new-search-item 异常: {}", e.getMessage());
            return null;
        }
    }

    /** 从 JsonNode 安全读取字符串字段，为空返回空串 */
    private String nullSafe(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText().trim() : "";
    }

    /**
     * 解析 .j_joblist .j_job 结构（文本拼接格式）
     * HTML结构示例：
     * <div class="j_job">
     *   <h2><a href="...">职位名称</a></h2>
     *   <span class="company">公司名称</span>
     *   <span class="city">北京</span>
     *   <span class="salary">5000-8000</span>
     * </div>
     *
     * 实际 yingjiesheng.com 结构（已知）：
     * 容器内文本拼接为一行，格式为：
     *   职位名称 城市-区域 经验 学历 公司名称 公司性质 公司规模 行业 薪资 链接...
     */
    private SpiderCollectedData parseJobItem(Element job, String sourceCode, String major, String keyword) {
        try {
            String text = job.text();

            if (text.isEmpty() || text.contains("登录") || text.contains("请登录")) {
                return null;
            }

            // 如果有子元素的 class/结构，按结构解析（理想情况）
            Element titleEl = job.selectFirst("h2 a, h3 a, .title a");
            String jobName = "";
            String detailUrl = "";
            if (titleEl != null) {
                jobName = titleEl.text().trim();
                // 提取详情页 URL
                String href = titleEl.attr("href");
                if (!href.isEmpty()) {
                    detailUrl = href;
                }
            }
            if (jobName.isEmpty()) {
                Element titleEl2 = job.selectFirst("h2, h3, .title");
                if (titleEl2 != null) {
                    jobName = titleEl2.text().trim();
                }
                // 尝试从 h2/h3 内的 a 标签获取 URL
                Element linkInTitle = job.selectFirst("h2 a, h3 a");
                if (linkInTitle != null && detailUrl.isEmpty()) {
                    detailUrl = linkInTitle.attr("href");
                }
            }

            // 检查是否有独立的子元素提供结构化字段
            String companyName = "";
            Element companyEl = job.selectFirst("[class*=company]");
            if (companyEl != null) {
                companyName = companyEl.text().trim();
            }
            // 如果公司名没有被单独子元素提取，则从全文中解析
            if (companyName.isEmpty()) {
                companyName = parseCompanyNameFromText(text, jobName);
            }

            String city = "";
            Element cityEl = job.selectFirst("[class*=city]");
            if (cityEl != null) {
                city = cityEl.text().trim().replaceAll("[\\[\\]]", "");
            }

            String salary = "";
            Element salaryEl = job.selectFirst("[class*=salary]");
            if (salaryEl != null) {
                salary = salaryEl.text().trim();
            }

            String experience = "";
            Element expEl = job.selectFirst("[class*=experience], [class*=workyear]");
            if (expEl != null) {
                experience = expEl.text().trim();
            }

            String education = "";
            Element eduEl = job.selectFirst("[class*=education], [class*=degree]");
            if (eduEl != null) {
                education = eduEl.text().trim();
            }

            // 如果上述结构化解析全部为空，说明是纯文本拼接格式，使用正则解析
            ParsedJobFields fields = null;
            String companyScale = "";
            if (city.isEmpty() && salary.isEmpty() && experience.isEmpty() && education.isEmpty()) {
                fields = parseJobTextByRegex(text, jobName);
                if (fields != null) {
                    if (city.isEmpty()) city = fields.city;
                    if (salary.isEmpty()) salary = fields.salary;
                    if (experience.isEmpty()) experience = fields.experience;
                    if (education.isEmpty()) education = fields.education;
                    if (companyName.isEmpty()) companyName = fields.companyName;
                    if (!fields.companyScale.isEmpty()) companyScale = fields.companyScale;
                    // 如果 jobName 被解析得更准确，用它替换
                    if (!fields.jobName.isEmpty() && fields.jobName.length() < jobName.length()) {
                        jobName = fields.jobName;
                    }
                }
            }

            if (jobName.isEmpty()) {
                return null;
            }

            // 过滤职位名中的福利标签等噪音
            jobName = jobName.replaceAll("(五险一金|带薪年假|定期体检|出差补贴|有餐补|定期团建|定期团建|节日福利|年终奖).*", "").trim();

            return buildSpiderData(sourceCode, major, keyword, jobName, companyName,
                    salary, city, experience, education, "", companyScale, detailUrl, job);

        } catch (Exception e) {
            log.debug("解析 j_job 结构异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 使用正则从纯文本中解析职位字段。
     * 适用于 yingjiesheng.com 的文本拼接格式。
     */
    private ParsedJobFields parseJobTextByRegex(String text, String fallbackJobName) {
        if (text == null || text.isEmpty()) return null;

        ParsedJobFields f = new ParsedJobFields();

        // 1. 提取薪资（最可靠的特征）- 匹配格式：数字+千/万/元/月 等
        // 例如: "5.2-6千" "6000-8000" "面议" "10-15万/年"
        Matcher salaryMatcher = Pattern.compile(
            "([5-9]\\d*\\.?\\d*千|"    // e.g. 5.2-6千, 6000
            + "\\d+\\.?\\d*万|"        // e.g. 10万, 1.5万
            + "\\d+-\\d+元|"           // e.g. 6000-8000元
            + "\\d+-\\d+千(?![\\d千])|" // e.g. 6-7千 (not followed by another 千)
            + "面议|薪资面议)"
        , Pattern.CASE_INSENSITIVE).matcher(text);
        if (salaryMatcher.find()) {
            f.salary = salaryMatcher.group().trim();
        }

        // 2. 提取城市
        // 匹配: "城市-区域" 或 "城市" 后面紧跟经验/学历/数字
        Matcher cityMatcher = PAT_CITY.matcher(text);
        while (cityMatcher.find()) {
            String city = cityMatcher.group(1) != null ? cityMatcher.group(1) : cityMatcher.group(2);
            if (city != null && city.length() >= 2 && city.length() <= 10 && !city.equals(fallbackJobName)) {
                if (f.city.isEmpty()) {
                    f.city = city.trim();
                    break;
                }
            }
        }
        // 如果上面没匹配到，尝试匹配 "城市-区域" 格式
        if (f.city.isEmpty()) {
            Matcher citySimple = Pattern.compile("([^\\s\\-\\d]{2,8}?)(?=-[\\w]{2,6}\\d|-\\d|\\s{2,})").matcher(text);
            while (citySimple.find()) {
                String city = citySimple.group(1);
                if (city != null && city.length() >= 2 && city.length() <= 8) {
                    f.city = city.trim();
                    break;
                }
            }
        }

        // 3. 提取学历
        Matcher eduMatcher = PAT_EDU.matcher(text);
        if (eduMatcher.find()) {
            f.education = eduMatcher.group(1);
        }

        // 4. 提取经验
        Matcher expMatcher = PAT_EXP.matcher(text);
        if (expMatcher.find()) {
            f.experience = expMatcher.group(1);
        }

        // 5. 提取公司名 - 通常在学历/经验后面，薪资/福利标签前面
        // 格式: 城市 经验 学历 公司名称 公司性质 公司规模 ...
        // 公司名通常由 2-6 个汉字组成
        String cleaned = text
            .replaceAll("先聊聊|立即申请|提示|\\d{4,}[人万]|[\\u4e00-\\u9fa5]{1,3}年以上?|无需经验|学历不限", " ")
            .trim();

        // 匹配公司名（2-6个汉字，在福利/薪资/城市之后出现）
        Matcher companyMatcher = Pattern.compile(
            "[^\\s]{2,15}?(?=.*[民营合资国企上市公司创业公司]|\\d{3,4}[人万])"
        ).matcher(cleaned);
        String lastMatch = "";
        while (companyMatcher.find()) {
            String candidate = companyMatcher.group().trim();
            if (candidate.length() >= 4 && candidate.length() <= 15 && !candidate.contains("招聘")) {
                lastMatch = candidate;
            }
        }
        if (!lastMatch.isEmpty()) {
            f.companyName = lastMatch;
        }

        // 6. 提取公司规模
        Matcher scaleMatcher = PAT_COMPANY_SCALE.matcher(text);
        if (scaleMatcher.find()) {
            f.companyScale = scaleMatcher.group(1);
        }

        // 7. 提取职位名称（第一条记录作为参考）
        // 职位名通常在开头，后面紧跟城市或数字
        if (fallbackJobName != null && !fallbackJobName.isEmpty()
                && !fallbackJobName.contains("招聘")
                && fallbackJobName.length() < 30) {
            f.jobName = fallbackJobName;
        }

        return f;
    }

    private String parseCompanyNameFromText(String text, String jobName) {
        // 从文本后半部分寻找公司名特征（民营/合资/上市公司等）
        int idx = text.indexOf("公司");
        if (idx > 0) {
            String candidate = text.substring(Math.max(0, idx - 10), idx + 2).trim();
            candidate = candidate.replaceAll("^[^\\u4e00-\\u9fa5]+", "");
            if (candidate.length() >= 4) {
                return candidate;
            }
        }
        // 尝试匹配 2-6 个汉字后面跟公司相关词
        Matcher m = Pattern.compile("([\\u4e00-\\u9fa5]{2,12}公司)").matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static class ParsedJobFields {
        String jobName = "";
        String companyName = "";
        String salary = "";
        String city = "";
        String experience = "";
        String education = "";
        String companyScale = "";
    }

    /**
     * 解析列表项结构
     */
    private SpiderCollectedData parseListItem(Element li, String sourceCode, String major, String keyword) {
        try {
            // 尝试查找职位名
            String jobName = "";
            Element titleEl = li.selectFirst("a[href*=job], a.title, .job-name");
            if (titleEl != null) {
                jobName = titleEl.text().trim();
            }

            if (jobName.isEmpty() || jobName.contains("登录")) {
                return null;
            }

            // 公司名
            String companyName = "";
            Element companyEl = li.selectFirst(".company, .cname, .company-name");
            if (companyEl != null) {
                companyName = companyEl.text().trim();
            }

            // 城市
            String city = "";
            Element cityEl = li.selectFirst(".city, .location");
            if (cityEl != null) {
                city = cityEl.text().trim().replaceAll("[\\[\\]]", "");
            }

            // 薪资
            String salary = "";
            Element salaryEl = li.selectFirst(".salary, .pay");
            if (salaryEl != null) {
                salary = salaryEl.text().trim();
            }

            // 尝试从列表项中提取详情页 URL
            String detailUrlLi = "";
            Element linkEl = li.selectFirst("a[href*=job], a.title, .job-name");
            if (linkEl != null) {
                detailUrlLi = linkEl.attr("href");
            }

            return buildSpiderData(sourceCode, major, keyword, jobName, companyName,
                    salary, city, "", "", "", "", detailUrlLi, li);

        } catch (Exception e) {
            log.debug("解析列表项异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 通过标题元素解析数据
     * 适用于 h2 包含职位名，附近有公司名和城市信息的结构
     */
    private SpiderCollectedData parseByTitle(Element h2, Document doc, String sourceCode, String major, String keyword) {
        try {
            String jobName = h2.text().trim();

            if (jobName.isEmpty() || jobName.contains("登录") || jobName.length() > 100) {
                return null;
            }

            // 在父元素或兄弟元素中查找公司名
            String companyName = "";
            String city = "";
            String salary = "";
            String experience = "";
            String education = "";
            String companyScale = "";
            Element parent = h2.parent();
            if (parent != null) {
                Element companyEl = parent.selectFirst(".company, .cname, [class*=company]");
                if (companyEl != null) {
                    companyName = companyEl.text().trim();
                }

                Element cityEl = parent.selectFirst(".city, .location");
                if (cityEl != null) {
                    city = cityEl.text().trim().replaceAll("[\\[\\]]", "");
                }

                // 如果没有结构化字段，用正则解析父元素的文本
                if (city.isEmpty() && salary.isEmpty() && companyName.isEmpty()) {
                    ParsedJobFields fields = parseJobTextByRegex(parent.text(), jobName);
                    if (fields != null) {
                        if (city.isEmpty()) city = fields.city;
                        if (salary.isEmpty()) salary = fields.salary;
                        if (companyName.isEmpty()) companyName = fields.companyName;
                        if (experience.isEmpty()) experience = fields.experience;
                        if (education.isEmpty()) education = fields.education;
                        if (!fields.companyScale.isEmpty()) companyScale = fields.companyScale;
                    }
                }
            }

            return buildSpiderData(sourceCode, major, keyword, jobName, companyName,
                    salary, city, experience, education, "", companyScale, "", parent);

        } catch (Exception e) {
            log.debug("通过标题解析异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 通过链接解析数据
     */
    private SpiderCollectedData parseByLink(Element a, String sourceCode, String major, String keyword) {
        try {
            String jobName = a.text().trim();

            if (jobName.isEmpty() || jobName.length() > 100 ||
                jobName.contains("登录") || jobName.contains("注册")) {
                return null;
            }

            // 获取父容器以查找更多信息
            Element parent = a.parent();
            while (parent != null && parent.tagName().equals("div")) {
                Element companyEl = parent.selectFirst(".company, [class*=company]");
                if (companyEl != null) {
                    String companyName = companyEl.text().trim();
                    Element cityEl = parent.selectFirst(".city, .location");
                    String city = cityEl != null ? cityEl.text().trim().replaceAll("[\\[\\]]", "") : "";

                    return buildSpiderData(sourceCode, major, keyword, jobName, companyName,
                            "", city, "", "", "", "", a.attr("href"), parent);
                }
                parent = parent.parent();
            }

            return null;

        } catch (Exception e) {
            log.debug("通过链接解析异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析通用卡片结构
     */
    private SpiderCollectedData parseCardItem(Element item, String sourceCode, String major, String keyword) {
        try {
            String jobName = "";
            Element titleEl = item.selectFirst("h2, h3, .title, .job-name");
            if (titleEl != null) {
                jobName = titleEl.text().trim();
            }

            if (jobName.isEmpty() || jobName.contains("登录")) {
                return null;
            }

            String companyName = "";
            Element companyEl = item.selectFirst(".company, .company-name");
            if (companyEl != null) {
                companyName = companyEl.text().trim();
            }

            String city = "";
            Element cityEl = item.selectFirst(".city, .location");
            if (cityEl != null) {
                city = cityEl.text().trim().replaceAll("[\\[\\]]", "");
            }

            String salary = "";
            Element salaryEl = item.selectFirst(".salary, .pay");
            if (salaryEl != null) {
                salary = salaryEl.text().trim();
            }

            // 尝试从卡片中提取详情页 URL
            String detailUrlCard = "";
            Element linkEl = item.selectFirst("a[href*=job]");
            if (linkEl != null) {
                detailUrlCard = linkEl.attr("href");
            }

            return buildSpiderData(sourceCode, major, keyword, jobName, companyName,
                    salary, city, "", "", "", "", detailUrlCard, item);

        } catch (Exception e) {
            log.debug("解析卡片异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 文本模式解析 - 尝试从纯文本中提取结构化数据
     */
    private List<SpiderCollectedData> parseTextPattern(String text, String sourceCode, String major, String keyword) {
        List<SpiderCollectedData> result = new ArrayList<>();

        // 匹配模式：城市 公司名 职位名 薪资
        // 例如：[北京] 公司名 职位名 月薪5000-8000元
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() < 10 || line.length() > 200) continue;

            // 检查是否包含职位相关关键词
            if (line.contains("招聘") || line.contains("月薪") || line.contains("元")) {
                String jobName = "";
                String companyName = "";
                String city = "";
                String salary = "";

                // 提取城市 [北京] 或 北京-
                if (line.contains("[")) {
                    int start = line.indexOf("[");
                    int end = line.indexOf("]");
                    if (start >= 0 && end > start) {
                        city = line.substring(start + 1, end);
                    }
                }

                // 提取薪资 月薪5000-8000元
                if (line.contains("月薪")) {
                    int idx = line.indexOf("月薪");
                    String remaining = line.substring(idx);
                    int end = remaining.indexOf("元");
                    if (end > 0) {
                        salary = remaining.substring(3, end + 1);
                    }
                }

                // 提取职位名 - 通常是链接文本
                // 简化处理：取第一个超过4个字符的中文字符串作为职位名
                if (!line.contains("登录") && line.length() > 20) {
                    // 清理HTML标签
                    line = line.replaceAll("<[^>]+>", "").trim();
                    if (!line.isEmpty() && !line.contains("登录")) {
                        jobName = line;
                    }
                }

                if (!jobName.isEmpty()) {
                    SpiderCollectedData data = new SpiderCollectedData();
                    data.setDataType("job");
                    data.setSourceCode(sourceCode);
                    data.setMajorName(major);
                    data.setIndustryKeyword(keyword);
                    data.setJobName(jobName);
                    data.setCompanyName(companyName);
                    data.setSalary(salary);
                    data.setCity(city);
                    data.setCollectTime(java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    result.add(data);
                }
            }
        }

        return result;
    }

    /**
     * 构建 SpiderCollectedData 对象
     */
    private SpiderCollectedData buildSpiderData(String sourceCode, String major, String keyword,
                                               String jobName, String companyName,
                                               String salary, String city,
                                               String experience, String education,
                                               String publishTime, Element sourceEl) {
        return buildSpiderData(sourceCode, major, keyword, jobName, companyName,
                salary, city, experience, education, publishTime, "", "", sourceEl);
    }

    private SpiderCollectedData buildSpiderData(String sourceCode, String major, String keyword,
                                               String jobName, String companyName,
                                               String salary, String city,
                                               String experience, String education,
                                               String publishTime, String companyScale,
                                               String detailUrl, Element sourceEl) {
        SpiderCollectedData data = new SpiderCollectedData();
        data.setDataType("job");
        data.setSourceCode(sourceCode);
        data.setMajorName(major);
        data.setIndustryKeyword(keyword);
        data.setJobName(jobName);
        data.setCompanyName(companyName);
        data.setSalary(salary);
        data.setCity(city);
        data.setExperience(experience);
        data.setEducation(education);
        if (companyScale != null && !companyScale.isEmpty()) {
            data.setCompanyScale(companyScale);
        }
        if (detailUrl != null && !detailUrl.isEmpty()) {
            data.setDetailUrl(detailUrl);
        }
        data.setCollectTime(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 存储原始HTML用于调试
        if (sourceEl != null) {
            data.setRawData(sourceEl.outerHtml());
        }

        return data;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 从 yingjiesheng.com 详情页 URL 中解析 property JSON，
     * 提取薪资、公司名等关键字段。
     * 格式示例：
     *   https://q.yingjiesheng.com/jobdetail/172116886.html?property={...}&recommendReasons=[]
     */
    public static JsonNode parsePropertyFromUrl(String detailUrl) {
        if (detailUrl == null || !detailUrl.contains("property=")) {
            return null;
        }
        try {
            int start = detailUrl.indexOf("property=") + "property=".length();
            int end = detailUrl.indexOf("&", start);
            if (end < 0) end = detailUrl.length();
            String jsonStr = java.net.URLDecoder.decode(detailUrl.substring(start, end), "UTF-8");
            return MAPPER.readTree(jsonStr);
        } catch (Exception e) {
            log.debug("解析 property JSON 失败 url={}: {}", detailUrl, e.getMessage());
            return null;
        }
    }

    private String getJsonText(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isMissingNode() ? "" : n.asText().trim();
    }

    /**
     * 从应届生求职网详情页 URL 的 property JSON 中提取数据，
     * 避免额外发起 HTTP 请求。
     * 这是最重要的高效数据源。
     */
    @Override
    public SpiderCollectedData extractFromDetail(Document detailDoc, String sourceCode,
            String major, String keyword, String searchJobName, String detailUrl) {
        try {
            String responsibility = "";
            if (detailDoc != null) {
                responsibility = extractResponsibilityFromDoc(detailDoc);
            }

            // ========== 优先方案：从 URL 的 property JSON 中提取（零 HTTP 请求） ==========
            JsonNode propertyJson = parsePropertyFromUrl(detailUrl);
            if (propertyJson != null) {
                String jobName = getJsonText(propertyJson, "jobTitle");
                if (jobName.isEmpty()) jobName = getJsonText(propertyJson, "title");
                if (jobName.isEmpty()) jobName = searchJobName;

                String companyName = getJsonText(propertyJson, "companyName");
                String salary = getJsonText(propertyJson, "monthSalary");

                if (!jobName.isEmpty() || !companyName.isEmpty() || !salary.isEmpty()) {
                    SpiderCollectedData data = new SpiderCollectedData();
                    data.setDataType("job");
                    data.setSourceCode(sourceCode);
                    data.setMajorName(major);
                    data.setIndustryKeyword(keyword);
                    data.setJobName(jobName.isEmpty() ? "未知职位" : jobName);
                    data.setCompanyName(companyName);
                    data.setSalary(salary);
                    data.setResponsibility(responsibility);
                    data.setCollectTime(java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    data.setRawData(detailUrl);
                    data.setDetailUrl(detailUrl);
                    return data;
                }
            }

            // ========== 兜底方案：解析详情页 HTML ==========
            if (detailDoc == null) {
                log.debug("detailDoc 为 null 且 URL 解析失败，跳过: {}", detailUrl);
                return null;
            }

            String bodyText = detailDoc.body().text();

            // 职位名称
            String jobName = "";
            Element titleEl = detailDoc.selectFirst("h1, div.job-title, .position-title");
            if (titleEl != null) jobName = titleEl.text().trim();
            if (jobName.isEmpty()) {
                Element titleTag = detailDoc.selectFirst("title");
                if (titleTag != null) jobName = titleTag.text().trim().replaceAll("[-_].*", "").trim();
            }
            if (jobName.isEmpty()) jobName = searchJobName;

            // 公司名称
            String companyName = "";
            Element companyEl = detailDoc.selectFirst(
                    "div.company-name a, div.company-name[data-v-94771390], span.company-name, a.company-link");
            if (companyEl != null) companyName = companyEl.text().trim();
            if (companyName.isEmpty()) {
                Element metaDesc = detailDoc.selectFirst("meta[name=description]");
                if (metaDesc != null) {
                    String desc = metaDesc.attr("content");
                    int idx = desc.indexOf("招聘");
                    if (idx > 3) companyName = desc.substring(0, idx);
                }
            }

            // 薪资
            String salary = "";
            Element salaryEl = detailDoc.selectFirst(
                    "div.salary[data-v-94771390], div.salary, span.salary, [class*=salary]");
            if (salaryEl != null) {
                salary = salaryEl.attr("title");
                if (salary.isEmpty()) salary = salaryEl.text().trim();
            }

            if (jobName.isEmpty()) {
                log.debug("详情页无法提取职位名称: {}", detailUrl);
                return null;
            }

            SpiderCollectedData data = new SpiderCollectedData();
            data.setDataType("job");
            data.setSourceCode(sourceCode);
            data.setMajorName(major);
            data.setIndustryKeyword(keyword);
            data.setJobName(jobName);
            data.setCompanyName(companyName);
            data.setSalary(salary);
            data.setResponsibility(responsibility);
            data.setCollectTime(java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            data.setRawData(detailDoc.body().html());
            data.setDetailUrl(detailUrl);

            return data;

        } catch (Exception e) {
            log.debug("解析详情页异常 url={}: {}", detailUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 从 Jsoup Document 中提取职位信息（职责+要求）整块文本。
     * 先尝试各常见选择器，再从正文中找包含"职位信息"的 div，最后正则兜底。
     */
    private String extractResponsibilityFromDoc(Document doc) {
        String bodyText = doc.body().text();

        // ① 优先找完整职位信息区块
        Element jobinfoEl = doc.selectFirst("div.jobinfo");
        if (jobinfoEl != null) {
            String txt = jobinfoEl.text().trim();
            if (txt.length() > 20) return txt;
        }

        // ② 依次尝试其他常见内容区（选最长）
        String[] candSelectors = {
                "div.detail-content-common", "div.detail-content", "div.job-detail",
                "div.content", "div.info-content", "div.info-wrapper",
                "div.tab-content", "div.job-detail-box", "div.detail-con",
                "div.main", "div#content", "div.job-content"
        };
        String best = "";
        for (String sel : candSelectors) {
            Elements els = doc.select(sel);
            if (!els.isEmpty()) {
                String txt = els.first().text().trim();
                if (txt.length() > best.length()) best = txt;
            }
        }
        if (best.length() > 30) return best;

        // ③ 从整个 body 找包含"职位信息"的 div
        Elements allDivs = doc.select("div");
        int maxLen = 0;
        Element bestDiv = null;
        for (Element div : allDivs) {
            String txt = div.text();
            if (txt.contains("职位信息") && txt.length() > maxLen) {
                maxLen = txt.length();
                bestDiv = div;
            }
        }
        if (bestDiv != null && maxLen > 30) return bestDiv.text().trim();

        // ④ 正则兜底
        ParsedJobFields fields = parseJobTextByRegex(bodyText, "");
        if (fields != null) {
            StringBuilder sb = new StringBuilder();
            if (!fields.city.isEmpty())      sb.append("城市: ").append(fields.city).append("\n");
            if (!fields.education.isEmpty()) sb.append("学历: ").append(fields.education).append("\n");
            if (!fields.experience.isEmpty()) sb.append("经验: ").append(fields.experience).append("\n");
            if (sb.length() > 0) return sb.toString().trim();
        }

        return best;
    }
}
