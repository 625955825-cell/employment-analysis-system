package com.employment.repository;

import com.employment.model.entity.SysClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface SysClassRepository extends JpaRepository<SysClass, Long>, JpaSpecificationExecutor<SysClass> {
    List<SysClass> findByMajorId(Long majorId);
    List<SysClass> findByDeptId(Long deptId);
    List<SysClass> findByGrade(String grade);
    List<SysClass> findByMajorIdAndGrade(Long majorId, String grade);
    boolean existsByClassNameAndMajorId(String className, Long majorId);
    List<SysClass> findByAdvisorId(Long advisorId);

    void deleteByGrade(String grade);

    @Query("SELECT s.classId AS classId, COUNT(s) AS studentCount FROM SysUser s WHERE s.classId IS NOT NULL AND s.studentNo IS NOT NULL GROUP BY s.classId")
    List<Map<String, Object>> countStudentsByClassId();
}
