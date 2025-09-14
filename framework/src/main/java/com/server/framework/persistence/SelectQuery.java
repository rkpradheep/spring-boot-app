package com.server.framework.persistence;

import java.util.ArrayList;
import java.util.List;

public class SelectQuery
{
	String tableName;
	Criteria criteria;
	List<Join> joinList = new ArrayList<>();
	boolean needLock = false;
	List<SortColumn> sortColumnList = new ArrayList<>();
	Range range;
	List<Column> selectColumnList = new ArrayList<>();
	GroupByClause groupByClause;

	public SelectQuery(String tableName)
	{
		this.tableName = tableName;
	}

	public void setCriteria(Criteria criteria)
	{
		this.criteria = criteria;
	}
	public void setGroupByClause(GroupByClause groupByClause)
	{
		this.groupByClause = groupByClause;
	}

	public void addSortColumn(SortColumn sortColumn)
	{
		this.sortColumnList.add(sortColumn);
	}

	public void setRange(Range range)
	{
		this.range = range;
	}

	public void setLock()
	{
		this.needLock = true;
	}

	public void addJoin(Join join, String joinType)
	{
		join.joinType = joinType;
		joinList.add(join);
	}

	public void addSelectColumn(Column column)
	{
		selectColumnList.add(column);
	}
}
