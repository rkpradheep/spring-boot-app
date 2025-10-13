package com.server.framework.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(name = "WorkflowInstance")
@Table(name = "WorkflowInstance")
public class WorkflowInstanceEntity {

    @Id
    @Column(name = "ReferenceID", nullable = false, length = 255)
    private String referenceID;

    @OneToMany(mappedBy = "workflowInstanceEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<WorkflowEventEntity> workflowEventEntityList;

    @Column(name = "WorkflowName", length = 255, nullable = false)
    private String workflowName;

    @Column(name = "CurrentState", length = 255, nullable = false)
    private String currentState;

    @Column(name = "Status", length = 50, nullable = false)
    private String status;

    @Column(name = "Context", columnDefinition = "TEXT")
    private String context;

    @Column(name = "StartTime", nullable = false)
    private Long startTime;

    @Column(name = "EndTime")
    private Long endTime;

    @Column(name = "LastUpdateTime", nullable = false)
    private Long lastUpdateTime;

    @Column(name = "ErrorMessage", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "Variables", columnDefinition = "TEXT")
    private String variables; // JSON string

    @Column(name = "CreatedBy", length = 100)
    private String createdBy;

    @Column(name = "LastModifiedBy", length = 100)
    private String lastModifiedBy;

    public WorkflowInstanceEntity() {}

    public WorkflowInstanceEntity(String referenceID, String workflowName, String currentState, String status, String createdBy, String lastModifiedBy) {
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
        this.referenceID = referenceID;
        this.workflowName = workflowName;
        this.currentState = currentState;
        this.status = status;
        this.startTime = System.currentTimeMillis();
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public String getReferenceID() { return referenceID; }
    public void setReferenceID(String referenceID) { this.referenceID = referenceID; }

    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }

    public Long getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(Long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getVariables() { return variables; }
    public void setVariables(String variables) { this.variables = variables; }

    public Object getContextAsObject() {
        if (context == null || context.trim().isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(context, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void setContextFromObject(Object contextObj) {
        if (contextObj == null) {
            this.context = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.context = mapper.writeValueAsString(contextObj);
        } catch (JsonProcessingException e) {
            this.context = null;
        }
    }

    public Map<String, Object> getVariablesAsMap() {
        if (variables == null || variables.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(variables, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    public void setVariablesFromMap(Map<String, Object> variablesMap) {
        if (variablesMap == null || variablesMap.isEmpty()) {
            this.variables = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.variables = mapper.writeValueAsString(variablesMap);
        } catch (JsonProcessingException e) {
            this.variables = null;
        }
    }

    public void updateLastUpdateTime() {
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public Long getDuration() {
        if (endTime != null && startTime != null) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public String getCreatedBy() { return createdBy; }
    public String getLastModifiedBy() { return lastModifiedBy; }

    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }
    public void setLastModifiedBy(String lastModifiedBy)
    {
        this.lastModifiedBy = lastModifiedBy;
    }
}
