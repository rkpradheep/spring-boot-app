package com.server.framework.error;

public class AppException extends RuntimeException {
    
    private final String errorCode;
    private Object data;
    
    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public AppException(String errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public AppException(String message) {
        super(message);
        this.errorCode = "internal.error";
    }
    
    public AppException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    public Object getData() {
        return data;
    }
}
