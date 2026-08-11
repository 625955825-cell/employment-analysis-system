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
 * 国家大学生就业服务平台（bjbys.ncss.cn）职位数据提取器。
 * 教育部官方平台，面向应届毕业生，职位以校招/实习为主。
 *
 * HTML 结构说明：
 * - 搜索结果页通常使用 Vue/React 渲染，HTML 中可能没有完整 DOM
 * - 常见结构：
 *   <div class="job-list"> → <div class="job-item"> 每条职位
 *   <div class="job-box"> → 各字段分散在不同子元素中
 *   <ul class="job-ul"> → <li> 每条职位
 *
 * 特点：
 * 1. 面向应届生，experience 通常为"在校学生"或"应届生"
 * 2. companyScale 通常为空（中小企业为主）
 * 3. salary 可能有缺失或为"面议"
 *
 * 训练推荐算法所需字段：jobName, companyName, salary, city, education, experience, companyScale, industry
 */
@Slf4j
@Component
public class JobEducationExtractor implements SiteDataExtractor {

    @Override
    public String getSourceCode() {
        return "moe";
    }

    @Override
    public List<SpiderCollectedData> extract(Document doc, String sourceCode, String major, String keyword) {
        List<SpiderCollectedData> result = new ArrayList<>();

        // 方案1：标准职位列表结构 .job-list .job-item
        Elements items = doc.select(".job-list .job-item, .job-list .job-item-box");
        if (!items.isEmpty()) {
            log.debug("使用 .job-list .job-item 结构，命中 {} 条", items.size());
            for (Element item : items) {
                SpiderCollectedData data = parseJobItem(item, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案2：ul.job-ul > li 结构
        items = doc.select("ul.job-ul li, ul.job-list li");
        if (!items.isEmpty()) {
            log.debug("使用 ul.job-ul li 结构，命中 {} 条", items.size());
            for (Element li : items) {
                SpiderCollectedData data = parseJobItem(li, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案3：div.job-box 结构
        items = doc.select("div.job-box, div[class*='job-box']");
        if (!items.isEmpty()) {
            log.debug("使用 div.job-box 结构，命中 {} 条", items.size());
            for (Element box : items) {
                SpiderCollectedData data = parseJobItem(box, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        // 方案4：通用职位卡片结构（兜底）
        items = doc.select("[class*='job-list'] > div, [class*='joblist'] > div, [class*='job-item']");
        if (!items.isEmpty()) {
            log.debug("使用通用职位卡片结构，命中 {} 条", items.size());
            for (Element card : items) {
                SpiderCollectedData data = parseJobItem(card, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            if (!result.isEmpty()) return result;
        }

        log.warn("教育部就业平台页面中未找到已知职位列表结构");
        return result;
    }

    private SpiderCollectedData parseJobItem(Element item, String sourceCode, String major, String keyword) {
        String text = item.text();
        if (text.length() < 10) return null;

        SpiderCollectedData data = newData(sourceCode, major, keyword);

        // 职位名：优先从标题/链接中获取
        Element jobLink = item.selectFirst("a[class*='title'], a[class*='job'], h3 a, h4 a, .job-title a, .job-name a");
        if (jobLink != null) {
            String jobName = jobLink.text().trim();
            if (jobName.length() >= 2) data.setJobName(jobName);
        }
        // 兜底：直接找标题元素
        if (data.getJobName() == null) {
            Element titleEl = item.selectFirst("h3, h4, .job-title, .job-name, [class*='title']");
            if (titleEl != null) {
                String jobName = titleEl.text().trim();
                if (jobName.length() >= 2) data.setJobName(jobName);
            }
        }

        // 公司名
        Element companyEl = item.selectFirst("a[class*='company'], .company-name, [class*='company'] a, .company");
        if (companyEl != null) {
            String cn = companyEl.text().trim();
            if (cn.length() >= 2 && !cn.contains("<")) data.setCompanyName(cn);
        }
        if (data.getCompanyName() == null) {
            Elements allText = item.select("span, p, div");
            for (Element el : allText) {
                String t = el.text().trim();
                if (t.length() >= 4 && t.length() <= 20 && !t.contains("<")
                    && !t.contains("职位") && !t.contains("招聘") && !t.contains("薪资")
                    && !t.contains("城市") && !t.contains("学历") && !t.contains("经验")) {
                    data.setCompanyName(t);
                    break;
                }
            }
        }

        // 城市
        Element cityEl = item.selectFirst("[class*='city'], [class*='area'], [class*='location'], .area");
        if (cityEl != null) data.setCity(extractCity(cityEl.text().trim()));

        // 薪资
        Element salaryEl = item.selectFirst("[class*='salary'], .salary, [class*='pay']");
        if (salaryEl != null) {
            data.setSalary(extractSalary(salaryEl.text()));
        }
        // 兜底：从全文中找薪资
        if (data.getSalary() == null) {
            data.setSalary(extractSalary(text));
        }

        // 学历
        Element eduEl = item.selectFirst("[class*='edu'], [class*='degree'], [class*='require']");
        if (eduEl != null) {
            data.setEducation(extractEducation(eduEl.text()));
        }
        // 应届生平台默认学历较低
        if (data.getEducation() == null) {
            data.setEducation(extractEducation(text));
        }

        // 经验：应届生平台多为无经验/在校学生
        Element expEl = item.selectFirst("[class*='exp'], [class*='experience'], [class*='work']");
        if (expEl != null) {
            data.setExperience(extractExperience(expEl.text()));
        }
        if (data.getExperience() == null) {
            data.setExperience(extractExperience(text));
        }
        // 教育部平台默认为应届生
        if (data.getExperience() == null) {
            data.setExperience("应届生");
        }

        // 公司规模
        Element scaleEl = item.selectFirst("[class*='scale'], [class*='size'], .company-scale");
        if (scaleEl != null) data.setCompanyScale(scaleEl.text().trim());

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
        String jn = data.getJobName();
        if (jn.contains("首页") || jn.contains("下一页") || jn.contains("上一页") || jn.contains("展开")) return null;
        if (data.getCompanyName() == null || data.getCompanyName().isEmpty()) {
            data.setCompanyName("未知单位");
        }
        return data;
    }

    private String extractCity(String raw) {
        if (raw == null) return null;
        raw = raw.trim().replace("异地招聘", "").replace("[切换城市]", "");
        return raw;
    }

    private String extractEducation(String text) {
        if (text.contains("博士")) return "博士";
        if (text.contains("硕士")) return "硕士";
        if (text.contains("本科")) return "本科";
        if (text.contains("大专")) return "大专";
        if (text.contains("学历不限")) return "学历不限";
        return null;
    }

    private String extractExperience(String text) {
        if (text.contains("无工作经验") || text.contains("无工作经历")) return "无经验";
        if (text.contains("在校学生") || text.contains("实习生")) return "在校学生";
        if (text.contains("应届生")) return "应届生";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d[\\-至~]\\d+年|\\d+年以上?|无经验)");
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
        String combined = keyword != null ? keyword : "";
        if (combined.contains("软件") || combined.contains("计算机") || combined.contains("IT")
            || combined.contains("前端") || combined.contains("后端") || combined.contains("全栈")) {
            return "互联网/IT";
        }
        if (combined.contains("算法") || combined.contains("人工智能") || combined.contains("AI")) {
            return "人工智能";
        }
        if (combined.contains("数据") && (combined.contains("分析") || combined.contains("开发"))) {
            return "数据服务";
        }
        if (combined.contains("机械") || combined.contains("制造") || combined.contains("工艺")) {
            return "机械/制造";
        }
        if (combined.contains("土木") || combined.contains("建筑") || combined.contains("施工")) {
            return "建筑/房地产";
        }
        if (combined.contains("电气") || combined.contains("自动化") || combined.contains("控制")) {
            return "电子/自动化";
        }
        if (combined.contains("化学") || combined.contains("化工") || combined.contains("材料")) {
            return "化工/材料";
        }
        if (combined.contains("金融") || combined.contains("银行") || combined.contains("投资")) {
            return "金融";
        }
        if (combined.contains("教育") || combined.contains("教师") || combined.contains("培训")) {
            return "教育培训";
        }
        if (combined.contains("地质") || combined.contains("测绘") || combined.contains("勘查")) {
            return "地质/矿产";
        }
        if (combined.contains("生物") || combined.contains("制药") || combined.contains("医学")) {
            return "生物医药";
        }
        if (combined.contains("新能源") || combined.contains("电池") || combined.contains("光伏")) {
            return "新能源";
        }
        if (combined.contains("物流") || combined.contains("供应链") || combined.contains("电商")) {
            return "交通物流";
        }
        return "教育培训";  // 教育部平台默认归类为教育培训
    }
}
