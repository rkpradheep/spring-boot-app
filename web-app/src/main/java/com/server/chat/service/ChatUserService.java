package com.server.chat.service;

import com.server.chat.entity.ChatUserEntity;
import com.server.chat.repository.ChatUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatUserService {
    
    @Autowired
    private ChatUserRepository chatUserRepository;
    
    public ChatUserEntity save(ChatUserEntity chatUserEntity) {
        return chatUserRepository.save(chatUserEntity);
    }
    
    public Optional<ChatUserEntity> findById(Long id) {
        return chatUserRepository.findById(id);
    }
    
    public Optional<ChatUserEntity> findByName(String name) {
        return chatUserRepository.findByName(name);
    }
    
    public Optional<ChatUserEntity> findByNameIgnoreCase(String name) {
        return chatUserRepository.findByNameIgnoreCase(name);
    }
    
    public List<ChatUserEntity> findAll() {
        return chatUserRepository.findAll();
    }
    
    public boolean existsByNameIgnoreCase(String name) {
        return chatUserRepository.existsByNameIgnoreCase(name);
    }
    
    public void deleteById(Long id) {
        chatUserRepository.deleteById(id);
    }
    
    public void delete(ChatUserEntity chatUserEntity) {
        chatUserRepository.delete(chatUserEntity);
    }
    
    public ChatUserEntity addOrGetUser(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        
        String trimmedName = name.trim();
        Optional<ChatUserEntity> existingUser = findByNameIgnoreCase(trimmedName);
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        ChatUserEntity newUser = new ChatUserEntity();
        newUser.setId(generateNextUserId());
        newUser.setName(trimmedName);
        ChatUserEntity savedUser = save(newUser);
        
        return savedUser;
    }
    
    public ChatUserEntity createUser(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        
        ChatUserEntity chatUserEntity = new ChatUserEntity();
        chatUserEntity.setId(generateNextUserId());
        chatUserEntity.setName(name.trim());
        return save(chatUserEntity);
    }
    
    private Long generateNextUserId() {
        Long maxId = chatUserRepository.findAll()
            .stream()
            .map(ChatUserEntity::getId)
            .max(Long::compareTo)
            .orElse(1000000055000L);
        return maxId + 1;
    }

    public Optional<ChatUserEntity> findUserWithDetails(Long userId) {
        Optional<ChatUserEntity> userOpt = chatUserRepository.findById(userId);
        userOpt.ifPresent(user -> {
            if (user.getChatUserDetails() != null) {
                user.getChatUserDetails().size();
            }
        });
        return userOpt;
    }
}
