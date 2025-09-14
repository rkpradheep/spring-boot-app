package com.server.framework.persistence;

import java.util.HashMap;
import java.util.Map;

public class UpdateQuery
{
	String tableName;
	Criteria criteria;
	Map<String, Object> columnNameValueMap = new HashMap<>();

	public UpdateQuery(String tableName)
	{
		this.tableName = tableName;
	}
	public void setCriteria(Criteria criteria)
	{
		this.criteria = criteria;
	}
	public void setValue(String columnName, Object columnValue)
	{
		columnNameValueMap.put(columnName, columnValue);
	}

}
