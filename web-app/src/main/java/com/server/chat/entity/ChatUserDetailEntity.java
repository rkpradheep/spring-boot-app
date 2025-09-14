package com.server.chat.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "`ChatUserDetail`")
@Table(name = "`ChatUserDetail`")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatUserDetailEntity
{
    
    @Id
    @Column(name = "Id")
    @GeneratedValue(generator = "custom-id")
    @org.hibernate.annotations.GenericGenerator(name = "custom-id", type = com.server.framework.id.CustomIdGenerator.class)
    private Long id;
    
    @Column(name = "`Message`", nullable = false, columnDefinition = "TEXT")
    private String message;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ChatUserId", nullable = false)
    @JsonBackReference
    private ChatUserEntity chatUser;

    public ChatUserDetailEntity() {}
    public ChatUserDetailEntity(ChatUserEntity chatUser, String message) {
        this.chatUser = chatUser;
        this.message = message;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public ChatUserEntity getChatUser() {
        return chatUser;
    }
    
    public void setChatUser(ChatUserEntity chatUserEntity) {
        this.chatUser = chatUserEntity;
    }
    
    @Override
    public String toString() {
        return "ChatUserDetail{" +
                "id=" + id +
                ", chatUserId=" + chatUser.getId() +
                ", message='" + message + '\'' +
                '}';
    }
}
