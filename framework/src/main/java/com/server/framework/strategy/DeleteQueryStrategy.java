package com.server.framework.strategy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class DeleteQueryStrategy implements QueryStrategy
{
	@Override public List<Map<String, String>> executeQuery(Connection connection, String query, Map<String, Object> params) throws SQLException
	{
		return List.of();
	}

	@Override public String getStrategyType()
	{
		return "";
	}

	@Override public boolean canHandle(String query)
	{
		return false;
	}
}
