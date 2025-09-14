package com.server.framework.persistence;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.server.framework.common.CustomConsumer;

public class DataAccessUtil
{
	private static final AtomicLong BATCH_END = new AtomicLong(-1L);
	private static final AtomicLong CURRENT_PK = new AtomicLong(-1L);

	static long getNextPK() throws Exception
	{

		if(!(Objects.equals(CURRENT_PK.get(), -1L) || Objects.equals(CURRENT_PK.get(), BATCH_END.get())))
		{
			return CURRENT_PK.getAndIncrement();
		}

		synchronized(CURRENT_PK)
		{
			SelectQuery selectQuery = new SelectQuery("BatchTable");
			DataObject dataObject = DataAccess.get(selectQuery);
			if(dataObject.isEmpty())
			{
				Row row = new Row("BatchTable");
				row.set("AccountId", 1);
				row.set("BatchStart", 1000000001000L);

				dataObject.addRow(row);

				DataAccess.add(dataObject);

				CURRENT_PK.set(1000000000100L);
				BATCH_END.set(1000000001000L - 1L);
			}
			else
			{
				Long batchStart = (Long) dataObject.getFirstRow("BatchTable").get("BatchStart");
				long updatedBatchEnd = batchStart + 1000L;

				CURRENT_PK.set(batchStart);
				BATCH_END.set(updatedBatchEnd - 1L);

				UpdateQuery updateQuery = new UpdateQuery("BatchTable");
				updateQuery.setValue("BatchStart", updatedBatchEnd);

				DataAccess.update(updateQuery);
			}
		}

		return CURRENT_PK.getAndIncrement();
	}

	static String getInsertQueryString(String table) throws Exception
	{
		StringBuilder insertQuery = new StringBuilder("INSERT INTO " + table);
		insertQuery.append(" ( ");
		StringBuilder valuesPart = new StringBuilder(" (");
		for(String columnName : DBUtil.columnList(table))
		{
			insertQuery.append(columnName);
			insertQuery.append(",");

			valuesPart.append("?,");
		}

		insertQuery.deleteCharAt(insertQuery.length() - 1);
		valuesPart.deleteCharAt(valuesPart.length() - 1);

		insertQuery.append(")");
		insertQuery.append(" VALUES ");
		insertQuery.append(valuesPart);
		insertQuery.append(")");

		return insertQuery.toString();

	}

	static String getCriteriaString(Criteria criteria, List<Object> criteriaPlaceHolderList)
	{
		if(Objects.isNull(criteria.leftCriteria))
		{
			Criteria.Criterion criterion = criteria.criterion;
			if(criterion == null)
			{
				return StringUtils.EMPTY;
			}

			if(criterion.column instanceof Function function)
			{
				StringBuilder functionContent = new StringBuilder();
				for(Object arg : function.args)
				{
					if(arg instanceof Column column)
					{
						functionContent.append(column.tableName).append(".").append(column.columnName);
						functionContent.append(",");
					}
					else
					{
						criteriaPlaceHolderList.add(arg);
						functionContent.append("?");
						functionContent.append(",");
					}
				}
				functionContent.deleteCharAt(functionContent.length() - 1);
				functionContent.append(criterion.comparator).append("?");
				criteriaPlaceHolderList.add(criterion.columnValue);
				return "(" + function.name + "(" + functionContent  + ")" +  ")";
			}

			if(criterion.columnValue instanceof List<?> placeHolderValueList)
			{
				criteriaPlaceHolderList.addAll(placeHolderValueList);
				String placeHolders = String.join(",", "?".repeat(placeHolderValueList.size()).split(StringUtils.EMPTY));
				return "(" + criterion.column.tableName + "." + criterion.column.columnName + StringUtils.SPACE + criterion.comparator + StringUtils.SPACE + "(" + placeHolders + "))";
			}

			criteriaPlaceHolderList.add(criteria.criterion.columnValue);
			return "(" + criterion.column.tableName + "." + criterion.column.columnName + StringUtils.SPACE + criterion.comparator + StringUtils.SPACE + "?" + ")";
		}

		String left = getCriteriaString(criteria.leftCriteria, criteriaPlaceHolderList);

		if(criteria.rightCriteria == null)
		{
			return left;
		}

		left += criteria.operator;

		String right = getCriteriaString(criteria.rightCriteria, criteriaPlaceHolderList);

		return "(" + left + right + ")";
	}

