package com.employment.repository;

import com.employment.model.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {
    Optional<SysRole> findByRoleKey(String roleKey);
    boolean existsByRoleKey(String roleKey);
}
