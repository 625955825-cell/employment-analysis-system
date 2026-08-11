package com.employment.repository;

import com.employment.model.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    Optional<SysUser> findByEmail(String email);
    Optional<SysUser> findByPhone(String phone);
    Optional<SysUser> findByStudentNo(String studentNo);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Query(value = "UPDATE sys_user SET password = :password, update_time = NOW() WHERE id = :id", nativeQuery = true)
    @Modifying
    void updatePasswordById(Long id, String password);

    List<SysUser> findByDeptId(Long deptId);

    List<SysUser> findByClassId(Long classId);

    void deleteByClassId(Long classId);
}
