package com.server.framework.repository;

import com.server.framework.entity.ConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigurationRepository extends JpaRepository<ConfigurationEntity, Long> {

    @Query("SELECT c FROM Configuration c WHERE c.cKey = :ck")
    Optional<ConfigurationEntity> findByCKey(@Param("ck") String cKey);

    @Modifying
    @Query("DELETE FROM Configuration c WHERE c.cKey = :ck")
    void deleteByCKey(@Param("ck") String cKey);
}
