package com.server.framework.repository;

import com.server.framework.entity.AuthTokenEntity;
import com.server.framework.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthTokenEntity, Long> {
    
    Optional<AuthTokenEntity> findByToken(String token);
    
    List<AuthTokenEntity> findByUserId(Long userId);

    Optional<AuthTokenEntity> findByUserAndToken(UserEntity userEntity, String token);

    Long countByUser(UserEntity userEntity);

    void deleteByUser(UserEntity userEntity);

    void deleteByToken(String token);
    
    boolean existsByToken(String token);

    @Modifying
    @Query(value = "DELETE FROM AuthToken a WHERE a.id < ?1", nativeQuery = true)
    int deleteExpiredTokens(Long timestamp);
}
