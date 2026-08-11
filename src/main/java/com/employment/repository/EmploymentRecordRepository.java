package com.employment.repository;

import com.employment.model.entity.EmploymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmploymentRecordRepository extends JpaRepository<EmploymentRecord, Long> {
    List<EmploymentRecord> findByStudentId(Long studentId);
    List<EmploymentRecord> findByStudentIdIn(List<Long> studentIds);
    List<EmploymentRecord> findByAuditStatus(String auditStatus);

    @Query("SELECT e.employmentType, COUNT(e) FROM EmploymentRecord e GROUP BY e.employmentType")
    List<Object[]> countByEmploymentType();

    @Query("SELECT e.workProvince, COUNT(e) FROM EmploymentRecord e GROUP BY e.workProvince ORDER BY COUNT(e) DESC")
    List<Object[]> countByWorkProvince();

    @Query("SELECT e.companyIndustry, COUNT(e) FROM EmploymentRecord e GROUP BY e.companyIndustry ORDER BY COUNT(e) DESC")
    List<Object[]> countByCompanyIndustry();

    @Query("SELECT e FROM EmploymentRecord e ORDER BY e.createTime DESC")
    List<EmploymentRecord> findRecentRecords();

    @Query("SELECT s.classId, COUNT(DISTINCT e.studentId) FROM EmploymentRecord e " +
           "JOIN StudentInfo s ON e.studentId = s.id " +
           "WHERE e.auditStatus = 'approved' AND s.classId IN :classIds GROUP BY s.classId")
    List<Object[]> countApprovedByClassIds(List<Long> classIds);

    @Query("SELECT s.classId, e.workProvince, COUNT(e) FROM EmploymentRecord e " +
           "JOIN StudentInfo s ON e.studentId = s.id " +
           "WHERE e.auditStatus = 'approved' AND s.classId IN :classIds " +
           "AND e.workProvince IS NOT NULL AND e.workProvince <> '' " +
           "GROUP BY s.classId, e.workProvince")
    List<Object[]> countApprovedByProvince(List<Long> classIds);

    // 按班级ID列表过滤待审核记录（仅返回指定班级的记录）
    @Query("SELECT e FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE e.auditStatus = 'pending' AND s.classId IN :classIds ORDER BY e.createTime DESC")
    List<EmploymentRecord> findPendingByClassIds(@Param("classIds") List<Long> classIds);

    // 按班级ID列表过滤已审核记录
    @Query("SELECT e FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE s.classId IN :classIds AND e.auditStatus <> 'pending' ORDER BY e.createTime DESC")
    List<EmploymentRecord> findApprovedByClassIds(@Param("classIds") List<Long> classIds);

    // 按班级ID列表过滤所有记录
    @Query("SELECT e FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE s.classId IN :classIds ORDER BY e.createTime DESC")
    List<EmploymentRecord> findByClassIds(@Param("classIds") List<Long> classIds);

    // 按班级ID列表统计总数
    @Query("SELECT COUNT(DISTINCT e) FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE s.classId IN :classIds")
    long countByClassIds(@Param("classIds") List<Long> classIds);

    // 按班级ID列表统计待审核数
    @Query("SELECT COUNT(DISTINCT e) FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE e.auditStatus = 'pending' AND s.classId IN :classIds")
    long countPendingByClassIds(@Param("classIds") List<Long> classIds);

    // 按院系ID列表过滤待审核记录
    @Query("SELECT e FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE e.auditStatus = 'pending' AND s.deptId IN :deptIds ORDER BY e.createTime DESC")
    List<EmploymentRecord> findPendingByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 按院系ID列表过滤已审核记录
    @Query("SELECT e FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE s.deptId IN :deptIds AND e.auditStatus <> 'pending' ORDER BY e.createTime DESC")
    List<EmploymentRecord> findApprovedByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 按院系ID列表过滤所有记录
    @Query("SELECT e FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE s.deptId IN :deptIds ORDER BY e.createTime DESC")
    List<EmploymentRecord> findByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 按院系ID列表统计总数
    @Query("SELECT COUNT(DISTINCT e) FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE s.deptId IN :deptIds")
    long countByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 按院系ID列表统计待审核数
    @Query("SELECT COUNT(DISTINCT e) FROM EmploymentRecord e JOIN StudentInfo s ON e.studentId = s.id WHERE e.auditStatus = 'pending' AND s.deptId IN :deptIds")
    long countPendingByDeptIds(@Param("deptIds") List<Long> deptIds);

    // ==================== SQL 聚合统计（替代内存计算） ====================

    // 统计指定班级的已就业人数（取每个学生最新一条 approved 记录）
    @Query(value = """
        SELECT COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.class_id IN :classIds
        """, nativeQuery = true)
    long countEmployedByClassIds(@Param("classIds") List<Long> classIds);

    // 统计指定院系的已就业人数
    @Query(value = """
        SELECT COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.dept_id IN :deptIds
        """, nativeQuery = true)
    long countEmployedByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 统计指定班级的特殊就业类型人数
    @Query(value = """
        SELECT latest.employment_type, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.class_id IN :classIds
          AND latest.employment_type IN ('继续深造','升学','应征入伍','入伍','服兵役','自主创业','创业','出国出境','出国','境外')
        GROUP BY latest.employment_type
        """, nativeQuery = true)
    List<Object[]> countSpecialTypeByClassIds(@Param("classIds") List<Long> classIds);

    // 统计指定院系的特殊就业类型人数
    @Query(value = """
        SELECT latest.employment_type, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.dept_id IN :deptIds
          AND latest.employment_type IN ('继续深造','升学','应征入伍','入伍','服兵役','自主创业','创业','出国出境','出国','境外')
        GROUP BY latest.employment_type
        """, nativeQuery = true)
    List<Object[]> countSpecialTypeByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 统计省份分布（指定班级）
    @Query(value = """
        SELECT latest.work_province, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.class_id IN :classIds AND latest.work_province IS NOT NULL AND latest.work_province <> ''
        GROUP BY latest.work_province
        """, nativeQuery = true)
    List<Object[]> countProvinceByClassIds(@Param("classIds") List<Long> classIds);

    // 统计省份分布（指定院系）
    @Query(value = """
        SELECT latest.work_province, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.dept_id IN :deptIds AND latest.work_province IS NOT NULL AND latest.work_province <> ''
        GROUP BY latest.work_province
        """, nativeQuery = true)
    List<Object[]> countProvinceByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 统计就业类型分布（指定班级）
    @Query(value = """
        SELECT latest.employment_type, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.class_id IN :classIds AND latest.employment_type IS NOT NULL AND latest.employment_type <> ''
        GROUP BY latest.employment_type
        """, nativeQuery = true)
    List<Object[]> countEmploymentTypeByClassIds(@Param("classIds") List<Long> classIds);

    // 统计就业类型分布（指定院系）
    @Query(value = """
        SELECT latest.employment_type, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.dept_id IN :deptIds AND latest.employment_type IS NOT NULL AND latest.employment_type <> ''
        GROUP BY latest.employment_type
        """, nativeQuery = true)
    List<Object[]> countEmploymentTypeByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 统计行业分布（指定班级）
    @Query(value = """
        SELECT latest.company_industry, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.class_id IN :classIds AND latest.company_industry IS NOT NULL AND latest.company_industry <> ''
        GROUP BY latest.company_industry
        """, nativeQuery = true)
    List<Object[]> countIndustryByClassIds(@Param("classIds") List<Long> classIds);

    // 统计行业分布（指定院系）
    @Query(value = """
        SELECT latest.company_industry, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.dept_id IN :deptIds AND latest.company_industry IS NOT NULL AND latest.company_industry <> ''
        GROUP BY latest.company_industry
        """, nativeQuery = true)
    List<Object[]> countIndustryByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 统计薪资分布（指定班级），返回每条记录供 Java 层分段聚合
    @Query(value = """
        SELECT latest.salary, latest.student_id
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.class_id IN :classIds AND latest.salary IS NOT NULL AND latest.salary <> ''
        """, nativeQuery = true)
    List<Object[]> salaryDataByClassIds(@Param("classIds") List<Long> classIds);

    // 统计薪资分布（指定院系）
    @Query(value = """
        SELECT latest.salary, latest.student_id
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.dept_id IN :deptIds AND latest.salary IS NOT NULL AND latest.salary <> ''
        """, nativeQuery = true)
    List<Object[]> salaryDataByDeptIds(@Param("deptIds") List<Long> deptIds);

    // 统计城市分布（指定班级）
    @Query(value = """
        SELECT latest.work_city, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.class_id IN :classIds AND latest.work_city IS NOT NULL AND latest.work_city <> ''
        GROUP BY latest.work_city
        """, nativeQuery = true)
    List<Object[]> countCityByClassIds(@Param("classIds") List<Long> classIds);

    // 统计城市分布（指定院系）
    @Query(value = """
        SELECT latest.work_city, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.dept_id IN :deptIds AND latest.work_city IS NOT NULL AND latest.work_city <> ''
        GROUP BY latest.work_city
        """, nativeQuery = true)
    List<Object[]> countCityByDeptIds(@Param("deptIds") List<Long> deptIds);

    // ==================== Analytics 全局 SQL 聚合（不限定班级/院系） ====================

    // 统计全局已就业人数（取每个学生最新一条 approved 记录）
    @Query(value = """
        SELECT COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE (:year IS NULL OR si.graduation_year = :year)
        """, nativeQuery = true)
    long countGlobalEmployed(@Param("year") Integer year);

    // 全局特殊就业类型统计
    @Query(value = """
        SELECT latest.employment_type, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE (:year IS NULL OR si.graduation_year = :year)
          AND latest.employment_type IN ('继续深造','升学','应征入伍','入伍','服兵役','自主创业','创业','出国出境','出国','境外')
        GROUP BY latest.employment_type
        """, nativeQuery = true)
    List<Object[]> countGlobalSpecialType(@Param("year") Integer year);

    // 全局就业类型分布
    @Query(value = """
        SELECT latest.employment_type, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE (:year IS NULL OR si.graduation_year = :year)
          AND latest.employment_type IS NOT NULL AND latest.employment_type <> ''
        GROUP BY latest.employment_type
        """, nativeQuery = true)
    List<Object[]> countGlobalEmploymentType(@Param("year") Integer year);

    // 全局薪资数据（只取 salary 和 student_id 两个字段，最小传输量）
    @Query(value = """
        SELECT latest.salary
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE (:year IS NULL OR si.graduation_year = :year)
          AND latest.salary IS NOT NULL AND latest.salary <> ''
        """, nativeQuery = true)
    List<Object[]> getGlobalSalaryData(@Param("year") Integer year);

    // 全局行业分布
    @Query(value = """
        SELECT latest.company_industry, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE (:year IS NULL OR si.graduation_year = :year)
          AND latest.company_industry IS NOT NULL AND latest.company_industry <> ''
        GROUP BY latest.company_industry
        """, nativeQuery = true)
    List<Object[]> countGlobalIndustry(@Param("year") Integer year);

    // 全局省份分布
    @Query(value = """
        SELECT latest.work_province, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE (:year IS NULL OR si.graduation_year = :year)
          AND latest.work_province IS NOT NULL AND latest.work_province <> ''
        GROUP BY latest.work_province
        """, nativeQuery = true)
    List<Object[]> countGlobalProvince(@Param("year") Integer year);

    // 全局待审核记录数
    @Query("SELECT COUNT(e) FROM EmploymentRecord e INNER JOIN StudentInfo s ON e.studentId = s.id WHERE e.auditStatus = 'pending' AND (:year IS NULL OR s.graduationYear = :year)")
    long countGlobalPending(@Param("year") Integer year);

    // 全局记录总数（有就业记录的学生数）
    @Query(value = """
        SELECT COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE (:year IS NULL OR si.graduation_year = :year)
        """, nativeQuery = true)
    long countGlobalRecords(@Param("year") Integer year);

    // 按院系统计已就业人数（带年份过滤，全量）
    @Query(value = """
        SELECT si.dept_id, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.dept_id IS NOT NULL AND (:year IS NULL OR si.graduation_year = :year)
        GROUP BY si.dept_id
        """, nativeQuery = true)
    List<Object[]> countEmployedByAllDepts(@Param("year") Integer year);

    // 按班级统计已就业人数（带年份过滤，全量）
    @Query(value = """
        SELECT si.class_id, COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN (
            SELECT student_id, MAX(create_time) as max_time
            FROM employment_record
            WHERE audit_status = 'approved'
            GROUP BY student_id
        ) m ON latest.student_id = m.student_id AND latest.create_time = m.max_time
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE si.class_id IS NOT NULL AND (:year IS NULL OR si.graduation_year = :year)
        GROUP BY si.class_id
        """, nativeQuery = true)
    List<Object[]> countEmployedByAllClasses(@Param("year") Integer year);

    // ==================== 未就业学生查询（数据分析员专用） ====================

    // 查询未就业学生列表（没有 approved 就业记录的毕业届学生）
    @Query(value = """
        SELECT si.id AS studentId,
               si.real_name AS realName,
               si.student_no AS studentNo,
               si.gender,
               si.graduation_year AS graduationYear,
               sd.dept_name AS deptName,
               sm.major_name AS majorName,
               sc.class_name AS className,
               latest.audit_status AS recordStatus,
               latest.employment_type AS employmentType
        FROM student_info si
        LEFT JOIN sys_dept sd ON si.dept_id = sd.id
        LEFT JOIN sys_major sm ON si.major_id = sm.id
        LEFT JOIN sys_class sc ON si.class_id = sc.id
        LEFT JOIN (
            SELECT er.student_id,
                   er.audit_status,
                   er.employment_type,
                   er.create_time,
                   ROW_NUMBER() OVER (PARTITION BY er.student_id ORDER BY er.create_time DESC) AS rn
            FROM employment_record er
        ) latest ON si.id = latest.student_id AND latest.rn = 1
        WHERE (:year IS NULL OR si.graduation_year = :year)
          AND (latest.student_id IS NULL OR latest.audit_status != 'approved')
        ORDER BY si.graduation_year DESC, sd.dept_name, sc.class_name
        """, nativeQuery = true)
    List<Object[]> findUnemployedStudents(@Param("year") Integer year);

    // 未就业学生总数
    @Query(value = """
        SELECT COUNT(si.id)
        FROM student_info si
        LEFT JOIN (
            SELECT er.student_id,
                   er.audit_status,
                   ROW_NUMBER() OVER (PARTITION BY er.student_id ORDER BY er.create_time DESC) AS rn
            FROM employment_record er
        ) latest ON si.id = latest.student_id AND latest.rn = 1
        WHERE (:year IS NULL OR si.graduation_year = :year)
          AND (latest.student_id IS NULL OR latest.audit_status != 'approved')
        """, nativeQuery = true)
    long countUnemployedStudents(@Param("year") Integer year);

    // 近7天新增审核通过的就业记录数（按年份过滤）
    @Query(value = """
        SELECT COUNT(DISTINCT latest.student_id)
        FROM employment_record latest
        INNER JOIN student_info si ON latest.student_id = si.id
        WHERE (:year IS NULL OR si.graduation_year = :year)
          AND latest.audit_status = 'approved'
          AND latest.create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
        """, nativeQuery = true)
    long countRecent7DaysApproved(@Param("year") Integer year);
}
