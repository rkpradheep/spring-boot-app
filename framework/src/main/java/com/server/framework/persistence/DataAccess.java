package com.server.framework.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.server.framework.common.DateUtil;
import com.server.framework.job.CustomRunnable;

public class DataAccess
{
	public static class Transaction
	{
		private static final ThreadLocal<Connection> TRANSACTION_TL = new ThreadLocal<>();

		static Connection getActiveTxnFromTL() throws Exception
		{
			Connection connection = TRANSACTION_TL.get();
			if(Objects.nonNull(connection))
			{
				if(!connection.isClosed())
				{
					return connection;
				}

				TRANSACTION_TL.remove();
			}
			return null;
		}

		public static void begin() throws Exception
		{
			Connection connection = TRANSACTION_TL.get();
			if(Objects.nonNull(connection))
			{
				if(!connection.isClosed())
				{
					connection.rollback();
					connection.close();
				}

				TRANSACTION_TL.remove();
			}

			TRANSACTION_TL.set(DBUtil.getServerDBConnectionForTxn());
		}

		public static void commit() throws Exception
		{
			Connection connection = TRANSACTION_TL.get();
			if(Objects.isNull(connection))
			{
				throw new Exception("No active transaction found to commit");
			}

			connection.commit();
			connection.close();
			TRANSACTION_TL.remove();
		}

		public static void rollback()
		{
			try
			{
				Connection connection = TRANSACTION_TL.get();
				if(Objects.isNull(connection))
				{
					return;
				}

				connection.rollback();
				connection.close();
				TRANSACTION_TL.remove();
			}
			catch(Exception e)
			{

			}
		}
	}

	public static <T> T executeInTxn(Supplier<T> supplier) throws Exception
	{
		try
		{
			Transaction.begin();
			T returnValue = supplier.get();
			Transaction.commit();
			return returnValue;
		}
		catch(Exception e)
		{
			Transaction.rollback();
			throw e;
		}
	}

	public static void executeInTxn(CustomRunnable runnable) throws Exception
	{
		try
		{
			Transaction.begin();
			runnable.run();
			Transaction.commit();
		}
		catch(Exception e)
		{
			Transaction.rollback();
			throw e;
		}
	}

	public static void add(Row row) throws Exception
	{
		DataObject dataObject = new DataObject();
		dataObject.addRow(row);

		add(dataObject);
	}

	public static void add(DataObject dataObject) throws Exception
	{
		List<Row> rowList = dataObject.getRows();
		DataAccessUtil.populatePK(rowList);

		Connection connection = null;

		try
		{
			connection = DBUtil.getServerDBConnectionForTxn();

			for(String table : dataObject.getTables())
			{

				PreparedStatement preparedStatement = connection.prepareStatement(DataAccessUtil.getInsertQueryString(table), PreparedStatement.RETURN_GENERATED_KEYS);

				for(Row row : dataObject.getRows(table))
				{
					if(DBUtil.columnList(row.tableName).contains("CreatedTime"))
					{
						row.set("CreatedTime", DateUtil.getCurrentTimeInMillis());
					}
					int i = 1;
					for(String columnName : DBUtil.columnList(table))
					{
						preparedStatement.setObject(i++, row.get(columnName));
					}

					preparedStatement.addBatch();
				}

				preparedStatement.executeBatch();
			}

			if(Objects.isNull(Transaction.getActiveTxnFromTL()))
			{
				connection.commit();
			}
		}
		catch(Exception e)
		{
			DataAccessUtil.handleExceptionForTxn(connection);
			throw e;
		}
		finally
		{
			DataAccessUtil.handlePostProcessForTxn(connection);
		}
	}

	public static DataObject get(SelectQuery selectQuery) throws Exception
	{

		DataObject dataObject = new DataObject();

		Connection connection = null;

		try
		{
			connection = DBUtil.getServerDBConnectionForTxn();

			List<String> selectColumnList = new ArrayList<>();
			List<Object> placeHolderList = new ArrayList<>();

			DataAccessUtil.populateSelectColumnAndPlaceHolderListForSelectQuery(selectQuery, selectColumnList, placeHolderList);

			PreparedStatement preparedStatement = connection.prepareStatement(DataAccessUtil.getSelectQueryString(selectQuery, selectColumnList, placeHolderList));

			int i = 1;
			for(Object columnValue : placeHolderList)
			{
				preparedStatement.setObject(i++, columnValue);
			}

			ResultSet resultSet = preparedStatement.executeQuery();

			boolean isWithoutJoin = selectQuery.joinList.isEmpty();

			while(resultSet.next())
			{
				Row row = isWithoutJoin ? new Row(selectQuery.tableName) : new RowWrapper(selectQuery.tableName);

				for(String selectColumn : selectColumnList)
				{
					Pattern pattern = Pattern.compile(".* AS \"(.*)\"");
					Matcher matcher = pattern.matcher(selectColumn);

					selectColumn = matcher.matches() ? matcher.group(1) : selectColumn;
					String columnName = isWithoutJoin ? selectColumn.split("\\.")[1] : selectColumn;
					row.set(columnName, resultSet.getObject(selectColumn));
				}
				dataObject.addRow(row);
			}
			return dataObject;
		}
		finally
		{
			DataAccessUtil.handlePostProcessForTxn(connection);
		}

	}

	public static int update(UpdateQuery updateQuery) throws Exception
	{
		Connection connection = null;
		try
		{
			connection = DBUtil.getServerDBConnectionForTxn();

			List<Object> valuePlaceHolderList = new ArrayList<>();
			PreparedStatement preparedStatement = connection.prepareStatement(DataAccessUtil.getUpdateQueryString(updateQuery, valuePlaceHolderList));

			int i = 1;
			for(Object columnValue : valuePlaceHolderList)
			{
				preparedStatement.setObject(i++, columnValue);
			}

			int updatedRows = preparedStatement.executeUpdate();

			if(Objects.isNull(Transaction.getActiveTxnFromTL()))
			{
				connection.commit();
			}

			return updatedRows;
		}
		catch(Exception e)
		{
			DataAccessUtil.handleExceptionForTxn(connection);
			throw e;
		}
		finally
		{
			DataAccessUtil.handlePostProcessForTxn(connection);
		}
	}

	public static void delete(String tableName, Criteria criteria) throws Exception
	{
		Connection connection = null;
		try
		{
			connection = DBUtil.getServerDBConnectionForTxn();

			List<Object> valuePlaceHolderList = new ArrayList<>();
			PreparedStatement preparedStatement = connection.prepareStatement(DataAccessUtil.getDeleteQueryString(tableName, criteria, valuePlaceHolderList));

			int i = 1;
			for(Object columnValue : valuePlaceHolderList)
			{
				preparedStatement.setObject(i++, columnValue);
			}

			preparedStatement.execute();

			if(Objects.isNull(Transaction.getActiveTxnFromTL()))
			{
				connection.commit();
			}
		}
		catch(Exception e)
		{
			DataAccessUtil.handleExceptionForTxn(connection);
			throw e;
		}
		finally
		{
			DataAccessUtil.handlePostProcessForTxn(connection);
		}
	}
}
