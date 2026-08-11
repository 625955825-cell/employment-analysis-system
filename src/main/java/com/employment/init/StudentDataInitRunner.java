package com.employment.init;

import com.employment.repository.CompanyInfoRepository;
import com.employment.repository.JobPositionRepository;
import com.employment.repository.SysClassRepository;
import com.employment.service.StudentDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * 学生数据初始化入口
 * 项目启动时自动运行，检测到锁文件则跳过
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentDataInitRunner implements CommandLineRunner {

    private static final String LOCK_FILE = "student_init_done.lock";

    private final StudentDataService studentDataService;
    private final SysClassRepository sysClassRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final JobPositionRepository jobPositionRepository;

    @Override
    public void run(String... args) {
        if (new File(LOCK_FILE).exists()) {
            log.info("[StudentDataInit] 检测到 {} 文件，学生数据已初始化，跳过。", LOCK_FILE);
            return;
        }

        // 检查前置数据
        long classCount = sysClassRepository.count();
        if (classCount < 100) {
            log.warn("[StudentDataInit] sys_class 表只有 {} 条记录（期望约800条），请先运行班级导入脚本，跳过初始化。", classCount);
            return;
        }
        long companyCount = companyInfoRepository.count();
        if (companyCount < 10) {
            log.warn("[StudentDataInit] company_info 表只有 {} 条记录（期望约200家），请先运行企业导入脚本，跳过初始化。", companyCount);
            return;
        }
        long jobCount = jobPositionRepository.count();
        if (jobCount < 10) {
            log.warn("[StudentDataInit] job_position 表只有 {} 条记录（期望约585个），请先运行职位导入脚本，跳过初始化。", jobCount);
            return;
        }

        log.info("========================================");
        log.info("[StudentDataInit] 开始初始化学生数据（约33,600人）...");
        log.info("  班级数: {}", classCount);
        log.info("  企业数: {}", companyCount);
        log.info("  职位数: {}", jobCount);
        log.info("========================================");

        try {
            studentDataService.initializeAllStudents();
            writeLockFile();
            log.info("[StudentDataInit] 学生数据初始化完成！");
        } catch (Exception e) {
            log.error("[StudentDataInit] 学生数据初始化失败: {}", e.getMessage(), e);
        }
    }

    private void writeLockFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOCK_FILE))) {
            pw.println("Student data initialized at: " + LocalDateTime.now());
            pw.println("This file prevents re-initialization on next startup.");
            pw.println("Delete this file to re-run initialization.");
        } catch (Exception e) {
            log.warn("[StudentDataInit] 写入锁文件失败: {}", e.getMessage());
        }
    }
}
