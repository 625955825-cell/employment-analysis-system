package com.employment.repository;

import com.employment.model.entity.JobPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {
    List<JobPosition> findByCompanyId(Long companyId);
    Page<JobPosition> findByStatusAndIsDeleted(String status, String isDeleted, Pageable pageable);

    List<JobPosition> findByStatusAndIsDeleted(String status, String isDeleted);

    @Query("SELECT j FROM JobPosition j WHERE j.status = 'published' AND j.isDeleted = '0' " +
           "AND (:keyword IS NULL OR j.jobName LIKE %:keyword% OR j.companyName LIKE %:keyword%) " +
           "AND (:city IS NULL OR j.workCity LIKE %:city%)")
    Page<JobPosition> searchJobs(@Param("keyword") String keyword, @Param("city") String city,
                                 Pageable pageable);

    @Modifying
    @Query("UPDATE JobPosition j SET j.viewCount = j.viewCount + 1 WHERE j.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT COUNT(j) FROM JobPosition j WHERE j.status = :status AND j.isDeleted = :isDeleted")
    long countByStatusAndIsDeleted(@Param("status") String status, @Param("isDeleted") String isDeleted);
}
