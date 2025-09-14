package com.server.framework.strategy;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class UpdateQueryStrategy implements QueryStrategy {
    
    private static final Logger LOGGER = Logger.getLogger(UpdateQueryStrategy.class.getName());

    @Override
    public List<Map<String, String>> executeQuery(Connection connection, String query, Map<String, Object> params) throws SQLException {
        LOGGER.log(Level.INFO, "Executing UPDATE query: {0}", query);
        
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // Set parameters if any
            if (params != null) {
                setParameters(preparedStatement, params);
            }
            
            int affectedRows = preparedStatement.executeUpdate();
            
            // Validate that only one row is affected for safety
            if (affectedRows > 1) {
                connection.rollback();
                throw new SQLException("Update query affects more than one record: " + affectedRows);
            }
            
            connection.commit();
            
            List<Map<String, String>> result = new ArrayList<>();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("affected_rows", String.valueOf(affectedRows));
            response.put("message", "Update query executed successfully");
            result.add(response);
            
            return result;
        }
    }

    @Override
    public String getStrategyType() {
        return "UPDATE";
    }

    @Override
    public boolean canHandle(String query) {
        return query != null && query.trim().toLowerCase().startsWith("update");
    }

    private void setParameters(PreparedStatement preparedStatement, Map<String, Object> params) throws SQLException {
        int paramIndex = 1;
        for (Object value : params.values()) {
            preparedStatement.setObject(paramIndex++, value);
        }
    }
}
