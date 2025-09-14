package com.server.framework.repository;

import com.server.framework.entity.WorkflowInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, String> {

    Optional<WorkflowInstanceEntity> findByReferenceID(String referenceID);

    List<WorkflowInstanceEntity> findByStatus(String status);

    List<WorkflowInstanceEntity> findByWorkflowName(String workflowName);

    List<WorkflowInstanceEntity> findByWorkflowNameAndStatus(String workflowName, String status);

    @Query("SELECT w FROM WorkflowInstance w ORDER BY w.startTime DESC")
    List<WorkflowInstanceEntity> findRecentInstances();

    @Query(value = "SELECT * FROM WorkflowInstance ORDER BY StartTime DESC LIMIT :limit", nativeQuery = true)
    List<WorkflowInstanceEntity> findRecentInstancesWithLimit(@Param("limit") int limit);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.status IN ('COMPLETED', 'FAILED') AND w.lastUpdateTime < :cutoffTime")
    List<WorkflowInstanceEntity> findOldInstancesForCleanup(@Param("cutoffTime") Long cutoffTime);

    long countByStatus(String status);

    long countByWorkflowName(String workflowName);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.status = 'RUNNING' AND w.lastUpdateTime < :thresholdTime")
    List<WorkflowInstanceEntity> findStaleRunningInstances(@Param("thresholdTime") Long thresholdTime);
}
