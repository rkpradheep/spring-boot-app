package com.server.zoho.repository;

import com.server.zoho.entity.BuildProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuildProductRepository extends JpaRepository<BuildProductEntity, Long> {

    List<BuildProductEntity> findByBuildMonitorIdOrderByOrderIndexAsc(Long buildMonitorId);


    @Query("SELECT bp FROM BuildProduct bp WHERE bp.buildMonitorId = :buildMonitorId AND bp.status = 'PENDING' ORDER BY bp.orderIndex ASC LIMIT 1")
    Optional<BuildProductEntity> findNextPendingProduct(@Param("buildMonitorId") Long buildMonitorId);

    @Query("SELECT bp FROM BuildProduct bp WHERE bp.buildMonitorId = :buildMonitorId AND bp.status IN ('PENDING', 'STARTED', 'MILESTONE_CREATED', 'CHANNEL_MAPPED') ORDER BY bp.orderIndex ASC")
    Optional<BuildProductEntity> findCurrentInProgressProduct(@Param("buildMonitorId") Long buildMonitorId);

    @Query("SELECT bp FROM BuildProduct bp WHERE bp.buildMonitorId = :buildMonitorId AND bp.status IN ('PENDING', 'STARTED', 'MILESTONE_CREATED', 'CHANNEL_MAPPED') ORDER BY CASE WHEN bp.status = 'STARTED' THEN 0 WHEN bp.status = 'MILESTONE_CREATED' THEN 1 WHEN bp.status = 'CHANNEL_MAPPED' THEN 2 WHEN bp.status = 'PENDING' THEN 3 ELSE 4 END, bp.orderIndex ASC")
    Optional<BuildProductEntity> findCurrentActiveProduct(@Param("buildMonitorId") Long buildMonitorId);

    long countByBuildMonitorIdAndStatus(Long buildMonitorId, String status);

    long countByBuildMonitorId(Long buildMonitorId);

    @Query("SELECT COUNT(bp) FROM BuildProduct bp WHERE bp.buildMonitorId = :buildMonitorId AND bp.status IN ('SUCCESS', 'FAILED', 'ERROR')")
    long countCompletedProducts(@Param("buildMonitorId") Long buildMonitorId);

    @Modifying
    @Transactional
    void deleteByBuildMonitorId(Long buildMonitorId);

    Optional<BuildProductEntity> findByBuildId(Long buildId);
}
