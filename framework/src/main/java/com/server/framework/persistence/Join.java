package com.server.framework.persistence;

public class Join
{
	String baseTableName;
	String baseTableColumnName;
	String referenceTableName;
	String referenceTableColumnName;
	String joinType;

	public Join(String baseTableName, String baseTableColumnName, String referenceTableName, String referenceTableColumnName)
	{
		this.baseTableName = baseTableName;
		this.baseTableColumnName = baseTableColumnName;
		this.referenceTableName = referenceTableName;
		this.referenceTableColumnName = referenceTableColumnName;
	}

	public static class Constants
	{
		public static final String INNER_JOIN = "INNER JOIN";
		public static final String LEFT_JOIN = "LEFT JOIN";
		public static final String RIGHT_JOIN = "RIGHT JOIN";
		public static final String FULL_JOIN = "FULL JOIN";
	}
}
