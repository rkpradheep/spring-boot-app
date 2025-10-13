package com.server.framework.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import com.server.framework.entity.LockEntity;

public interface LockRepository extends JpaRepository<LockEntity, String>
{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	LockEntity findByName(@Param("name") String name);
}
