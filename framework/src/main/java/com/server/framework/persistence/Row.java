package com.server.framework.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Row
{
	String tableName;
	Map<String, Object> rowMap = new LinkedHashMap<>();

	public Row(String tableName)
	{
		this.tableName = tableName;
		initPK();
	}

	public void initPK()
	{
		try
		{
			List<String> pkList = DBUtil.getPKList(tableName);

			for(String pk : pkList)
			{
				set(tableName, pk, new UVH());
			}
		}
		catch(Exception e)
		{
			throw new RuntimeException("Exception occurred while populating PK");
		}
	}

	public void set(String columnName, Object columnValue)
	{
		rowMap.put(tableName + "."  + columnName, columnValue);
	}

	public void set(String tableName, String columnName, Object columnValue)
	{
		rowMap.put(tableName + "." + columnName, columnValue);
	}

	public Object get(String columnName)
	{
		return rowMap.get(tableName + "." +  columnName);
	}

	public Object get(String tableName, String columnName)
	{
		return rowMap.get(tableName + "." + columnName);
	}
	public Object get(Column column)
	{
		return rowMap.get(column.tableName + "." + column.columnName);
	}

	public Map<String, Object> getRowMap()
	{
		return rowMap;
	}

	public String getTableName()
	{
		return tableName;
	}

	public String toString()
	{
		StringBuilder rowBuilder = new StringBuilder();
		rowBuilder.append("<");

		for(Map.Entry<String, Object> rowEntry : rowMap.entrySet())
		{
			rowBuilder.append(rowEntry.getKey()).append("=").append(rowEntry.getValue()).append(StringUtils.SPACE);
		}
		rowBuilder.append("/>");
		return rowBuilder.toString();
	}

}
