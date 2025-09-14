package com.server.framework.builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ApiResponseBuilder {
    
    private final Map<String, Object> response;
    private int statusCode;
    private String message;
    private Object data;
    private Map<String, Object> metadata;
    private boolean success;
    private String errorCode;
    private String timestamp;

    private ApiResponseBuilder() {
        this.response = new HashMap<>();
        this.success = true;
        this.statusCode = 200;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.metadata = new HashMap<>();
    }

    public static ApiResponseBuilder create() {
        return new ApiResponseBuilder();
    }

    public ApiResponseBuilder success(boolean success) {
        this.success = success;
        return this;
    }

    public ApiResponseBuilder statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public ApiResponseBuilder message(String message) {
        this.message = message;
        return this;
    }

    public ApiResponseBuilder data(Object data) {
        this.data = data;
        return this;
    }

    public ApiResponseBuilder errorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public ApiResponseBuilder timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return this;
    }
    
    public ApiResponseBuilder timestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ApiResponseBuilder addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public ApiResponseBuilder metadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    public Map<String, Object> build() {
        response.put("success", success);
        response.put("status_code", statusCode);
        response.put("timestamp", timestamp);
        
        if (message != null) {
            response.put("message", message);
        }
        
        if (data != null) {
            response.put("data", data);
        }
        
        if (errorCode != null) {
            response.put("error_code", errorCode);
        }
        
        if (!metadata.isEmpty()) {
            response.put("metadata", metadata);
        }
        
        return response;
    }

    public static Map<String, Object> success(String message, Object data) {
        return create()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }

    public static Map<String, Object> error(String message, int statusCode) {
        return create()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .build();
    }

    public static Map<String, Object> error(String message, String errorCode, int statusCode) {
        return create()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
