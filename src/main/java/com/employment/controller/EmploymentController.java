package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.EmploymentAttachment;
import com.employment.model.entity.EmploymentRecord;
import com.employment.model.entity.StudentInfo;
import com.employment.repository.EmploymentAttachmentRepository;
import com.employment.repository.EmploymentRecordRepository;
import com.employment.repository.StudentInfoRepository;
import com.employment.security.SecurityUtils;
import com.employment.service.StatsCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/employment")
@RequiredArgsConstructor
public class EmploymentController {

    private final EmploymentRecordRepository employmentRecordRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final EmploymentAttachmentRepository attachmentRepository;
    private final SecurityUtils securityUtils;
    private final StatsCacheService statsCacheService;

    @GetMapping("/my-record")
    public Result<EmploymentRecord> getMyRecord() {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId).orElse(null);
        if (studentInfo == null) {
            return Result.success(null);
        }
        List<EmploymentRecord> records = employmentRecordRepository.findByStudentId(studentInfo.getId());
        if (records.isEmpty()) {
            return Result.success(null);
        }
        // 每个学生只有一条最新记录（按时间倒序取最后一条）
        records.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });
        return Result.success(records.get(0));
    }

    @PostMapping("/record")
    @Transactional
    @OperationLog(module = "就业登记", content = "提交就业登记")
    public Result<EmploymentRecord> create(@RequestBody EmploymentRecord record) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "学生信息不存在"));

        // 每个学生只保留一条最新的就业去向记录
        // 如果已有记录，则更新该记录（而不是新建），确保就业统计准确性
        List<EmploymentRecord> existingRecords = employmentRecordRepository.findByStudentId(studentInfo.getId());
        EmploymentRecord target;
        if (!existingRecords.isEmpty()) {
            // 已有记录，更新为最新信息
            target = existingRecords.get(existingRecords.size() - 1);
        } else {
            // 无记录，创建新记录
            target = new EmploymentRecord();
            target.setStudentId(studentInfo.getId());
        }

        // 更新字段
        if (record.getEmploymentType() != null) target.setEmploymentType(record.getEmploymentType());
        if (record.getCompanyName() != null) target.setCompanyName(record.getCompanyName());
        if (record.getCompanyScale() != null) target.setCompanyScale(record.getCompanyScale());
        if (record.getCompanyIndustry() != null) target.setCompanyIndustry(record.getCompanyIndustry());
        if (record.getPositionName() != null) target.setPositionName(record.getPositionName());
        if (record.getPositionCategory() != null) target.setPositionCategory(record.getPositionCategory());
        if (record.getWorkCity() != null) target.setWorkCity(record.getWorkCity());
        if (record.getWorkProvince() != null) target.setWorkProvince(record.getWorkProvince());
        if (record.getSalary() != null) target.setSalary(record.getSalary());
        if (record.getIsThreePartySigned() != null) target.setIsThreePartySigned(record.getIsThreePartySigned());
        if (record.getThreePartyNo() != null) target.setThreePartyNo(record.getThreePartyNo());
        if (record.getContractStartDate() != null) target.setContractStartDate(record.getContractStartDate());
        if (record.getContractEndDate() != null) target.setContractEndDate(record.getContractEndDate());
        if (record.getProbationSalary() != null) target.setProbationSalary(record.getProbationSalary());
        if (record.getRemark() != null) target.setRemark(record.getRemark());

        // 每次登记都重置为待审核状态，重新审核（不清除ID，保持原有记录更新）
        target.setAuditStatus("pending");

        EmploymentRecord saved = employmentRecordRepository.save(target);
        statsCacheService.evictByStudent(studentInfo.getClassId(), studentInfo.getDeptId());
        return Result.success(saved);
    }

    @PutMapping("/record/{id}")
    @Transactional
    @OperationLog(module = "就业登记", content = "编辑就业登记")
    public Result<EmploymentRecord> update(@PathVariable Long id, @RequestBody EmploymentRecord record) {
        EmploymentRecord existing = employmentRecordRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "记录不存在"));
        if (record.getEmploymentType() != null) existing.setEmploymentType(record.getEmploymentType());
        if (record.getCompanyName() != null) existing.setCompanyName(record.getCompanyName());
        if (record.getCompanyScale() != null) existing.setCompanyScale(record.getCompanyScale());
        if (record.getCompanyIndustry() != null) existing.setCompanyIndustry(record.getCompanyIndustry());
        if (record.getPositionName() != null) existing.setPositionName(record.getPositionName());
        if (record.getPositionCategory() != null) existing.setPositionCategory(record.getPositionCategory());
        if (record.getWorkCity() != null) existing.setWorkCity(record.getWorkCity());
        if (record.getWorkProvince() != null) existing.setWorkProvince(record.getWorkProvince());
        if (record.getSalary() != null) existing.setSalary(record.getSalary());
        if (record.getIsThreePartySigned() != null) existing.setIsThreePartySigned(record.getIsThreePartySigned());
        if (record.getThreePartyNo() != null) existing.setThreePartyNo(record.getThreePartyNo());
        if (record.getContractStartDate() != null) existing.setContractStartDate(record.getContractStartDate());
        if (record.getContractEndDate() != null) existing.setContractEndDate(record.getContractEndDate());
        if (record.getProbationSalary() != null) existing.setProbationSalary(record.getProbationSalary());
        if (record.getRemark() != null) existing.setRemark(record.getRemark());
        existing.setAuditStatus("pending");
        EmploymentRecord saved = employmentRecordRepository.save(existing);
        StudentInfo stu = studentInfoRepository.findById(existing.getStudentId()).orElse(null);
        if (stu != null) {
            statsCacheService.evictByStudent(stu.getClassId(), stu.getDeptId());
        }
        return Result.success(saved);
    }

    @GetMapping("/record/{id}/status")
    public Result<EmploymentRecord> getStatus(@PathVariable Long id) {
        return employmentRecordRepository.findById(id)
                .map(Result::success)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "记录不存在"));
    }

    @PostMapping("/record/{id}/attachment")
    @Transactional
    @OperationLog(module = "就业登记", content = "上传附件")
    public Result<EmploymentAttachment> uploadAttachment(@PathVariable Long id, @RequestParam String attachmentType, @RequestParam("file") MultipartFile file) throws IOException {
        EmploymentRecord record = employmentRecordRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "记录不存在"));

        Long userId = securityUtils.getCurrentUserId();
        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "学生信息不存在"));

        if (!record.getStudentId().equals(studentInfo.getId())) {
            throw new com.employment.exception.BusinessException(403, "无权操作");
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/employment/";
        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String savedFilename = UUID.randomUUID().toString() + extension;
        Path filePath = dirPath.resolve(savedFilename);
        Files.write(filePath, file.getBytes());

        EmploymentAttachment attachment = new EmploymentAttachment();
        attachment.setEmploymentId(id);
        attachment.setAttachmentType(attachmentType);
        attachment.setAttachmentName(originalFilename);
        attachment.setFilePath("/uploads/employment/" + savedFilename);
        attachment.setFileSize(file.getSize());
        attachment.setUploadStatus("uploaded");

        return Result.success(attachmentRepository.save(attachment));
    }

    @GetMapping("/record/{id}/attachments")
    public Result<List<EmploymentAttachment>> getAttachments(@PathVariable Long id) {
        return Result.success(attachmentRepository.findByEmploymentId(id));
    }
}
