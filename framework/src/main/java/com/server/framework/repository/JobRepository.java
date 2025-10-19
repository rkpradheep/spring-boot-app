package com.server.framework.repository;

import com.server.framework.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, Long> {

    @Transactional
    @Modifying
    @Query("UPDATE Job j SET j.status = -1")
    void markAllJobsAsNotRunning();
    
    List<JobEntity> findByTaskName(String taskName);
    
    List<JobEntity> findByIsRecurring(Boolean isRecurring);
    
    @Query("SELECT j FROM Job j WHERE j.scheduledTime <= :executionTime AND j.status = :status")
    List<JobEntity> findJobsToExecute(@Param("executionTime") Long executionTime, @Param("status") Integer status);
    
    @Query("SELECT j FROM Job j WHERE j.taskName = :taskName AND j.isRecurring = :isRecurring")
    List<JobEntity> findByTaskNameAndIsRecurring(@Param("taskName") String taskName, @Param("isRecurring") Boolean isRecurring);
    
    @Query("SELECT COUNT(j) FROM Job j WHERE j.isRecurring = :isRecurring")
    Long countByIsRecurring(@Param("isRecurring") Boolean isRecurring);
    
    @Query("SELECT j FROM Job j WHERE j.scheduledTime BETWEEN :startTime AND :endTime")
    List<JobEntity> findJobsByTimeRange(@Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    void deleteByTaskName(String taskName);
}
