package com.server.framework.workflow.model;

import java.util.HashMap;
import java.util.Map;

import com.server.framework.workflow.definition.WorkFlowEventType;

public class WorkflowEvent {
    
    private String eventType;
    private Map<String, Object> payload;
    private Long timestamp;
    private String source;
    private String correlationId;

    public WorkflowEvent() {
        this.payload = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public WorkflowEvent(String eventType) {
        this();
        this.eventType = eventType;
    }

    public WorkflowEvent(String eventType, Map<String, Object> payload) {
        this(eventType);
        this.payload = payload != null ? payload : new HashMap<>();
    }

    public WorkflowEvent(WorkFlowEventType eventType) {
        this(eventType.getValue());
    }

    public WorkflowEvent(WorkFlowEventType eventType, Map<String, Object> payload) {
        this(eventType.getValue());
        this.payload = payload != null ? payload : new HashMap<>();
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public void addPayload(String key, Object value) {
        this.payload.put(key, value);
    }

    public Object getPayloadValue(String key) {
        return this.payload.get(key);
    }

    public boolean hasPayload(String key) {
        return this.payload.containsKey(key);
    }

}
