package com.server.framework.repository;

import com.server.framework.entity.HttpLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HttpLogRepository extends JpaRepository<HttpLogEntity, Long> {
    
    List<HttpLogEntity> findByUrl(String url);
    
    List<HttpLogEntity> findByMethod(String method);
    
    List<HttpLogEntity> findByIp(String ip);
    
    List<HttpLogEntity> findByStatusCode(Integer statusCode);
    
    List<HttpLogEntity> findByIsOutgoing(Boolean isOutgoing);
    
    @Query("SELECT h FROM HttpLog h WHERE h.url LIKE %:url%")
    List<HttpLogEntity> findByUrlContaining(@Param("url") String url);
    
    @Query("SELECT h FROM HttpLog h WHERE h.createdTime BETWEEN :startTime AND :endTime")
    List<HttpLogEntity> findByTimeRange(@Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    @Query("SELECT h FROM HttpLog h WHERE h.method = :method AND h.statusCode = :statusCode")
    List<HttpLogEntity> findByMethodAndStatusCode(@Param("method") String method, @Param("statusCode") Integer statusCode);
    
    @Query("SELECT h FROM HttpLog h WHERE h.ip = :ip AND h.isOutgoing = :isOutgoing")
    List<HttpLogEntity> findByIpAndIsOutgoing(@Param("ip") String ip, @Param("isOutgoing") Boolean isOutgoing);
    
    @Query("SELECT COUNT(h) FROM HttpLog h WHERE h.statusCode = :statusCode")
    Long countByStatusCode(@Param("statusCode") Integer statusCode);
    
    @Query("SELECT COUNT(h) FROM HttpLog h WHERE h.isOutgoing = :isOutgoing")
    Long countByIsOutgoing(@Param("isOutgoing") Boolean isOutgoing);
    
    @Modifying
    @Query(value = "DELETE FROM HttpLog h WHERE h.createdTime < ?1", nativeQuery = true)
    int deleteOldLogs(Long timestamp);
}
