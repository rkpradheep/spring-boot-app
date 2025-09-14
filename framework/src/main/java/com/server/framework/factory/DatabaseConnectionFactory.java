package com.server.framework.factory;

import com.server.framework.common.AppProperties;
import com.server.framework.service.DatabaseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class DatabaseConnectionFactory
{

	private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionFactory.class.getName());

	@Autowired
	private DatabaseService databaseService;

	public Connection createConnection(String server, String ip, String db, String user, String password) throws SQLException
	{
		LOGGER.log(Level.INFO, "Creating database connection for server: {0}, ip: {1}, db: {2}",
			new Object[] {server, ip, db});

		try
		{
			return databaseService.getConnection(server, ip, db, user, password);
		}
		catch(SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to create database connection", e);
			throw e;
		}
	}

	public Connection createDefaultConnection() throws SQLException
	{
		LOGGER.log(Level.INFO, "Creating default database connection");
		return databaseService.getConnection(AppProperties.getProperty("spring.datasource.url"), AppProperties.getProperty("spring.datasource.username"), AppProperties.getProperty("spring.datasource.password"));
	}

	public Connection createValidatedConnection(DatabaseType dbType, ConnectionParams connectionParams) throws SQLException
	{
		Connection connection = createConnection(
			dbType.getServerType(),
			connectionParams.getIp(),
			connectionParams.getDatabase(),
			connectionParams.getUsername(),
			connectionParams.getPassword()
		);


		return connection;
	}

	public enum DatabaseType
	{
		MYSQL("mysql"),
		POSTGRESQL("postgresql"),
		MARIADB("mariadb"),
		H2("h2");

		private final String serverType;

		DatabaseType(String serverType)
		{
			this.serverType = serverType;
		}

		public String getServerType()
		{
			return serverType;
		}
	}

	public static class ConnectionParams
	{
		private final String ip;
		private final String database;
		private final String username;
		private final String password;

		public ConnectionParams(String ip, String database, String username, String password)
		{
			this.ip = ip;
			this.database = database;
			this.username = username;
			this.password = password;
		}

		public String getIp()
		{
			return ip;
		}

		public String getDatabase()
		{
			return database;
		}

		public String getUsername()
		{
			return username;
		}

		public String getPassword()
		{
			return password;
		}
	}
}
