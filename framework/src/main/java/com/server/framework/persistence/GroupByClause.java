package com.server.framework.persistence;

import java.util.List;

public class GroupByClause
{
	List<Column> columnList;
	Criteria criteria;

	public GroupByClause(List<Column> columnList, Criteria criteria)
	{
		this.columnList = columnList;
		this.criteria = criteria;
	}

	public GroupByClause(List<Column> columnList)
	{
		this(columnList, null);
	}
}
