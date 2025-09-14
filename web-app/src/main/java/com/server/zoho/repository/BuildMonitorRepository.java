package com.server.zoho.repository;

import com.server.zoho.entity.BuildMonitorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildMonitorRepository extends JpaRepository<BuildMonitorEntity, Long> {
    
    List<BuildMonitorEntity> findByStatus(String status);

    @Query("SELECT bm FROM BuildMonitor bm WHERE bm.lastUpdateTime < :cutoffTime AND bm.status = 'ACTIVE'")
    List<BuildMonitorEntity> findStaleActiveMonitors(@Param("cutoffTime") Long cutoffTime);
    
    @Query("SELECT bm FROM BuildMonitor bm ORDER BY bm.startTime DESC")
    List<BuildMonitorEntity> findTop10ByOrderByStartTimeDesc();
    
    @Query("DELETE FROM BuildMonitor bm WHERE bm.status IN ('COMPLETED', 'FAILED') AND bm.lastUpdateTime < :cutoffTime")
    void deleteOldCompletedMonitors(@Param("cutoffTime") Long cutoffTime);
}
