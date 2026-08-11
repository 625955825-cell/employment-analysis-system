package com.employment.repository;

import com.employment.model.entity.CareerActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerActivityRepository extends JpaRepository<CareerActivity, Long> {
    List<CareerActivity> findByStatus(String status);
    List<CareerActivity> findByActivityType(String activityType);
}
