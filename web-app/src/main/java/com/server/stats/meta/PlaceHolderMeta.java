package com.server.stats.meta;

import java.util.Map;

public class PlaceHolderMeta
{
	private StatsMeta statsMeta;
	private String columnName;
	private Object columnValue;
	private Map<String, String> requestDataRow;

	public PlaceHolderMeta(String columnName, Object columnValue)
	{
		this.columnName = columnName;
		this.columnValue = columnValue;
	}

	public Map<String, String> getRequestDataRow()
	{
		return requestDataRow;
	}

	public void setRequestDataRow(Map<String, String> requestDataRow)
	{
		this.requestDataRow = requestDataRow;
	}

	public StatsMeta getStatsMeta()
	{
		return statsMeta;
	}

	public void setStatsMeta(StatsMeta statsMeta)
	{
		this.statsMeta = statsMeta;
	}

	public String getColumnName()
	{
		return columnName;
	}

	public Object getColumnValue()
	{
		return columnValue;
	}
}
