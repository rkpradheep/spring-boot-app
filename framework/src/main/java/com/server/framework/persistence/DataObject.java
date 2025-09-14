package com.server.framework.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataObject
{
	private transient Map<String, List<Row>> tableUniqueRowList = new HashMap<>();

	public void addRow(Row row)
	{
		String tableName = row instanceof RowWrapper ? "NULL" : row.getTableName();
		List<Row> rowList = tableUniqueRowList.getOrDefault(tableName, new ArrayList<>());
		rowList.add(row);
		tableUniqueRowList.put(tableName, rowList);
	}

	public List<Row> getRows()
	{
		return tableUniqueRowList.values().stream().flatMap(Collection::stream).toList();
	}

	public Row getFirstRow(String tableName)
	{
		return getRows(tableName).get(0);
	}

	public List<String> getTables()
	{
		return new ArrayList<>(tableUniqueRowList.keySet());
	}

	public List<Row> getRows(String tableName)
	{
		return tableUniqueRowList.get(tableName);
	}

	public List<Row> getRows(Criteria criteria)
	{
		List<Row> filteredRowList = new ArrayList<>();
		for(Row row : getRows())
		{
			if(criteria.matches(row))
			{
				filteredRowList.add(row);
			}
		}
		return filteredRowList;
	}

	public boolean isEmpty()
	{
		return getRows().isEmpty();
	}

	public String toString()
	{
		StringBuilder dataObjectBuilder = new StringBuilder();
		dataObjectBuilder.append("<Rows>").append(System.lineSeparator());
		for(Row row : getRows())
		{
			dataObjectBuilder.append(row).append(System.lineSeparator());
		}
		dataObjectBuilder.append("</Rows>").append(System.lineSeparator());
		return dataObjectBuilder.toString();
	}
}
