package com.employment.repository;

import com.employment.model.entity.SysPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysPermissionRepository extends JpaRepository<SysPermission, Long> {
    Optional<SysPermission> findByPermissionKey(String permissionKey);
    List<SysPermission> findByParentId(Long parentId);
    List<SysPermission> findByStatus(String status);
}
