package com.server.framework.strategy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface QueryStrategy {
    
    List<Map<String, String>> executeQuery(Connection connection, String query, Map<String, Object> params) throws SQLException;
    
    String getStrategyType();
    
    boolean canHandle(String query);
}
