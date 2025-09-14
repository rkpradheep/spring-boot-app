package com.server.chat.repository;

import com.server.chat.entity.ChatUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatUserRepository extends JpaRepository<ChatUserEntity, Long> {

    Optional<ChatUserEntity> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT c FROM ChatUser c WHERE UPPER(c.name) = UPPER(:name)")
    Optional<ChatUserEntity> findByNameIgnoreCase(@Param("name") String name);
}
