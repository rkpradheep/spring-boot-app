package com.server.framework.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.GenericGenerator;

@Entity(name = "HttpLog")
@Table(name = "HttpLog")
public class HttpLogEntity
{
    
    @Id
    @Column(name = "Id")
    @GeneratedValue(generator = "custom-id")
    @GenericGenerator(name = "custom-id", type = com.server.framework.id.CustomIdGenerator.class)
    private Long id;
    
    @Column(name = "Url", nullable = false, length = 255)
    private String url;
    
    @Column(name = "Method", nullable = false, length = 255)
    private String method;
    
    @Column(name = "IP", length = 255)
    private String ip;
    
    @Column(name = "Parameters", columnDefinition = "TEXT")
    private String parameters;
    
    @Column(name = "RequestHeaders", columnDefinition = "TEXT")
    private String requestHeaders;
    
    @Column(name = "ResponseHeaders", columnDefinition = "TEXT")
    private String responseHeaders;
    
    @Column(name = "RequestData", columnDefinition = "LONGTEXT")
    private String requestData;
    
    @Column(name = "ResponseData", columnDefinition = "LONGTEXT")
    private String responseData;
    
    @Column(name = "ThreadName", nullable = false, length = 255)
    private String threadName;
    
    @Column(name = "CreatedTime", nullable = false)
    private Long createdTime;
    
    @Column(name = "EntityType", nullable = false)
    private Integer entityType;
    
    @Column(name = "StatusCode")
    private Integer statusCode;
    
    @Column(name = "IsOutgoing", nullable = false)
    private Boolean isOutgoing;

    public HttpLogEntity() {}

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public String getParameters() {
        return parameters;
    }
    
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    
    public String getRequestHeaders() {
        return requestHeaders;
    }
    
    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }
    
    public String getResponseHeaders() {
        return responseHeaders;
    }
    
    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }
    
    public String getRequestData() {
        return requestData;
    }
    
    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }
    
    public String getResponseData() {
        return responseData;
    }
    
    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }
    
    public String getThreadName() {
        return threadName;
    }
    
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
    
    public Long getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }
    
    public Integer getEntityType() {
        return entityType;
    }
    
    public void setEntityType(Integer entityType) {
        this.entityType = entityType;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    public Boolean getIsOutgoing() {
        return isOutgoing;
    }
    
    public void setIsOutgoing(Boolean isOutgoing) {
        this.isOutgoing = isOutgoing;
    }
    
    @Override
    public String toString() {
        return "HttpLog{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", ip='" + ip + '\'' +
                ", threadName='" + threadName + '\'' +
                ", createdTime=" + createdTime +
                ", entityType=" + entityType +
                ", statusCode=" + statusCode +
                ", isOutgoing=" + isOutgoing +
                '}';
    }
}
