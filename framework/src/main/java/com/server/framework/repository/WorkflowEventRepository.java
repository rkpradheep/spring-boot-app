package com.server.framework.repository;

import com.server.framework.entity.WorkflowEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowEventRepository extends JpaRepository<WorkflowEventEntity, Long> {

    List<WorkflowEventEntity> findByReferenceIDOrderByTimestampAsc(String referenceID);

    List<WorkflowEventEntity> findByReferenceIDAndEventTypeOrderByTimestampAsc(String referenceID, String eventType);

    @Query(value = "SELECT * FROM WorkflowEvent WHERE ReferenceID = :referenceID ORDER BY Timestamp DESC LIMIT :limit", nativeQuery = true)
    List<WorkflowEventEntity> findByReferenceIDWithLimit(@Param("referenceID") String referenceID, @Param("limit") int limit);

    long countByReferenceID(String referenceID);

    List<WorkflowEventEntity> findByEventTypeOrderByTimestampDesc(String eventType);

    @Query("SELECT e FROM WorkflowEvent e WHERE e.timestamp < :cutoffTime")
    List<WorkflowEventEntity> findOldEventsForCleanup(@Param("cutoffTime") Long cutoffTime);

    void deleteByReferenceID(String referenceID);

    List<WorkflowEventEntity> findByCorrelationIdOrderByTimestampAsc(String correlationId);
}


