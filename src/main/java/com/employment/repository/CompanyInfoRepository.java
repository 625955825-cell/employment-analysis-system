package com.employment.repository;

import com.employment.model.entity.CompanyInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long> {
    Optional<CompanyInfo> findByUserId(Long userId);
    Optional<CompanyInfo> findByCompanyCode(String companyCode);
    List<CompanyInfo> findByAuthStatus(String authStatus);
    long countByAuthStatus(String authStatus);
    @Query("SELECT c.industry, COUNT(c) FROM CompanyInfo c WHERE c.authStatus = 'approved' GROUP BY c.industry ORDER BY COUNT(c) DESC")
    List<Object[]> countApprovedByIndustry();
}
