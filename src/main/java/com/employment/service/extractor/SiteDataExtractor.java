package com.employment.service.extractor;

import com.employment.model.entity.SpiderCollectedData;
import org.jsoup.nodes.Document;

import java.util.List;

/**
 * 各招聘网站职位数据提取器接口。
 * 每个网站有独立的提取器，负责将原始 HTML 解析为结构化职位数据。
 * 这样设计的好处：
 * 1. 每个站点解析逻辑独立，互不干扰，便于调试和维护
 * 2. 解析失败时能精确定位问题，而不是多选择器兜底导致数据混乱
 * 3. 便于后续扩展新的数据源
 */
public interface SiteDataExtractor {

    /**
     * 从解析后的 Document 中提取职位列表。
     *
     * @param doc      Jsoup 解析后的 HTML 文档
     * @param sourceCode 数据源代码
     * @param major     所属专业
     * @param keyword   搜索关键词
     * @return 提取到的职位数据列表（永不为 null）
     */
    List<SpiderCollectedData> extract(Document doc, String sourceCode, String major, String keyword);

    /**
     * 获取该提取器对应的数据源代码。
     */
    String getSourceCode();

    /**
     * 从详情页 HTML 中提取完整的职位信息。
     * 用于两阶段爬取：搜索页获取列表 → 逐条访问详情页获取完整字段。
     *
     * @param detailDoc  Jsoup 解析后的详情页 HTML 文档
     * @param sourceCode 数据源代码
     * @param major     所属专业
     * @param keyword   搜索关键词
     * @param searchJobName 搜索页记录的职位名称（用于关联上下文）
     * @param detailUrl 详情页 URL
     * @return 提取到的完整职位数据；返回 null 表示该详情页不可用或解析失败
     */
    default SpiderCollectedData extractFromDetail(Document detailDoc, String sourceCode,
            String major, String keyword, String searchJobName, String detailUrl) {
        return null;
    }
}
