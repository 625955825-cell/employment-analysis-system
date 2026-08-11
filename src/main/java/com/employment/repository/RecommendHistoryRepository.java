package com.employment.repository;

import com.employment.model.entity.RecommendHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendHistoryRepository extends JpaRepository<RecommendHistory, Long> {

    Page<RecommendHistory> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<RecommendHistory> findByRecommendTypeOrderByCreateTimeDesc(String recommendType, Pageable pageable);

    List<RecommendHistory> findByTargetIdAndRecommendType(Long targetId, String recommendType);
}
