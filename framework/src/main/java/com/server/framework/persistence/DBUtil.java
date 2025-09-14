package com.server.framework.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.server.framework.common.AppProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DBUtil
{
	private static final HikariDataSource dataSource;
	public static final String schemaName = AppProperties.getProperty("db.server.schema");


	static
	{
		HikariConfig config = new HikariConfig();
		String jdbcUrl = AppProperties.getProperty("spring.datasource.url");
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(AppProperties.getProperty("spring.datasource.username"));
		config.setPassword(AppProperties.getProperty("spring.datasource.password"));
		config.setMaximumPoolSize(1);
		config.setMinimumIdle(1);
		config.setIdleTimeout(30000);
		config.setConnectionTimeout(30000);
		config.setPoolName("MyHikariCP");
		config.setAutoCommit(false);

		dataSource = new HikariDataSource(config);
	}

	private static final Map<String, List<String>> tablePkList = new HashMap<>();
	private static final Map<String, List<String>> tableFkList = new HashMap<>();

	public static List<String> columnList(String tableName) throws Exception
	{
		List<String> columnList = new ArrayList<>();
		try(Connection connection = DBUtil.getServerDBConnection())
		{
			DatabaseMetaData databaseMetaData = connection.getMetaData();

			ResultSet columnResultSet = databaseMetaData.getColumns(null, DBUtil.schemaName, tableName, null);
			while(columnResultSet.next())
			{
				String columnName = columnResultSet.getString("COLUMN_NAME");
				columnList.add(columnName);
			}
		}
		return columnList;
	}

	public static List<String> getPKList(String tableName) throws Exception
	{
		if(tablePkList.containsKey(tableName))
		{
			return tablePkList.get(tableName);
		}
		List<String> pkList = new ArrayList<>();
		try(Connection connection = DBUtil.getServerDBConnection())
		{
			ResultSet resultSet = connection.getMetaData().getPrimaryKeys(null, schemaName, tableName);
			while(resultSet.next())
			{
				pkList.add(resultSet.getString("COLUMN_NAME"));
			}
			tablePkList.put(tableName, pkList);
		}
		return pkList;
	}

	public static List<String> getFKList(String tableName)
	{
		if(tableFkList.containsKey(tableName))
		{
			return tableFkList.get(tableName);
		}

		List<String> fkList = new ArrayList<>();
		try(Connection connection = DBUtil.getServerDBConnection())
		{
			ResultSet resultSet = connection.getMetaData().getImportedKeys(null, schemaName, tableName);

			while(resultSet.next())
			{
				fkList.add(resultSet.getString("FKCOLUMN_NAME"));
			}
			tableFkList.put(tableName, fkList);
		}
		catch(Exception e)
		{
		}
		return fkList;
	}

	public static Connection getServerDBConnection() throws Exception
	{
		Connection connection =  dataSource.getConnection();
		connection.setAutoCommit(true); //HirakiCP wil automatically reset it to false based on config after connection is returned to pool
		return connection;
	}

	public static Connection getServerDBConnectionForTxn() throws Exception
	{
		return Objects.isNull(DataAccess.Transaction.getActiveTxnFromTL()) ? dataSource.getConnection() : DataAccess.Transaction.getActiveTxnFromTL();
	}

}
