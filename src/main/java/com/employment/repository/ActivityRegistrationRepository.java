package com.employment.repository;

import com.employment.model.entity.ActivityRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {
    List<ActivityRegistration> findByStudentId(Long studentId);
    List<ActivityRegistration> findByActivityId(Long activityId);
    Optional<ActivityRegistration> findByActivityIdAndStudentId(Long activityId, Long studentId);
}
