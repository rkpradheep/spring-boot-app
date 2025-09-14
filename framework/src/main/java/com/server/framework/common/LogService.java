package com.server.framework.common;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogService {
    
    private static final Logger LOGGER = Logger.getLogger(LogService.class.getName());
    
    public static void logMethodEntry(String className, String methodName, Object... params) {
        if (LOGGER.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("→ ").append(className).append(".").append(methodName).append("(");
            
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(params[i] != null ? params[i].toString() : "null");
            }
            sb.append(")");
            
            LOGGER.log(Level.FINE, sb.toString());
        }
    }
    
    public static void logMethodExit(String className, String methodName, Object returnValue) {
        if (LOGGER.isLoggable(Level.FINE)) {
            String returnStr = returnValue != null ? returnValue.toString() : "void";
            LOGGER.log(Level.FINE, "← " + className + "." + methodName + " = " + returnStr);
        }
    }
    
    public static void logPerformance(String operation, long startTime, long endTime) {
        long duration = endTime - startTime;
        String level = duration > 1000 ? "WARNING" : "INFO";
        LOGGER.log(Level.INFO, "⏱️  " + operation + " completed in " + duration + "ms");
    }
    
    public static void logDatabaseOperation(String operation, String table, int affectedRows) {
        LOGGER.log(Level.INFO, "🗄️  DB " + operation + " on " + table + " affected " + affectedRows + " rows");
    }
    
    public static void logHttpRequest(String method, String uri, int statusCode, long duration) {
        String statusColor = statusCode >= 400 ? "❌" : statusCode >= 300 ? "⚠️" : "✅";
        LOGGER.log(Level.INFO, statusColor + " " + method + " " + uri + " " + statusCode + " (" + duration + "ms)");
    }
    
    public static void logSecurityEvent(String event, String user, String details) {
        LOGGER.log(Level.WARNING, "🔒 SECURITY: " + event + " - User: " + user + " - " + details);
    }
    
    public static void logBusinessEvent(String event, String entity, String action) {
        LOGGER.log(Level.INFO, "💼 BUSINESS: " + event + " - " + entity + " - " + action);
    }
    
    public static void logSystemEvent(String event, String component, String status) {
        String icon = "success".equals(status) ? "✅" : "error".equals(status) ? "❌" : "ℹ️";
        LOGGER.log(Level.INFO, icon + " SYSTEM: " + event + " - " + component + " - " + status);
    }
    
    public static void logStructured(String level, String category, String message, Object... data) {
        Level logLevel = Level.parse(level.toUpperCase());
        if (LOGGER.isLoggable(logLevel)) {
            StringBuilder sb = new StringBuilder();
            sb.append("📊 ").append(category).append(": ").append(message);
            
            if (data.length > 0) {
                sb.append(" | Data: ");
                for (int i = 0; i < data.length; i += 2) {
                    if (i > 0) sb.append(", ");
                    if (i + 1 < data.length) {
                        sb.append(data[i]).append("=").append(data[i + 1]);
                    }
                }
            }
            
            LOGGER.log(logLevel, sb.toString());
        }
    }
}
