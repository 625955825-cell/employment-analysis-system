package com.employment.repository;

import com.employment.model.entity.SysMajor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysMajorRepository extends JpaRepository<SysMajor, Long> {
    Optional<SysMajor> findByMajorCode(String majorCode);
    List<SysMajor> findByDeptId(Long deptId);
    @Query("SELECT m.id FROM SysMajor m WHERE m.majorName LIKE %:keyword%")
    List<Long> findIdsByMajorNameContaining(@Param("keyword") String keyword);
}
