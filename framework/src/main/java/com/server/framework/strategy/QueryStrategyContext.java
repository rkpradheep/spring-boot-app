package com.server.framework.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class QueryStrategyContext {
    
    private static final Logger LOGGER = Logger.getLogger(QueryStrategyContext.class.getName());
    
    private final List<QueryStrategy> strategies;

    @Autowired
    public QueryStrategyContext(List<QueryStrategy> strategies) {
        this.strategies = strategies;
        LOGGER.log(Level.INFO, "Initialized QueryStrategyContext with {0} strategies", strategies.size());
    }

    public List<Map<String, String>> executeQuery(Connection connection, String query, Map<String, Object> params) throws SQLException {
        QueryStrategy strategy = selectStrategy(query);
        
        if (strategy == null) {
            throw new SQLException("No suitable strategy found for query: " + query);
        }
        
        LOGGER.log(Level.INFO, "Using strategy: {0} for query", strategy.getStrategyType());
        return strategy.executeQuery(connection, query, params);
    }

    private QueryStrategy selectStrategy(String query) {
        for (QueryStrategy strategy : strategies) {
            if (strategy.canHandle(query)) {
                return strategy;
            }
        }
        return null;
    }

    public List<QueryStrategy> getAvailableStrategies() {
        return List.copyOf(strategies);
    }
}
