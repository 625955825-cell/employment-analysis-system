package com.employment.repository;

import com.employment.model.entity.TripartiteAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripartiteAgreementRepository extends JpaRepository<TripartiteAgreement, Long> {
    List<TripartiteAgreement> findByStudentId(Long studentId);
    List<TripartiteAgreement> findByCompanyId(Long companyId);
    Optional<TripartiteAgreement> findByAgreementNo(String agreementNo);
    List<TripartiteAgreement> findByStudentIdIn(List<Long> studentIds);

    @Query("SELECT t FROM TripartiteAgreement t WHERE t.studentId IN :studentIds ORDER BY t.createTime DESC")
    List<TripartiteAgreement> findAllByStudentIds(@Param("studentIds") List<Long> studentIds);
}
