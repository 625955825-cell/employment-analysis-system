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
 * 前程无忧（51job）职位数据提取器。
 *
 * HTML 结构说明：
 * - 经典版：search.jobs.51job.com → <div class="el"> 结构
 *   每条职位: <div class="el"> → <p class="t1"> 职位名, <span class="t2"> 公司名, <span class="t3"> 城市,
 *            <span class="t4"> 薪资, <div class="d"> 学历|经验
 *
 * - 新版：we.51job.com → <div class="j_joblist"> 结构（当 WebClient JSON API 不可用时的兜底）
 *   每条职位: <div class="j_joblist"> → <a class="t1 job-title"> 职位, <a class="t2 company"> 公司,
 *            <span class="t3 city"> 城市, <span class="t4 salary"> 薪资, <div class="d"> 学历|经验
 *
 * 训练推荐算法所需字段：jobName, companyName, salary, city, education, experience, companyScale, industry, skills
 */
@Slf4j
@Component
public class Job51jobExtractor implements SiteDataExtractor {

    @Override
    public String getSourceCode() {
        return "51job";
    }

    @Override
    public List<SpiderCollectedData> extract(Document doc, String sourceCode, String major, String keyword) {
        List<SpiderCollectedData> result = new ArrayList<>();

        // 方案1：经典版 div.el 结构
        Elements items = doc.select("div.el");
        if (!items.isEmpty()) {
            log.debug("使用经典版 div.el 结构，命中 {} 条", items.size());
            for (Element item : items) {
                SpiderCollectedData data = parseClassicEl(item, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            return result;
        }

        // 方案2：新版 j_joblist 结构
        items = doc.select("div.j_joblist");
        if (!items.isEmpty()) {
            log.debug("使用新版 j_joblist 结构，命中 {} 条", items.size());
            for (Element item : items) {
                SpiderCollectedData data = parseNewJobList(item, sourceCode, major, keyword);
                if (data != null) result.add(data);
            }
            return result;
        }

        log.warn("前程无忧页面中未找到任何已知职位列表结构");
        return result;
    }

    /**
     * 解析经典版 div.el 结构。
     * 结构：<div class="el"> → p.t1(职位) / span.t2(公司) / span.t3(城市) / span.t4(薪资) / div.d(学历|经验)
     */
    private SpiderCollectedData parseClassicEl(Element item, String sourceCode, String major, String keyword) {
        // 过滤表头等非数据行
        String text = item.text();
        if (text.length() < 15) return null;

        SpiderCollectedData data = new SpiderCollectedData();
        data.setSourceCode(sourceCode);
        data.setDataType("job");
        data.setMajorName(major);
        data.setIndustryKeyword(keyword);
        fillCommonFields(item, data, keyword);

        // 职位名：p.t1 a
        Element jobLink = item.selectFirst("p.t1 a");
        if (jobLink != null) {
            String jobName = jobLink.text().trim();
            if (jobName.length() < 2) return null;
            data.setJobName(jobName);
        }

        // 公司名：span.t2 a
        Element companyLink = item.selectFirst("span.t2 a");
        if (companyLink != null) {
            data.setCompanyName(companyLink.text().trim());
        }

        // 城市：span.t3
        Element cityEl = item.selectFirst("span.t3");
        if (cityEl != null) data.setCity(extractCity(cityEl.text().trim()));

        // 薪资：span.t4
        Element salaryEl = item.selectFirst("span.t4");
        if (salaryEl != null) data.setSalary(salaryEl.text().trim());

        // 学历+经验：div.d
        Element detailEl = item.selectFirst("div.d");
        if (detailEl != null) {
            String detailText = detailEl.text();
            data.setEducation(extractEducation(detailText));
            data.setExperience(extractExperience(detailText));
        }

        if (data.getJobName() == null) return null;
        if (data.getCompanyName() == null) data.setCompanyName("未知公司");

        return data;
    }

    /**
     * 解析新版 j_joblist 结构。
     * 结构：<div class="j_joblist"> → a.t1(职位) / a.t2(公司) / span.t3(城市) / span.t4(薪资) / div.d(学历|经验) / span.sspan(规模)
     */
    private SpiderCollectedData parseNewJobList(Element item, String sourceCode, String major, String keyword) {
        String text = item.text();
        if (text.length() < 15) return null;

        SpiderCollectedData data = new SpiderCollectedData();
        data.setSourceCode(sourceCode);
        data.setDataType("job");
        data.setMajorName(major);
        data.setIndustryKeyword(keyword);
        fillCommonFields(item, data, keyword);

        // 职位名
        Element jobLink = item.selectFirst("a.t1");
        if (jobLink != null) {
            String jobName = jobLink.text().trim();
            if (jobName.length() < 2) return null;
            data.setJobName(jobName);
        }

        // 公司名
        Element companyLink = item.selectFirst("a.t2");
        if (companyLink != null) data.setCompanyName(companyLink.text().trim());

        // 城市
        Element cityEl = item.selectFirst("span.t3");
        if (cityEl != null) data.setCity(extractCity(cityEl.text().trim()));

        // 薪资
        Element salaryEl = item.selectFirst("span.t4");
        if (salaryEl != null) data.setSalary(salaryEl.text().trim());

        // 学历+经验
        Element detailEl = item.selectFirst("div.d");
        if (detailEl != null) {
            String detailText = detailEl.text();
            data.setEducation(extractEducation(detailText));
            data.setExperience(extractExperience(detailText));
        }

        // 公司规模
        Element scaleEl = item.selectFirst("span.sspan");
        if (scaleEl != null) data.setCompanyScale(scaleEl.text().trim());

        if (data.getJobName() == null) return null;
        if (data.getCompanyName() == null) data.setCompanyName("未知公司");

        return data;
    }

    private void fillCommonFields(Element item, SpiderCollectedData data, String keyword) {
        data.setCollectTime(java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.setIndustry(inferIndustry(keyword, data.getJobName()));
        data.setIsSynced("0");
    }

    private String extractCity(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        // 过滤 "异地招聘" 等干扰
        if (raw.contains("异")) return raw.replace("异地招聘", "").trim();
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
        if (text.contains("无工作经验")) return "无经验";
        if (text.contains("无工作经历")) return "无经验";
        if (text.contains("在校学生")) return "在校学生";
        // 匹配 "1-3年" / "3-4年" / "5-10年" 等格式
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d[\\-至~]\\d+年|\\d+年以上?|无经验)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) return m.group().trim();
        return null;
    }

    private String inferIndustry(String keyword, String jobName) {
        String combined = (keyword != null ? keyword : "") + (jobName != null ? jobName : "");
        if (combined.contains("Java") || combined.contains("Python") || combined.contains("前端")
            || combined.contains("后端") || combined.contains("全栈") || combined.contains("Web")) {
            return "互联网/IT";
        }
        if (combined.contains("算法") || combined.contains("机器学习") || combined.contains("深度学习")
            || combined.contains("NLP") || combined.contains("AI")) {
            return "人工智能";
        }
        if (combined.contains("数据") && (combined.contains("分析") || combined.contains("挖掘"))) {
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
        if (combined.contains("生物") || combined.contains("制药") || combined.contains("医药")) {
            return "生物医药";
        }
        if (combined.contains("新能源") || combined.contains("电池") || combined.contains("储能")) {
            return "新能源";
        }
        if (combined.contains("物流") || combined.contains("供应链") || combined.contains("运输")) {
            return "交通物流";
        }
        return "其他";
    }
}
