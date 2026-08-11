package com.employment.repository;

import com.employment.model.entity.CompanyAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyAuthRepository extends JpaRepository<CompanyAuth, Long> {
    List<CompanyAuth> findByCompanyId(Long companyId);
    List<CompanyAuth> findByAuditStatus(String auditStatus);
}
