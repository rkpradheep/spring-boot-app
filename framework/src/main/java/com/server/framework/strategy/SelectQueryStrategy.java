package com.server.framework.strategy;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class SelectQueryStrategy implements QueryStrategy {
    
    private static final Logger LOGGER = Logger.getLogger(SelectQueryStrategy.class.getName());

    @Override
    public List<Map<String, String>> executeQuery(Connection connection, String query, Map<String, Object> params) throws SQLException {
        LOGGER.log(Level.INFO, "Executing SELECT query: {0}", query);
        
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // Set parameters if any
            if (params != null) {
                setParameters(preparedStatement, params);
            }
            
            preparedStatement.executeQuery();
            return processResultSet(preparedStatement);
        }
    }

    @Override
    public String getStrategyType() {
        return "SELECT";
    }

    @Override
    public boolean canHandle(String query) {
        return query != null && query.trim().toLowerCase().startsWith("select");
    }

    private List<Map<String, String>> processResultSet(PreparedStatement preparedStatement) throws SQLException {
        try (ResultSet resultSet = preparedStatement.getResultSet()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<Map<String, String>> results = new ArrayList<>();
            
            while (resultSet.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnLabel(i).toUpperCase();
                    String value = resultSet.getString(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
            
            // Return empty row if no results
            if (results.isEmpty()) {
                Map<String, String> emptyRow = new LinkedHashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    emptyRow.put(metaData.getColumnName(i).toUpperCase(), "<EMPTY>");
                }
                results.add(emptyRow);
            }
            
            return results;
        }
    }

    private void setParameters(PreparedStatement preparedStatement, Map<String, Object> params) throws SQLException {
        int paramIndex = 1;
        for (Object value : params.values()) {
            preparedStatement.setObject(paramIndex++, value);
        }
    }
}