	static String getSelectQueryString(SelectQuery selectQuery, List<String> selectColumnList, List<Object> valuePlaceHolderList)
	{
		StringBuilder selectQueryBuilder = new StringBuilder("SELECT ").append(String.join(",", selectColumnList)).append(" FROM ").append(selectQuery.tableName).append(" ");

		List<Join> joinList = selectQuery.joinList;

		for(Join join : joinList)
		{
			selectQueryBuilder.append(join.joinType).append(" ").append(join.referenceTableName).append(" ON ");
			selectQueryBuilder.append(join.baseTableName).append(".").append(join.baseTableColumnName);
			selectQueryBuilder.append("=").append(join.referenceTableName).append(".").append(join.referenceTableColumnName);
		}

		if(Objects.nonNull(selectQuery.criteria))
		{
			selectQueryBuilder.append(" WHERE ").append(getCriteriaString(selectQuery.criteria, valuePlaceHolderList));
		}

		if(Objects.nonNull(selectQuery.groupByClause))
		{
			GroupByClause groupByClause = selectQuery.groupByClause;
			selectQueryBuilder.append(" GROUP BY ");
			selectQueryBuilder.append(groupByClause.columnList.stream().map(Column::toString).collect(Collectors.joining(",")));
			if(Objects.nonNull(groupByClause.criteria))
			{
				selectQueryBuilder.append(" HAVING ").append(getCriteriaString(groupByClause.criteria, valuePlaceHolderList));
			}
		}

		List<SortColumn> sortColumnList = selectQuery.sortColumnList;
		if(!sortColumnList.isEmpty())
		{
			selectQueryBuilder.append(" ORDER BY ");

			for(SortColumn sortColumn : sortColumnList)
			{
			 selectQueryBuilder.append(sortColumn.column.tableName).append(".").append(sortColumn.column.columnName);
			 selectQueryBuilder.append(StringUtils.SPACE).append(sortColumn.isAscending ? "ASC" : "DESC");
			 selectQueryBuilder.append(",");
			}
			selectQueryBuilder.deleteCharAt(selectQueryBuilder.length() -1 );
		}

		Range range = selectQuery.range;

		if(Objects.nonNull(range))
		{
			selectQueryBuilder.append(" LIMIT ").append(range.startIndex).append(",").append(range.numOfObjects);
		}

		if(selectQuery.needLock)
		{
			selectQueryBuilder.append(" FOR UPDATE");
		}

		return selectQueryBuilder.toString();
	}

	static String getUpdateQueryString(UpdateQuery updateQuery, List<Object> valuePlaceHolderList)
	{
		StringBuilder updateQueryBuilder = new StringBuilder("UPDATE " + updateQuery.tableName + " SET ");

		for(Map.Entry<String, Object> objectEntry : updateQuery.columnNameValueMap.entrySet())
		{
			updateQueryBuilder.append(objectEntry.getKey()).append(" = ").append("?");
			updateQueryBuilder.append(",");
			valuePlaceHolderList.add(objectEntry.getValue());
		}

		updateQueryBuilder.deleteCharAt(updateQueryBuilder.length() - 1);
		if(updateQuery.criteria != null)
		{
			updateQueryBuilder.append(" WHERE ").append(getCriteriaString(updateQuery.criteria, valuePlaceHolderList));
		}

		return updateQueryBuilder.toString();
	}

	static String getDeleteQueryString(String tableName, Criteria criteria, List<Object> valuePlaceHolderList)
	{
		StringBuilder updateQueryBuilder = new StringBuilder("DELETE FROM " + tableName + " ");

		if(criteria != null)
		{
			updateQueryBuilder.append(" WHERE ").append(getCriteriaString(criteria, valuePlaceHolderList));
		}

		return updateQueryBuilder.toString();
	}

	static void populatePK(List<Row> rowList) throws Exception
	{
		Map<UVH, Long> uvhLongMap = new HashMap<>();

		for(Row row : rowList)
		{
			for(Map.Entry<String, Object> objectEntry : row.rowMap.entrySet())
			{
				if(objectEntry.getValue() instanceof UVH)
				{
					Long generatedValue = uvhLongMap.getOrDefault(objectEntry.getValue(), getNextPK());
					uvhLongMap.put((UVH) objectEntry.getValue(), generatedValue);
					String[] tableNameColumnName = objectEntry.getKey().split("\\.");
					row.set(tableNameColumnName[0], tableNameColumnName[1], generatedValue);
				}
			}
		}
	}

	static void handleExceptionForTxn(Connection connection) throws Exception
	{
		if(Objects.isNull(DataAccess.Transaction.getActiveTxnFromTL()))
		{
			if(Objects.nonNull(connection) && !connection.isClosed())
			{
				connection.rollback();
			}
		}
	}

	static void handlePostProcessForTxn(Connection connection) throws Exception
	{
		if(Objects.isNull(DataAccess.Transaction.getActiveTxnFromTL()))
		{
			if(Objects.nonNull(connection) && !connection.isClosed())
			{
				connection.close();
			}
		}
	}

	static void populateSelectColumnAndPlaceHolderListForSelectQuery(SelectQuery selectQuery, List<String> selectColumnList, List<Object> placeHolderList) throws Exception
	{
		if(selectQuery.selectColumnList.isEmpty())
		{
			CustomConsumer<String> generateSelectColumnForTable = tableName->
			{
				List<String> columnList = DBUtil.columnList(tableName);
				for(String columnName : columnList)
				{
					selectColumnList.add(tableName + "." + columnName);
				}
			};

			List<String> tableList = new ArrayList<>();
			tableList.add(selectQuery.tableName);

			generateSelectColumnForTable.accept(selectQuery.tableName);

			for(Join join : selectQuery.joinList)
			{
				String tableName = !tableList.contains(join.baseTableName) ? join.baseTableName : !tableList.contains(join.referenceTableName) ? join.referenceTableName : StringUtils.EMPTY;
				if(StringUtils.isNotEmpty(tableName))
				{
					tableList.add(tableName);
					generateSelectColumnForTable.accept(tableName);
				}
			}
		}

		for(Column column : selectQuery.selectColumnList)
		{
			if(column instanceof Function function)
			{
				StringBuilder selectColumnBuilderForFunction = new StringBuilder(function.name).append("(");

				for(Object arg : function.args)
				{
					if(arg instanceof Column functionColumn)
					{
						selectColumnBuilderForFunction.append(functionColumn);
					}
					else
					{
						selectColumnBuilderForFunction.append("?");
						placeHolderList.add(arg);
					}
				}
				selectColumnBuilderForFunction.append(")").append(" AS \"").append(column).append("\"");
				selectColumnList.add(selectColumnBuilderForFunction.toString());
			}
			else
			{
				selectColumnList.add(column.toString());
			}
		}
	}
}
