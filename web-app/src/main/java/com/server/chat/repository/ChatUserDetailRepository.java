package com.server.chat.repository;

import com.server.chat.entity.ChatUserDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatUserDetailRepository extends JpaRepository<ChatUserDetailEntity, Long> {

    List<ChatUserDetailEntity> findByChatUserId(Long chatUserId);
    List<ChatUserDetailEntity> findByChatUserIdOrderById(Long chatUserId);
    void deleteByChatUserId(Long chatUserId);

    List<ChatUserDetailEntity> findByChatUser_NameOrderById(String name);
}
