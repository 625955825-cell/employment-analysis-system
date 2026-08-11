package com.employment.repository;

import com.employment.model.entity.OfferLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferLetterRepository extends JpaRepository<OfferLetter, Long> {
    List<OfferLetter> findByStudentId(Long studentId);
    List<OfferLetter> findByCompanyId(Long companyId);
    List<OfferLetter> findByApplicationId(Long applicationId);
}
