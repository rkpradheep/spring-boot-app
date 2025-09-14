package com.server.framework.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import com.server.framework.id.CustomIdGenerator;

@Entity(name = "SessionManagement")
@Table(name = "SessionManagement")
public class SessionManagementEntity
{
    
    @Id
    @GeneratedValue(generator = "custom-id")
    @GenericGenerator(name = "custom-id", type = CustomIdGenerator.class)
    @Column(name = "Id")
    private Long id;
    
    @Column(name = "UserId", nullable = false)
    private Long userId;
    
    @Column(name = "ExpiryTime", nullable = false)
    private Long expiryTime;
    
    @Column(name = "SessionId", nullable = false, columnDefinition = "LONGTEXT")
    private String sessionId;

    public SessionManagementEntity() {}
    
    public SessionManagementEntity(Long id, Long userId, Long expiryTime, String sessionId) {
        this.id = id;
        this.userId = userId;
        this.expiryTime = expiryTime;
        this.sessionId = sessionId;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getExpiryTime() {
        return expiryTime;
    }
    
    public void setExpiryTime(Long expiryTime) {
        this.expiryTime = expiryTime;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    
    @Override
    public String toString() {
        return "SessionManagement{" +
                "id=" + id +
                ", userId=" + userId +
                ", expiryTime=" + expiryTime +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
