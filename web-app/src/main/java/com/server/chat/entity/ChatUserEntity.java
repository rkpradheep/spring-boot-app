package com.server.chat.entity;

import jakarta.persistence.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "ChatUser")
@Table(name = "ChatUser")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatUserEntity
{
    
    @Id
    @Column(name = "`Id`")
    private Long id;
    
    @Column(name = "`Name`", nullable = false, length = 255)
    private String name;

    @OneToMany(mappedBy = "chatUser", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ChatUserDetailEntity> chatUserDetailEntities;

    public ChatUserEntity() {}
    
    public ChatUserEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<ChatUserDetailEntity> getChatUserDetails() {
        return chatUserDetailEntities;
    }
    
    public void setChatUserDetails(List<ChatUserDetailEntity> chatUserDetailEntities) {
        this.chatUserDetailEntities = chatUserDetailEntities;
    }
    
    @Override
    public String toString() {
        return "ChatUser{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
