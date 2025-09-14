package com.server.framework.persistence;

public class SortColumn
{
	Column column;
	boolean isAscending;

	public SortColumn(Column column, boolean isAscending)
	{
		this.column = column;
		this.isAscending = isAscending;
	}
}
