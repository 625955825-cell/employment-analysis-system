package com.employment.service.impl;

import com.employment.exception.BusinessException;
import com.employment.model.entity.StudentInfo;
import com.employment.model.entity.StudentResume;
import com.employment.repository.StudentInfoRepository;
import com.employment.repository.StudentResumeRepository;
import com.employment.service.ResumeExportService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExportServiceImpl implements ResumeExportService {

    private final StudentResumeRepository resumeRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final Environment env;

    // ========== 配色方案 ==========
    private static final Color PRIMARY = new Color(47, 107, 255);    // #2f6bff
    private static final Color DARK_BLUE = new Color(15, 42, 95);   // #0f2a5f
    private static final Color BODY_TEXT = new Color(55, 65, 81);    // #374151
    private static final Color SECONDARY_TEXT = new Color(107, 114, 128); // #6b7280
    private static final Color LIGHT_BORDER = new Color(229, 234, 243);   // #e5eaf3
    private static final Color LIGHT_BLUE_BG = new Color(245, 249, 255);  // #f5f9ff
    private static final Color SECTION_BG = new Color(234, 242, 255);     // #eaf2ff

    @Override
    public byte[] exportToPdf(Long resumeId, Long userId) {
        StudentResume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new BusinessException(404, "简历不存在"));

        Long studentIdFromResume = resume.getStudentId();
        StudentInfo studentInfo = studentInfoRepository.findById(studentIdFromResume).orElse(null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 44, 44, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont bfChinese = getBaseFont();
            BaseFont bfChineseBold = getBaseFontBold();

            // ========== 字体定义 ==========
            Font nameFont = new Font(bfChineseBold, 22, Font.BOLD, DARK_BLUE);
            Font taglineFont = new Font(bfChinese, 10, Font.NORMAL, SECONDARY_TEXT);
            Font contactFont = new Font(bfChinese, 9, Font.NORMAL, SECONDARY_TEXT);
            Font sectionTitleFont = new Font(bfChineseBold, 11, Font.BOLD, PRIMARY);
            Font subTitleFont = new Font(bfChineseBold, 10, Font.BOLD, DARK_BLUE);
            Font bodyFont = new Font(bfChinese, 9.5f, Font.NORMAL, BODY_TEXT);
            Font labelFont = new Font(bfChinese, 8.5f, Font.NORMAL, SECONDARY_TEXT);
            Font footerFont = new Font(bfChinese, 8, Font.NORMAL, SECONDARY_TEXT);

            // ========== 头部区域 ==========
            addHeader(document, resume, studentInfo, nameFont, taglineFont, contactFont);

            // ========== 分割线 ==========
            addSeparatorLine(document, LIGHT_BORDER);
            document.add(new Paragraph(" "));

            // ========== 求职意向 ==========
            if (hasExpectation(resume)) {
                addExpectationSection(document, resume, sectionTitleFont, subTitleFont, bodyFont, labelFont);
            }

            // ========== 个人简介 ==========
            if (hasContent(resume.getPersonalSummary())) {
                addSection(document, "个人简介", sectionTitleFont);
                addBodyText(document, resume.getPersonalSummary(), bodyFont);
            }

            // ========== 教育经历 ==========
            if (hasContent(resume.getEducationExperience())) {
                addSection(document, "教育经历", sectionTitleFont);
                addBodyText(document, resume.getEducationExperience(), bodyFont);
            }

            // ========== 项目经验 ==========
            if (hasContent(resume.getProjectExperience())) {
                addSection(document, "项目经验", sectionTitleFont);
                addMultiBlockContent(document, resume.getProjectExperience(), subTitleFont, bodyFont);
            }

            // ========== 工作/实习经历 ==========
            if (hasContent(resume.getWorkExperience())) {
                addSection(document, "工作/实习经历", sectionTitleFont);
                addBodyText(document, resume.getWorkExperience(), bodyFont);
            }

            // ========== 技能证书 ==========
            if (hasContent(resume.getSkillCertificates())) {
                addSection(document, "技能证书", sectionTitleFont);
                addBodyText(document, resume.getSkillCertificates(), bodyFont);
            }

            // ========== 获奖荣誉 ==========
            if (hasContent(resume.getAwardsHonors())) {
                addSection(document, "获奖荣誉", sectionTitleFont);
                addBodyText(document, resume.getAwardsHonors(), bodyFont);
            }

            // ========== 自我评价 ==========
            if (hasContent(resume.getSelfEvaluation())) {
                addSection(document, "自我评价", sectionTitleFont);
                addBodyText(document, resume.getSelfEvaluation(), bodyFont);
            }

            // ========== 底部 ==========
            addFooter(document, footerFont);

            document.close();
            log.info("简历PDF导出成功: resumeId={}", resumeId);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("简历PDF导出失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "PDF导出失败: " + e.getMessage());
        }
    }

    private BaseFont getBaseFont() {
        String[] paths = {
            "C:\\Windows\\Fonts\\simsun.ttc,0",
            "C:\\Windows\\Fonts\\msyh.ttc,0",
            "C:\\Windows\\Fonts\\simhei.ttf",
            "C:\\Windows\\Fonts\\simsun.ttf"
        };
        for (String p : paths) {
            try {
                return BaseFont.createFont(p, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {}
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception ex) {
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private BaseFont getBaseFontBold() {
        return getBaseFont();
    }

    // ========== 头部区域（双列表格：左侧信息 + 右侧头像） ==========
    private void addHeader(Document document, StudentResume resume, StudentInfo info,
                           Font nameFont, Font taglineFont, Font contactFont) throws DocumentException {

        // 整体两列：左侧信息（占主要宽度），右侧头像
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{6.5f, 1f});
        headerTable.setSpacingAfter(8);

        // ---- 左侧单元格：姓名 + 标签行 + 联系方式 ----
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);
        leftCell.setVerticalAlignment(Element.ALIGN_TOP);

        String name = info != null ? nvl(info.getRealName()) : "学生姓名";
        Paragraph namePara = new Paragraph(name, nameFont);
        namePara.setSpacingAfter(4);
        leftCell.addElement(namePara);

        // 蓝色短装饰线（姓名下方）
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(40);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setBorderWidthBottom(3f);
        lineCell.setBorderColorBottom(PRIMARY);
        lineCell.setFixedHeight(8);
        lineTable.addCell(lineCell);
        leftCell.addElement(lineTable);

        // 身份标签行
        StringBuilder tagLine = new StringBuilder();
        if (info != null) {
            if (hasContent(info.getMajorName())) tagLine.append(nvl(info.getMajorName())).append("  ·  ");
            if (info.getGraduationYear() != null) tagLine.append(info.getGraduationYear()).append("届  ·  本科");
        }
        if (tagLine.length() > 0) {
            Paragraph tag = new Paragraph(tagLine.toString(), taglineFont);
            tag.setSpacingBefore(5);
            tag.setSpacingAfter(2);
            leftCell.addElement(tag);
        }

        // 联系方式行
        StringBuilder contactLine = new StringBuilder();
        if (info != null) {
            if (hasContent(info.getPhone())) contactLine.append(nvl(info.getPhone())).append("  ·  ");
            if (hasContent(info.getEmail())) contactLine.append(nvl(info.getEmail())).append("  ·  ");
            if (hasContent(info.getProvince())) contactLine.append(nvl(info.getProvince()));
            if (hasContent(info.getCity())) contactLine.append(nvl(info.getCity()));
        }
        if (contactLine.length() > 0) {
            String contactText = contactLine.toString();
            if (contactText.endsWith("  ·  ")) {
                contactText = contactText.substring(0, contactText.length() - 4);
            }
            Paragraph contact = new Paragraph(contactText.trim(), contactFont);
            contact.setSpacingAfter(0);
            leftCell.addElement(contact);
        }

        headerTable.addCell(leftCell);

        // ---- 右侧单元格：头像 ----
        PdfPCell avatarCell = new PdfPCell();
        avatarCell.setBorder(Rectangle.NO_BORDER);
        avatarCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        avatarCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        avatarCell.setPaddingTop(8);

        String avatarPath = (info != null) ? info.getAvatar() : null;
        if (hasContent(avatarPath)) {
            try {
                Image avatarImg = fetchAvatarImage(avatarPath);
                if (avatarImg != null) {
                    // 头像缩放到 85x85，圆形通过叠加边框实现
                    avatarImg.scaleToFit(85, 85);
                    avatarCell.addElement(avatarImg);
                } else {
                    avatarCell.addElement(makeAvatarPlaceholder(name));
                }
            } catch (Exception e) {
                log.warn("头像加载失败: {}", avatarPath);
                avatarCell.addElement(makeAvatarPlaceholder(name));
            }
        } else {
            avatarCell.addElement(makeAvatarPlaceholder(name));
        }

        headerTable.addCell(avatarCell);
        document.add(headerTable);
    }

    // 用表格绘制圆形头像占位（白色大圆 + 蓝色细边框 + 浅蓝底 + 姓名首字）
    private PdfPTable makeAvatarPlaceholder(String name) {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(60);
        outer.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell outerCell = new PdfPCell();
        outerCell.setBorder(Rectangle.NO_BORDER);
        outerCell.setBackgroundColor(SECTION_BG);
        outerCell.setBorderWidth(2f);
        outerCell.setBorderColor(PRIMARY);
        outerCell.setFixedHeight(85);
        outerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        outerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String initial = name != null && !name.isEmpty() ? name.substring(0, 1) : "学";
        Font initialFont = new Font(getBaseFont(), 24, Font.BOLD, PRIMARY);
        Paragraph p = new Paragraph(initial, initialFont);
        p.setAlignment(Element.ALIGN_CENTER);
        outerCell.addElement(p);

        outer.addCell(outerCell);
        return outer;
    }

    // ========== 分隔线（用表格实现，不依赖 document.top()） ==========
    private void addSeparatorLine(Document document, Color color) throws DocumentException {
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        lineTable.setSpacingBefore(4);
        lineTable.setSpacingAfter(8);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setBorderWidthBottom(0.5f);
        lineCell.setBorderColorBottom(color);
        lineCell.setFixedHeight(4);
        lineTable.addCell(lineCell);

        document.add(lineTable);
    }

    // ========== 区块标题（左侧蓝竖条 + 标题） ==========
    private void addSection(Document document, String title, Font sectionTitleFont) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(5);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);

        PdfPTable innerTable = new PdfPTable(2);
        innerTable.setWidthPercentage(100);
        innerTable.setWidths(new float[]{1f, 9f});

        // 蓝竖条
        PdfPCell barCell = new PdfPCell();
        barCell.setBorder(Rectangle.NO_BORDER);
        barCell.setBorderWidthLeft(3f);
        barCell.setBorderColorLeft(PRIMARY);
        barCell.setPadding(0);
        barCell.setPaddingLeft(2);
        barCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        barCell.setFixedHeight(16);
        innerTable.addCell(barCell);

        // 标题文字
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setPaddingTop(1);
        textCell.setPaddingBottom(4);
        Paragraph titlePara = new Paragraph(title, sectionTitleFont);
        titlePara.setSpacingAfter(0);
        textCell.addElement(titlePara);
        innerTable.addCell(textCell);

        cell.addElement(innerTable);

        // 标题下方浅灰分割线
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setBorderWidthBottom(0.5f);
        lineCell.setBorderColorBottom(LIGHT_BORDER);
        lineCell.setFixedHeight(6);
        lineTable.addCell(lineCell);
        cell.addElement(lineTable);

        table.addCell(cell);
        document.add(table);
    }

    // ========== 求职意向（卡片式横排） ==========
    private void addExpectationSection(Document document, StudentResume resume,
                                       Font sectionTitleFont, Font subTitleFont, Font bodyFont, Font labelFont) throws DocumentException {
        addSection(document, "求职意向", sectionTitleFont);

        PdfPTable cardTable = new PdfPTable(4);
        cardTable.setWidthPercentage(100);
        cardTable.setSpacingBefore(2);
        cardTable.setSpacingAfter(6);
        cardTable.setWidths(new float[]{1f, 1f, 1f, 1f});

        if (hasContent(resume.getExpectedPosition())) {
            addExpectationCard(cardTable, "期望岗位", resume.getExpectedPosition(), subTitleFont, labelFont);
        }
        if (hasContent(resume.getExpectedCity())) {
            addExpectationCard(cardTable, "期望城市", resume.getExpectedCity(), subTitleFont, labelFont);
        }
        if (hasContent(resume.getExpectedIndustry())) {
            addExpectationCard(cardTable, "期望行业", resume.getExpectedIndustry(), subTitleFont, labelFont);
        }
        if (resume.getExpectedSalaryMin() != null && resume.getExpectedSalaryMax() != null) {
            addExpectationCard(cardTable, "期望薪资",
                resume.getExpectedSalaryMin() + "-" + resume.getExpectedSalaryMax() + "元/月",
                subTitleFont, labelFont);
        }

        // 补充空白格子
        int filledCount = 0;
        if (hasContent(resume.getExpectedPosition())) filledCount++;
        if (hasContent(resume.getExpectedCity())) filledCount++;
        if (hasContent(resume.getExpectedIndustry())) filledCount++;
        if (resume.getExpectedSalaryMin() != null && resume.getExpectedSalaryMax() != null) filledCount++;
        for (int i = 0; i < 4 - filledCount; i++) {
            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setBorder(Rectangle.NO_BORDER);
            cardTable.addCell(emptyCell);
        }

        document.add(cardTable);
    }

    private void addExpectationCard(PdfPTable table, String label, String value, Font subTitleFont, Font labelFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(LIGHT_BLUE_BG);
        cell.setPadding(8);
        cell.setBorderColor(LIGHT_BORDER);
        cell.setBorderWidth(0.5f);

        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        PdfPCell labelCell = new PdfPCell();
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(4);
        Paragraph labelPara = new Paragraph(label, labelFont);
        labelCell.addElement(labelPara);
        inner.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell();
        valueCell.setBorder(Rectangle.NO_BORDER);
        Paragraph valuePara = new Paragraph(value, subTitleFont);
        valueCell.addElement(valuePara);
        inner.addCell(valueCell);

        cell.addElement(inner);
        table.addCell(cell);
    }

    // ========== 普通正文文本 ==========
    private void addBodyText(Document document, String content, Font bodyFont) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        cell.setLeading(1.6f, 1.6f);

        String[] lines = content.trim().split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                Paragraph spacer = new Paragraph(" ");
                spacer.setSpacingAfter(0);
                cell.addElement(spacer);
                continue;
            }
            Paragraph p = new Paragraph(line, bodyFont);
            p.setSpacingBefore(0);
            p.setSpacingAfter(0);
            cell.addElement(p);
        }

        table.addCell(cell);
        document.add(table);
    }

    // ========== 多项目内容（项目经验等，支持子标题） ==========
    private void addMultiBlockContent(Document document, String content,
                                      Font subTitleFont, Font bodyFont) throws DocumentException {
        String[] lines = content.trim().split("\n");
        boolean inBlock = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                inBlock = false;
                continue;
            }

            // 判断是否为子标题行（以 | 分隔的第一部分，或以特定符号开头的行）
            if (trimmed.contains("|") || trimmed.startsWith("【") || trimmed.startsWith("【") && trimmed.contains("】")) {
                // 项目名称子标题
                String projectTitle = trimmed.contains("|")
                    ? trimmed.substring(0, trimmed.indexOf("|")).trim()
                    : trimmed.replaceAll("【|】", "").trim();

                PdfPTable block = new PdfPTable(1);
                block.setWidthPercentage(100);
                block.setSpacingBefore(6);
                block.setSpacingAfter(4);

                PdfPCell blockCell = new PdfPCell();
                blockCell.setBorder(Rectangle.NO_BORDER);
                blockCell.setBackgroundColor(SECTION_BG);
                blockCell.setPadding(10);
                blockCell.setLeading(1.5f, 1.5f);

                // 项目名标题
                Paragraph titleP = new Paragraph(projectTitle, subTitleFont);
                titleP.setSpacingBefore(0);
                titleP.setSpacingAfter(4);
                blockCell.addElement(titleP);

                // 后续行作为项目描述
                int nextIdx = -1;
                for (int j = 0; j < lines.length; j++) {
                    if (lines[j].trim().equals(trimmed)) {
                        nextIdx = j + 1;
                        break;
                    }
                }
                if (nextIdx >= 0 && nextIdx < lines.length) {
                    StringBuilder descBuilder = new StringBuilder();
                    for (int k = nextIdx; k < lines.length; k++) {
                        String nextLine = lines[k].trim();
                        if (nextLine.isEmpty()) break;
                        if (nextLine.contains("|") || nextLine.startsWith("【")) break;
                        if (descBuilder.length() > 0) descBuilder.append("；");
                        descBuilder.append(nextLine.replaceFirst("^[\\-–—\\s]+", ""));
                    }
                    if (descBuilder.length() > 0) {
                        Paragraph descP = new Paragraph(descBuilder.toString(), bodyFont);
                        descP.setSpacingBefore(0);
                        descP.setSpacingAfter(0);
                        blockCell.addElement(descP);
                    }
                }

                block.addCell(blockCell);
                document.add(block);
            } else {
                // 普通文本行
                PdfPTable lineTable = new PdfPTable(1);
                lineTable.setWidthPercentage(100);
                lineTable.setSpacingAfter(2);

                PdfPCell lineCell = new PdfPCell();
                lineCell.setBorder(Rectangle.NO_BORDER);
                lineCell.setPadding(0);
                lineCell.setLeading(1.5f, 1.5f);

                // 无序圆点
                PdfPTable bulletTable = new PdfPTable(2);
                bulletTable.setWidthPercentage(100);
                bulletTable.setWidths(new float[]{0.5f, 9.5f});

                PdfPCell dotCell = new PdfPCell();
                dotCell.setBorder(Rectangle.NO_BORDER);
                dotCell.setPadding(0);
                dotCell.setPaddingTop(5);
                dotCell.addElement(new Paragraph("·", bodyFont));
                bulletTable.addCell(dotCell);

                PdfPCell textCell = new PdfPCell();
                textCell.setBorder(Rectangle.NO_BORDER);
                textCell.setPadding(0);
                textCell.addElement(new Paragraph(trimmed, bodyFont));
                bulletTable.addCell(textCell);

                lineCell.addElement(bulletTable);
                lineTable.addCell(lineCell);
                document.add(lineTable);
            }
        }
    }

    // ========== 底部 ==========
    private void addFooter(Document document, Font footerFont) throws DocumentException {
        document.add(new Paragraph(" "));
        addSeparatorLine(document, LIGHT_BORDER);
        document.add(new Paragraph(" "));

        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph footer = new Paragraph("高校就业数据综合分析平台", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingAfter(2);
        cell.addElement(footer);

        String date = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        Paragraph datePara = new Paragraph(date, footerFont);
        datePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(datePara);

        footerTable.addCell(cell);
        document.add(footerTable);
    }

    private boolean hasContent(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private boolean hasExpectation(StudentResume resume) {
        return hasContent(resume.getExpectedPosition()) || hasContent(resume.getExpectedCity()) ||
               hasContent(resume.getExpectedIndustry()) ||
               (resume.getExpectedSalaryMin() != null && resume.getExpectedSalaryMax() != null);
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private Image fetchAvatarImage(String avatarPath) {
        try {
            String serverUrl = env.getProperty("server.address", "localhost")
                    + ":" + env.getProperty("server.port", "8080");
            String imageUrl;
            if (avatarPath.startsWith("http")) {
                imageUrl = avatarPath;
            } else if (avatarPath.startsWith("/uploads/")) {
                imageUrl = "http://" + serverUrl + avatarPath;
            } else {
                imageUrl = "http://" + serverUrl + "/uploads/" + avatarPath;
            }
            return Image.getInstance(new java.net.URL(imageUrl));
        } catch (Exception e) {
            log.warn("获取头像图片失败: {}", avatarPath);
            return null;
        }
    }
}
