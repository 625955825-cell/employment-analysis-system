package com.employment.repository;

import com.employment.model.entity.SysNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysNoticeRepository extends JpaRepository<SysNotice, Long> {
    List<SysNotice> findByStatusOrderByCreateTimeDesc(String status);
    List<SysNotice> findByTopStatusOrderByCreateTimeDesc(String topStatus);
}
