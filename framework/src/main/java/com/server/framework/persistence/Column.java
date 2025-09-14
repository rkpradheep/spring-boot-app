package com.server.framework.persistence;

import org.apache.commons.lang3.StringUtils;

public class Column
{
	String tableName;
	String columnName;
	String aliasName;

	private Column(String tableName, String columnName)
	{
		this.tableName = tableName;
		this.columnName = columnName;
	}

	public Column()
	{

	}

	public String toString()
	{
		if(this instanceof Function)
		{
			if(StringUtils.isEmpty(aliasName))
			{
				throw new RuntimeException("Alias name is mandatory for function column");
			}
			return aliasName;
		}
		return tableName + "." + columnName;
	}

	public static Column getColumn(String tableName, String columnName)
	{
		return new Column(tableName, columnName);
	}

	public void setAliasName(String aliasName)
	{
		this.aliasName = aliasName;
	}
}
