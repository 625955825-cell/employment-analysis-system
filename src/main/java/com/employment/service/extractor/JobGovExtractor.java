package com.employment.service.extractor;

import com.employment.model.entity.SpiderCollectedData;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 中国公共招聘网（job.mohrss.gov.cn）职位数据提取器。
 *
 * HTML 结构说明：
 * - 搜索结果页以表格形式展示，职位信息在 <table> 或 <ul class="list"> 中
 * - 常见结构：
 *   <table class="result-list"> → <tr> 每行一条职位
 *   <ul class="list"> → <li> 每条职位
 * - 每条职位通常包含：职位名、公司名、薪资（可能为空）、工作地点、发布时间
 *
 * 政府网站特点：
 * 1. 无反爬，数据真实权威（国企/事业单位居多）
 * 2. 无薪资字段的情况较常见
 * 3. 职位数量相对较少
 *
 * 训练推荐算法所需字段：jobName, companyName, salary(could be null), city, education, experience, companyScale, industry
 */
@Slf4j
@Component
public class JobGovExtractor implements SiteDataExtractor {

    @Override
    public String getSourceCode() {
        return "gov";
    }

    @Override
    public List<SpiderCollectedData> extract(Document doc, String sourceCode, String major, String keyword) {
        List<SpiderCollectedData> result = new ArrayList<>();

        // 方案1：标准表格结构 table.list tr
        Elements items = doc.select("table.list tr, table.result-list tr");
        if (!items.isEmpty()) {
            log.debug("使用表格结构 table.list tr，命中 {} 行", items.size());
            for (Element row : items) {
                // 过滤表头行
                if (row.select("th").size() > 0) continue;
                SpiderCollectedData data = parseTableRow(row, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案2：ul.list > li 结构
        items = doc.select("ul.list li, ul.job-list li, div.list li");
        if (!items.isEmpty()) {
            log.debug("使用列表结构 ul.list li，命中 {} 条", items.size());
            for (Element li : items) {
                SpiderCollectedData data = parseListItem(li, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案3：div.result / div.job-item 结构
        items = doc.select("div.result-item, div.job-item, div.result-content");
        if (!items.isEmpty()) {
            log.debug("使用卡片结构 div.result-item，命中 {} 条", items.size());
            for (Element card : items) {
                SpiderCollectedData data = parseCard(card, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案4：政府平台常见结构 - tr.jobinfo
        items = doc.select("tr.jobinfo");
        if (!items.isEmpty()) {
            log.debug("使用 tr.jobinfo 结构，命中 {} 条", items.size());
            for (Element row : items) {
                SpiderCollectedData data = parseTableRow(row, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        log.warn("政府公共招聘网页面中未找到已知职位列表结构");
        return result;
    }

    private SpiderCollectedData parseTableRow(Element row, String sourceCode, String major, String keyword) {
        String text = row.text();
        if (text.length() < 10) return null;

        SpiderCollectedData data = newData(sourceCode, major, keyword);
        Elements tds = row.select("td");

        if (tds.size() >= 1) {
            // 职位名通常在第一个 td
            Element jobLink = tds.get(0).selectFirst("a");
            if (jobLink != null) {
                data.setJobName(jobLink.text().trim());
            } else {
                data.setJobName(tds.get(0).text().trim());
            }
        }
        if (tds.size() >= 2) {
            data.setCompanyName(tds.get(1).text().trim());
        }
        if (tds.size() >= 3) {
            data.setCity(extractCity(tds.get(2).text().trim()));
        }
        if (tds.size() >= 5) {
            data.setEducation(extractEducation(tds.get(4).text()));
        }

        // 薪资可能不在表格中，政府岗位多为面议
        // 尝试从文本中匹配
        if (data.getSalary() == null) {
            String salaryText = row.text();
            data.setSalary(extractSalary(salaryText));
        }

        return validateAndReturn(data);
    }

    private SpiderCollectedData parseListItem(Element li, String sourceCode, String major, String keyword) {
        String text = li.text();
        if (text.length() < 10) return null;

        SpiderCollectedData data = newData(sourceCode, major, keyword);

        // 职位名
        Element jobLink = li.selectFirst("a");
        if (jobLink != null) {
            String jobName = jobLink.text().trim();
            if (jobName.length() >= 2) data.setJobName(jobName);
        }

        // 公司名
        Element companyEl = li.selectFirst("[class*='company'], [class*='corp'], .company");
        if (companyEl != null) {
            data.setCompanyName(companyEl.text().trim());
        }

        // 城市/地区
        Element cityEl = li.selectFirst("[class*='area'], [class*='city'], [class*='location']");
        if (cityEl != null) data.setCity(extractCity(cityEl.text().trim()));

        // 薪资
        Element salaryEl = li.selectFirst("[class*='salary'], .salary");
        if (salaryEl != null) {
            data.setSalary(extractSalary(salaryEl.text()));
        }

        // 学历/经验
        Element detailEl = li.selectFirst("[class*='require'], [class*='info']");
        if (detailEl != null) {
            String detailText = detailEl.text();
            data.setEducation(extractEducation(detailText));
            data.setExperience(extractExperience(detailText));
        }

        return validateAndReturn(data);
    }

    private SpiderCollectedData parseCard(Element card, String sourceCode, String major, String keyword) {
        String text = card.text();
        if (text.length() < 10) return null;

        SpiderCollectedData data = newData(sourceCode, major, keyword);

        // 职位名
        Element jobLink = card.selectFirst("a[class*='title'], a[class*='job'], h3 a");
        if (jobLink != null) data.setJobName(jobLink.text().trim());

        // 公司名
        Element companyEl = card.selectFirst("[class*='company'], .company-name");
        if (companyEl != null) data.setCompanyName(companyEl.text().trim());

        // 薪资
        Element salaryEl = card.selectFirst("[class*='salary'], .salary");
        if (salaryEl != null) data.setSalary(extractSalary(salaryEl.text()));

        // 城市
        Element cityEl = card.selectFirst("[class*='area'], [class*='city'], [class*='location']");
        if (cityEl != null) data.setCity(extractCity(cityEl.text().trim()));

        // 学历/经验
        Element detailEl = card.selectFirst("[class*='require'], [class*='info'], .condition");
        if (detailEl != null) {
            String detailText = detailEl.text();
            data.setEducation(extractEducation(detailText));
            data.setExperience(extractExperience(detailText));
        }

        return validateAndReturn(data);
    }

    private SpiderCollectedData newData(String sourceCode, String major, String keyword) {
        SpiderCollectedData data = new SpiderCollectedData();
        data.setSourceCode(sourceCode);
        data.setDataType("job");
        data.setMajorName(major);
        data.setIndustryKeyword(keyword);
        data.setCollectTime(java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.setIsSynced("0");
        data.setIndustry(inferIndustry(keyword, null));
        return data;
    }

    private SpiderCollectedData validateAndReturn(SpiderCollectedData data) {
        if (data.getJobName() == null || data.getJobName().length() < 2) return null;
        // 过滤明显非职位的
        String jn = data.getJobName();
        if (jn.contains("首页") || jn.contains("下一页") || jn.contains("上一页")) return null;
        if (data.getCompanyName() == null || data.getCompanyName().isEmpty()) {
            data.setCompanyName("未知单位");
        }
        return data;
    }

    private String extractCity(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.contains("异地")) raw = raw.replace("异地", "").trim();
        return raw;
    }

    private String extractEducation(String text) {
        if (text.contains("博士")) return "博士";
        if (text.contains("硕士")) return "硕士";
        if (text.contains("本科")) return "本科";
        if (text.contains("大专")) return "大专";
        if (text.contains("不限")) return "学历不限";
        return null;
    }

    private String extractExperience(String text) {
        if (text.contains("无工作经验") || text.contains("无工作经历")) return "无经验";
        if (text.contains("在校学生")) return "在校学生";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d[\\-至~]\\d+年|\\d+年以上?|无经验|应届生)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) return m.group().trim();
        return null;
    }

    private String extractSalary(String text) {
        if (text == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "(\\d{3,6}/[月年天]|[1-9]\\d*[千万]?-?\\d*[千万]?元|[面议不限])");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) return m.group().trim();
        return null;
    }

    private String inferIndustry(String keyword, String jobName) {
        String combined = (keyword != null ? keyword : "");
        if (combined.contains("地质") || combined.contains("测绘") || combined.contains("勘查") || combined.contains("矿业")) {
            return "地质/矿产";
        }
        if (combined.contains("机械") || combined.contains("制造") || combined.contains("工艺")) {
            return "机械/制造";
        }
        if (combined.contains("电气") || combined.contains("自动化") || combined.contains("控制")) {
            return "电子/自动化";
        }
        if (combined.contains("化工") || combined.contains("材料") || combined.contains("化学")) {
            return "化工/材料";
        }
        if (combined.contains("生物") || combined.contains("制药") || combined.contains("医学")) {
            return "生物医药";
        }
        if (combined.contains("新能源") || combined.contains("电池") || combined.contains("光伏")) {
            return "新能源";
        }
        if (combined.contains("建筑") || combined.contains("土木") || combined.contains("造价") || combined.contains("施工")) {
            return "建筑/房地产";
        }
        if (combined.contains("物流") || combined.contains("供应链") || combined.contains("运输")) {
            return "交通物流";
        }
        if (combined.contains("教育") || combined.contains("教师") || combined.contains("培训")) {
            return "教育培训";
        }
        if (combined.contains("计算机") || combined.contains("软件") || combined.contains("IT") || combined.contains("互联网")) {
            return "互联网/IT";
        }
        return "政府/公共事业";
    }

}
