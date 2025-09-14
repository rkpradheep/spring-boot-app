package com.server.chat.service;

import com.server.chat.entity.ChatUserEntity;
import com.server.chat.entity.ChatUserDetailEntity;
import com.server.chat.repository.ChatUserDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatUserDetailService {
    
    @Autowired
    private ChatUserDetailRepository chatUserDetailRepository;
    
    @Autowired
    private ChatUserService chatUserService;
    
    public ChatUserDetailEntity save(ChatUserDetailEntity chatUserDetailEntity) {
        return chatUserDetailRepository.save(chatUserDetailEntity);
    }
    
    public Optional<ChatUserDetailEntity> findById(Long id) {
        return chatUserDetailRepository.findById(id);
    }
    
    public List<ChatUserDetailEntity> findByChatUserId(Long chatUserId) {
        return chatUserDetailRepository.findByChatUserId(chatUserId);
    }
    
    public List<ChatUserDetailEntity> findByChatUserIdOrderById(Long chatUserId) {
        return chatUserDetailRepository.findByChatUserIdOrderById(chatUserId);
    }
    
    public List<ChatUserDetailEntity> findByUserNameOrderById(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        return chatUserDetailRepository.findByChatUser_NameOrderById(name.trim());
    }
    
    public List<ChatUserDetailEntity> findAll() {
        return chatUserDetailRepository.findAll();
    }
    
    public void deleteById(Long id) {
        chatUserDetailRepository.deleteById(id);
    }
    
    public void deleteByChatUserId(Long chatUserId) {
        chatUserDetailRepository.deleteByChatUserId(chatUserId);
    }
    
    public void delete(ChatUserDetailEntity chatUserDetailEntity) {
        chatUserDetailRepository.delete(chatUserDetailEntity);
    }

    public void addMessage(String userName, String message) {
        ChatUserEntity chatUserEntity = chatUserService.addOrGetUser(userName);
        ChatUserDetailEntity chatUserDetailEntity = new ChatUserDetailEntity(chatUserEntity, message);
        save(chatUserDetailEntity);
    }

    public String getPreviousMessage(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return "";
        }
        
        List<ChatUserDetailEntity> messages = findByUserNameOrderById(userName.trim());
        
        StringBuilder previousMessage = new StringBuilder();
        for (ChatUserDetailEntity detail : messages) {
            previousMessage.append(detail.getMessage());
        }
        
        return previousMessage.toString();
    }
    public ChatUserDetailEntity createMessage(ChatUserEntity chatUserEntity, String message) {
        ChatUserDetailEntity chatUserDetailEntity = new ChatUserDetailEntity(chatUserEntity, message);
        return save(chatUserDetailEntity);
    }

    public ChatUserDetailEntity createMessage(String userName, String message) {
        ChatUserEntity chatUserEntity = chatUserService.addOrGetUser(userName);
        return createMessage(chatUserEntity, message);
    }
}
