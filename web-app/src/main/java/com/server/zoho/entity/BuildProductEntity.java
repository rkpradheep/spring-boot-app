package com.server.zoho.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

@Entity(name = "BuildProduct")
@Table(name = "BuildProduct")
public class BuildProductEntity
{

    @Id
    @GeneratedValue(generator = "custom-id")
    @GenericGenerator(name = "custom-id", type = com.server.framework.id.CustomIdGenerator.class)
    @Column(name = "Id")
    private Long id;

    @Column(name = "BuildMonitorId", nullable = false)
    private Long buildMonitorId;

    @Column(name = "ProductName", nullable = false, length = 100)
    private String productName;

    @Column(name = "BuildId")
    private Long buildId;

    @Column(name = "Status", nullable = false, length = 50)
    private String status; // PENDING, STARTED, SUCCESS, FAILED, ERROR

    @Column(name = "StartTime")
    private Long startTime;

    @Column(name = "EndTime")
    private Long endTime;

    @Column(name = "Duration")
    private Long duration; // in milliseconds

    @Column(name = "ErrorMessage", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "MilestoneVersion")
    private String milestoneVersion;

    @Column(name = "ChannelUrl")
    private String channelUrl;

    @Column(name = "OrderIndex", nullable = false)
    private Integer orderIndex; // Order in which products should be built

    @Column(name = "CreatedTime", nullable = false)
    private Long createdTime;

    @Column(name = "UpdatedTime", nullable = false)
    private Long updatedTime;

    public BuildProductEntity() {
        this.createdTime = System.currentTimeMillis();
        this.updatedTime = System.currentTimeMillis();
        this.status = "PENDING";
    }

    public BuildProductEntity(Long buildMonitorId, String productName, Integer orderIndex) {
        this();
        this.buildMonitorId = buildMonitorId;
        this.productName = productName;
        this.orderIndex = orderIndex;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBuildMonitorId() {
        return buildMonitorId;
    }

    public void setBuildMonitorId(Long buildMonitorId) {
        this.buildMonitorId = buildMonitorId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getBuildId() {
        return buildId;
    }

    public void setBuildId(Long buildId) {
        this.buildId = buildId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedTime = System.currentTimeMillis();
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
        this.updatedTime = System.currentTimeMillis();
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
        this.updatedTime = System.currentTimeMillis();
        if (this.startTime != null) {
            this.duration = endTime - this.startTime;
        }
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedTime = System.currentTimeMillis();
    }

    public String getMilestoneVersion() {
        return milestoneVersion;
    }

    public void setMilestoneVersion(String milestoneVersion) {
        this.milestoneVersion = milestoneVersion;
        this.updatedTime = System.currentTimeMillis();
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
        this.updatedTime = System.currentTimeMillis();
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public void updateUpdatedTime() {
        this.updatedTime = System.currentTimeMillis();
    }

    public void markAsStarted(Long buildId) {
        this.status = "STARTED";
        this.buildId = buildId;
        this.startTime = System.currentTimeMillis();
        this.updateUpdatedTime();
    }

    public void markAsSuccess() {
        this.status = "SUCCESS";
        this.endTime = System.currentTimeMillis();
        if (this.startTime != null) {
            this.duration = this.endTime - this.startTime;
        }
        this.updateUpdatedTime();
    }

    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.endTime = System.currentTimeMillis();
        if (this.startTime != null) {
            this.duration = this.endTime - this.startTime;
        }
        this.updateUpdatedTime();
    }

    public boolean isCompleted() {
        return "SUCCESS".equals(this.status) || "FAILED".equals(this.status) || "ERROR".equals(this.status);
    }

    public boolean isInProgress() {
        return "STARTED".equals(this.status);
    }

    public boolean isPending() {
        return "PENDING".equals(this.status);
    }
}
