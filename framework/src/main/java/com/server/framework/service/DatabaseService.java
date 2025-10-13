package com.server.framework.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

@Service
public class DatabaseService
{

	private static final Logger LOGGER = Logger.getLogger(DatabaseService.class.getName());

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public Connection getConnection(String server, String ip, String db, String user, String password) throws SQLException
	{
		String jdbcUrl = buildJdbcUrl(server, ip, db);
		return getConnection(jdbcUrl, user, password);
	}

	public Connection getConnection(String server, String ip, String port, String db, String user, String password) throws SQLException
	{
		String jdbcUrl = buildJdbcUrl(server, ip, port, db);
		return getConnection(jdbcUrl, user, password);
	}

	public Connection getConnection(String jdbcUrl, String user, String password) throws SQLException
	{

		Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
		connection.setAutoCommit(false);
		return connection;
	}

	public Connection getAppConnection() throws SQLException
	{
		return dataSource.getConnection();
	}

	public String buildJdbcUrl(String server, String ip, String db)
	{
		String port = "3306";

		if("postgresql".equalsIgnoreCase(server) || "postgres".equalsIgnoreCase(server))
		{
			port = "5432";
		}
		else if("mariadb".equalsIgnoreCase(server))
		{
			port = "3306";
		}

		return buildJdbcUrl(server, ip, port, db);
	}

	public  String buildJdbcUrl(String server, String ip, String port, String db)
	{
		if("postgresql".equalsIgnoreCase(server) || "postgres".equalsIgnoreCase(server))
		{
			return String.format("jdbc:postgresql://%s:%s/sasdb?currentSchema=%s&connectTimeout=20", ip, port, db);
		}
		else if("mariadb".equalsIgnoreCase(server))
		{
			return String.format("jdbc:mariadb://%s:%s/%s?useSSL=false&serverTimezone=UTC", ip, port, db);
		}
		else
		{
			// Default to MySQL
			return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", ip, port, db);
		}
	}

	public JdbcTemplate getJdbcTemplate()
	{
		return jdbcTemplate;
	}

	public boolean isConnectionValid(String server, String ip, String db, String user, String password)
	{
		try(Connection connection = getConnection(server, ip, db, user, password))
		{
			return connection.isValid(5);
		}
		catch(SQLException e)
		{
			return false;
		}
	}

	public boolean testConnection()
	{
		try(Connection connection = dataSource.getConnection())
		{
			return connection.isValid(5);
		}
		catch(SQLException e)
		{
			return false;
		}
	}
}
