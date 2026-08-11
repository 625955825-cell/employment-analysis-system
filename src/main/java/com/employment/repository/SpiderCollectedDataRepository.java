package com.employment.repository;

import com.employment.model.entity.SpiderCollectedData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpiderCollectedDataRepository extends JpaRepository<SpiderCollectedData, Long> {

    Page<SpiderCollectedData> findAllByOrderByCreateTimeDesc(Pageable pageable);

    Page<SpiderCollectedData> findByDataTypeOrderByCreateTimeDesc(String dataType, Pageable pageable);

    Page<SpiderCollectedData> findBySourceCodeOrderByCreateTimeDesc(String sourceCode, Pageable pageable);

    Page<SpiderCollectedData> findByIsSyncedOrderByCreateTimeDesc(String isSynced, Pageable pageable);

    List<SpiderCollectedData> findByIsSynced(String isSynced);

    List<SpiderCollectedData> findByIsValid(String isValid);

    List<SpiderCollectedData> findByIsValidAndIsSynced(String isValid, String isSynced);

    @Query("SELECT d FROM SpiderCollectedData d WHERE " +
           "(:sourceCode IS NULL OR d.sourceCode = :sourceCode) AND " +
           "(:dataType IS NULL OR d.dataType = :dataType) AND " +
           "(:majorName IS NULL OR d.majorName = :majorName) AND " +
           "(:startDate IS NULL OR d.collectTime >= :startDate) AND " +
           "(:endDate IS NULL OR d.collectTime <= :endDate) " +
           "ORDER BY d.createTime DESC")
    Page<SpiderCollectedData> searchData(@Param("sourceCode") String sourceCode,
                                        @Param("dataType") String dataType,
                                        @Param("majorName") String majorName,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        Pageable pageable);

    @Modifying
    @Query(value = "UPDATE spider_collected_data SET is_synced = '1', sync_time = NOW() WHERE id IN :ids", nativeQuery = true)
    int markAsSynced(@Param("ids") List<Long> ids);

    long countByDataType(String dataType);

    long countByIsSynced(String isSynced);

    @Modifying
    @Query(value = "DELETE FROM spider_collected_data", nativeQuery = true)
    void deleteAllData();
}
