package com.server.zoho.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

@Entity(name = "BuildMonitor")
@Table(name = "BuildMonitor")
public class BuildMonitorEntity
{
    
    @Id
    @GeneratedValue(generator = "custom-id")
    @GenericGenerator(name = "custom-id", type = com.server.framework.id.CustomIdGenerator.class)
    @Column(name = "Id")
    private Long id;


    @Column(name = "Status", length = 50, nullable = false)
    private String status; // ACTIVE, COMPLETED, FAILED, PAUSED
    
    @Column(name = "StartTime", nullable = false)
    private Long startTime;
    
    @Column(name = "LastUpdateTime", nullable = false)
    private Long lastUpdateTime;
    
    @Column(name = "TotalProducts", nullable = false)
    private Integer totalProducts;
    
    @Column(name = "CompletedProducts", nullable = false)
    private Integer completedProducts;

    public BuildMonitorEntity() {}
    
    public BuildMonitorEntity(Integer totalProducts) {
        this.status = "ACTIVE";
        this.startTime = System.currentTimeMillis();
        this.lastUpdateTime = System.currentTimeMillis();
        this.totalProducts = totalProducts;
        this.completedProducts = 0;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }


    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }
    
    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public Integer getTotalProducts() {
        return totalProducts;
    }
    
    public void setTotalProducts(Integer totalProducts) {
        this.totalProducts = totalProducts;
    }
    
    public Integer getCompletedProducts() {
        return completedProducts;
    }
    
    public void setCompletedProducts(Integer completedProducts) {
        this.completedProducts = completedProducts;
    }

    public void updateLastUpdateTime() {
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public void markCompleted() {
        this.completedProducts++;
        updateLastUpdateTime();
    }
    
    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }
    
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
    
    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }
}
