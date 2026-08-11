package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.dto.ResumeDTO;
import com.employment.security.SecurityUtils;
import com.employment.service.ResumeExportService;
import com.employment.service.ResumeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/student/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeExportService resumeExportService;
    private final SecurityUtils securityUtils;

    @GetMapping("/list")
    public Result<List<ResumeDTO>> getMyResumes() {
        return Result.success(resumeService.getMyResumes());
    }

    @GetMapping("/{id}")
    public Result<ResumeDTO> getResume(@PathVariable Long id) {
        return Result.success(resumeService.getResumeById(id));
    }

    @PostMapping
    @OperationLog(module = "简历管理", content = "创建简历")
    public Result<ResumeDTO> createResume(@RequestBody ResumeDTO dto) {
        return Result.success("创建成功", resumeService.createResume(dto));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "简历管理", content = "更新简历")
    public Result<ResumeDTO> updateResume(@PathVariable Long id, @RequestBody ResumeDTO dto) {
        return Result.success("更新成功", resumeService.updateResume(id, dto));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "简历管理", content = "删除简历")
    public Result<Void> deleteResume(@PathVariable Long id) {
        resumeService.deleteResume(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        resumeService.setDefaultResume(id);
        return Result.success("设置成功", null);
    }

    @GetMapping("/{id}/export")
    public void exportPdf(@PathVariable Long id, HttpServletResponse response) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            byte[] pdfBytes = resumeExportService.exportToPdf(id, userId);

            ResumeDTO resume = resumeService.getResumeById(id);
            String fileName = (resume.getResumeName() != null ? resume.getResumeName() : "简历") + ".pdf";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
            response.setContentLength(pdfBytes.length);

            OutputStream out = response.getOutputStream();
            out.write(pdfBytes);
            out.flush();
        } catch (Exception e) {
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"PDF导出失败: " + e.getMessage() + "\"}");
                response.getWriter().flush();
            } catch (Exception ignored) {}
        }
    }
}
