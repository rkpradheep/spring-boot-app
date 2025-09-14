package com.server.framework.persistence;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class Criteria
{
	public static class Constants
	{
		public static final String EQUAL = "=";
		public static final String NOT_EQUAL = "!=";
		public static final String LESS_THAN = "<";
		public static final String GREATER_THAN = ">";
		public static final String LESS_THAN_EQUAL = "<=";
		public static final String GREATER_THAN_EQUAL = ">=";
		public static final String LIKE = "LIKE";
		public static final String IN = "IN";
		public static final String NOT_IN = "NOT IN";
		public static final String AND = "AND";
		public static final String OR = "OR";
		public static final String IS_NULL = "IS_NULL";
		public static final String IS_NOT_NULL = "IS_NOT_NULL";
	}

	Criteria leftCriteria;
	Criteria rightCriteria;
	Criterion criterion;
	String operator;

	static class Criterion
	{
		Column column;
		Object columnValue;
		String comparator;

		private Criterion(Column column, Object value, String comparator)
		{
			this.column = column;
			this.columnValue = value;
			this.comparator = comparator;
		}

		private boolean matches(Object lhsValue, Object rhsValue)
		{
			String lhsValueString = String.valueOf(lhsValue);
			String rhsValueString = String.valueOf(lhsValue);
			return switch(comparator)
				{
					case Constants.EQUAL -> Objects.equals(lhsValue, rhsValue);
					case Constants.NOT_EQUAL -> !Objects.equals(lhsValue, rhsValue);
					case Constants.IS_NOT_NULL -> Objects.nonNull(lhsValue);
					case Constants.IS_NULL -> Objects.isNull(lhsValue);
					case Constants.LESS_THAN -> Long.parseLong(lhsValueString) < Long.parseLong(rhsValueString);
					case Constants.GREATER_THAN -> Long.parseLong(lhsValueString) > Long.parseLong(rhsValueString);
					case Constants.LESS_THAN_EQUAL -> Long.parseLong(lhsValueString) <= Long.parseLong(rhsValueString);
					case Constants.GREATER_THAN_EQUAL -> Long.parseLong(lhsValueString) >= Long.parseLong(rhsValueString);
					case Constants.LIKE -> StringUtils.contains(lhsValueString, rhsValueString);
					case Constants.IN -> ((List) rhsValue).contains(rhsValue);
					case Constants.NOT_IN -> !((List) rhsValue).contains(rhsValue);
					default -> false;
				};
		}

		public boolean matches(Row row)
		{
			Object lhsValue = row.get(column);
			Object rhsValue = columnValue;

			return this.matches(lhsValue, rhsValue);
		}
	}

	public Criteria(Column column, Object columnValue, String comparator)
	{
		this.criterion = new Criterion(column, columnValue, comparator);
		this.leftCriteria = null;
		this.rightCriteria = null;
	}

	public Criteria(String tableName, String columnName, Object columnValue, String comparator)
	{
		this(Column.getColumn(tableName, columnName), columnValue, comparator);
	}

	private Criteria()
	{
		this.criterion = null;
		this.leftCriteria = null;
		this.rightCriteria = null;
	}

	public Criteria and(Criteria criteria)
	{
		Criteria cr = new Criteria();

		cr.leftCriteria = this;

		cr.operator = Constants.AND;

		cr.rightCriteria = criteria;

		return cr;
	}

	public Criteria or(Criteria criteria)
	{
		Criteria cr = new Criteria();

		cr.leftCriteria = this;

		cr.operator = Constants.OR;

		cr.rightCriteria = criteria;

		return cr;
	}

	public boolean matches(Row row)
	{
		boolean result;
		if(Objects.nonNull(this.criterion))
		{
			result = this.criterion.matches(row);
		}
		else if(StringUtils.equals(Constants.AND, operator))
		{
			result = this.leftCriteria.matches(row) && this.rightCriteria.matches(row);
		}
		else
		{
			result = this.leftCriteria.matches(row) || this.rightCriteria.matches(row);
		}

		return result;
	}

}