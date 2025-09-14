package com.server.framework.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Map;

@Entity(name = "WorkflowEvent")
@Table(name = "WorkflowEvent")
public class WorkflowEventEntity {

    @Id
    @GeneratedValue(generator = "custom-id")
    @GenericGenerator(name = "custom-id", type = com.server.framework.id.CustomIdGenerator.class)
    @Column(name = "Id")
    private Long id;

    @Column(name = "ReferenceID", length = 255, nullable = false)
    private String referenceID;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReferenceID", insertable=false, updatable=false)
    @JsonBackReference
    private WorkflowInstanceEntity workflowInstanceEntity;


    @Column(name = "EventType", length = 255, nullable = false)
    private String eventType;

    @Column(name = "Payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "Timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "Source", length = 255)
    private String source;

    @Column(name = "CorrelationId", length = 255)
    private String correlationId;

    public WorkflowEventEntity() {}

    public WorkflowEventEntity(String referenceID, String eventType) {
        this.referenceID = referenceID;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }

    public WorkflowEventEntity(String referenceID, String eventType, Map<String, Object> payload) {
        this.referenceID = referenceID;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
        setPayloadFromMap(payload);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReferenceID() { return referenceID; }
    public void setReferenceID(String referenceID) { this.referenceID = referenceID; }


    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Map<String, Object> getPayloadAsMap() {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(payload, Map.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void setPayloadFromMap(Map<String, Object> payloadMap) {
        if (payloadMap == null || payloadMap.isEmpty()) {
            this.payload = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.payload = mapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            this.payload = null;
        }
    }
}
