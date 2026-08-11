package com.employment.repository;

import com.employment.model.entity.StudentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentInfoRepository extends JpaRepository<StudentInfo, Long> {
    Optional<StudentInfo> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<StudentInfo> findByClassId(Long classId);
    List<StudentInfo> findByDeptId(Long deptId);
    List<StudentInfo> findByClassIdAndGraduationYear(Long classId, Integer graduationYear);
    List<StudentInfo> findByDeptIdAndGraduationYear(Long deptId, Integer graduationYear);

    long countByClassId(Long classId);
    long countByDeptId(Long deptId);

    @Query("SELECT d.deptName, COUNT(s) FROM StudentInfo s LEFT JOIN SysDept d ON s.deptId = d.id GROUP BY d.deptName ORDER BY COUNT(s) DESC")
    List<Object[]> countByDept();

    @Query("SELECT s.majorName, COUNT(s) FROM StudentInfo s GROUP BY s.majorName ORDER BY COUNT(s) DESC")
    List<Object[]> countByMajor();

    @Query("SELECT s.graduationYear, COUNT(s) FROM StudentInfo s GROUP BY s.graduationYear ORDER BY s.graduationYear")
    List<Object[]> countByGraduationYear();

    // ==================== Analytics SQL 聚合（替代全表扫描） ====================

    // 按毕业年份统计学生人数
    @Query("SELECT COUNT(s) FROM StudentInfo s WHERE s.graduationYear = :year")
    long countByGraduationYearOnly(@Param("year") Integer year);

    // 按院系分组统计（带年份过滤，JOIN sys_dept 取正确名称）
    @Query(value = """
        SELECT student_info.dept_id, sys_dept.dept_name, COUNT(student_info.id)
        FROM student_info
        LEFT JOIN sys_dept ON student_info.dept_id = sys_dept.id
        WHERE student_info.dept_id IS NOT NULL AND (:year IS NULL OR student_info.graduation_year = :year)
        GROUP BY student_info.dept_id, sys_dept.dept_name
        ORDER BY COUNT(student_info.id) DESC
        """, nativeQuery = true)
    List<Object[]> countGroupByDept(@Param("year") Integer year);

    // 按班级分组统计（带年份过滤，JOIN sys_dept/sys_major 取正确名称）
    @Query(value = """
        SELECT student_info.class_id, student_info.class_name, sys_dept.dept_name, sys_major.major_name, COUNT(student_info.id)
        FROM student_info
        LEFT JOIN sys_dept ON student_info.dept_id = sys_dept.id
        LEFT JOIN sys_major ON student_info.major_id = sys_major.id
        WHERE student_info.class_id IS NOT NULL AND (:year IS NULL OR student_info.graduation_year = :year)
        GROUP BY student_info.class_id, student_info.class_name, sys_dept.dept_name, sys_major.major_name
        ORDER BY COUNT(student_info.id) DESC
        """, nativeQuery = true)
    List<Object[]> countGroupByClass(@Param("year") Integer year);

    // 全部学生总数（带年份过滤）
    @Query("SELECT COUNT(s) FROM StudentInfo s WHERE (:year IS NULL OR s.graduationYear = :year)")
    long countStudents(@Param("year") Integer year);

    // 按专业分组统计（带年份过滤，JOIN sys_dept/sys_major 取正确名称）
    @Query(value = """
        SELECT si.major_id, sys_major.major_name, si.dept_id, sys_dept.dept_name, COUNT(si.id)
        FROM student_info si
        LEFT JOIN sys_dept ON si.dept_id = sys_dept.id
        LEFT JOIN sys_major ON si.major_id = sys_major.id
        WHERE si.major_id IS NOT NULL AND (:year IS NULL OR si.graduation_year = :year)
        GROUP BY si.major_id, sys_major.major_name, si.dept_id, sys_dept.dept_name
        ORDER BY COUNT(si.id) DESC
        """, nativeQuery = true)
    List<Object[]> countGroupByMajor(@Param("year") Integer year);

    // 按专业 ID 统计已就业人数（用于专业预警）
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
        WHERE si.major_id = :majorId
          AND (:year IS NULL OR si.graduation_year = :year)
        """, nativeQuery = true)
    long countEmployedByMajorId(@Param("majorId") Long majorId, @Param("year") Integer year);
}
