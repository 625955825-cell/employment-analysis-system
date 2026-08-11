package com.employment.repository;

import com.employment.model.entity.SpiderMajorKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpiderMajorKeywordRepository extends JpaRepository<SpiderMajorKeyword, Long> {

    List<SpiderMajorKeyword> findByMajorName(String majorName);

    List<SpiderMajorKeyword> findByMajorNameOrderByIdAsc(String majorName);

    Optional<SpiderMajorKeyword> findByMajorNameAndKeyword(String majorName, String keyword);

    boolean existsByMajorNameAndKeyword(String majorName, String keyword);

    void deleteByMajorNameAndKeyword(String majorName, String keyword);

    void deleteByMajorName(String majorName);

    long countByMajorName(String majorName);
}
