package com.employment.repository;

import com.employment.model.entity.RecommendWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendWeightConfigRepository extends JpaRepository<RecommendWeightConfig, Long> {

    Optional<RecommendWeightConfig> findByWeightKey(String weightKey);

    List<RecommendWeightConfig> findByEnabled(String enabled);

    List<RecommendWeightConfig> findAllByOrderByIdAsc();

    boolean existsByWeightKey(String weightKey);
}
