package com.employment.repository;

import com.employment.model.entity.SysDept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysDeptRepository extends JpaRepository<SysDept, Long> {
    Optional<SysDept> findByDeptName(String deptName);
    Optional<SysDept> findByDeptCode(String deptCode);
    List<SysDept> findByParentId(Long parentId);
    List<SysDept> findByStatus(String status);
}
