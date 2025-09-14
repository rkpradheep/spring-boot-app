package com.server.framework.repository;

import com.server.framework.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByName(String name);
    
    Optional<UserEntity> findByNameAndPassword(String name, String password);

}
