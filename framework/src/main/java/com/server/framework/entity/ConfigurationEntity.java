package com.server.framework.entity;

import jakarta.persistence.*;

@Entity(name = "Configuration")
@Table(name = "Configuration")
public class ConfigurationEntity
{
    
    @Id
    @Column(name = "Id")
    private Long id;
    
    @Column(name = "CKey", nullable = false, length = 255)
    private String cKey;
    
    @Column(name = "CValue", nullable = false, columnDefinition = "LONGTEXT")
    private String cValue;

    public ConfigurationEntity() {}
    
    public ConfigurationEntity(Long id, String cKey, String cValue) {
        this.id = id;
        this.cKey = cKey;
        this.cValue = cValue;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCKey() {
        return cKey;
    }
    
    public void setCKey(String cKey) {
        this.cKey = cKey;
    }
    
    public String getCValue() {
        return cValue;
    }
    
    public void setCValue(String cValue) {
        this.cValue = cValue;
    }
    
    @Override
    public String toString() {
        return "Configuration{" +
                "id=" + id +
                ", cKey='" + cKey + '\'' +
                ", cValue='" + cValue + '\'' +
                '}';
    }
}
