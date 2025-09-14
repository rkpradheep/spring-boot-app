package com.server.framework.repository;

import com.server.framework.entity.SessionManagementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionManagementRepository extends JpaRepository<SessionManagementEntity, Long> {
    
    Optional<SessionManagementEntity> findBySessionId(String sessionId);
    
    List<SessionManagementEntity> findByUserId(Long userId);
    
    @Query("SELECT s FROM SessionManagement s WHERE s.userId = :userId AND s.sessionId = :sessionId")
    Optional<SessionManagementEntity> findByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
    
    @Query("SELECT s FROM SessionManagement s WHERE s.expiryTime < :currentTime")
    List<SessionManagementEntity> findExpiredSessions(@Param("currentTime") Long currentTime);
    
    @Query("SELECT COUNT(s) FROM SessionManagement s WHERE s.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    void deleteByUserId(Long userId);
    
    void deleteBySessionId(String sessionId);

    @Query("DELETE FROM SessionManagement s WHERE s.expiryTime < :currentTime")
    void deleteExpiredSessions(@Param("currentTime") Long currentTime);
    
    boolean existsBySessionId(String sessionId);
}
