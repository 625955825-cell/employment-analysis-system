package com.employment.repository;

import com.employment.model.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {
    List<SysUserRole> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    @Query("SELECT COUNT(sr) FROM SysUserRole sr WHERE sr.roleId = :roleId")
    long countByRoleId(Long roleId);

    @Query("SELECT sr.userId FROM SysUserRole sr WHERE sr.roleId = :roleId")
    List<Long> findUserIdsByRoleId(Long roleId);
}
