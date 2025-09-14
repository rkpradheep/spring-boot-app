package com.server.stats.meta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResponseMeta
{
	private Map<String,String> columnMeta = new LinkedHashMap<>();
	private List<String> columnPHList = new ArrayList<>();

	public void addResponseColumn(String columnName, String columnValue, boolean isPH)
	{
		columnMeta.put(columnName, columnValue);
		if(isPH)
		{
			columnPHList.add(columnName);
		}
	}

	public List<String> getResponseColumns()
	{
		return columnMeta.keySet().stream().toList();
	}

	public String getColumnValue(String columnName)
	{
		return columnMeta.get(columnName);
	}

	public boolean isPHColumn(String columnName)
	{
		return columnPHList.contains(columnName);
	}
}
