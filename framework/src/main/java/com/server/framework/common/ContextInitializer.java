package com.server.framework.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

public class ContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
{

	private static final Logger LOGGER = Logger.getLogger(ContextInitializer.class.getName());

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext)
	{
		LOGGER.log(Level.INFO, "Application Context Initializer Called");

		ConfigurableEnvironment environment = applicationContext.getEnvironment();

		try
		{

//			String customPropertiesPath = Paths.get( "application-custom.properties").toString();
//			Properties customProperties = new Properties();

//			MutablePropertySources propertySources = environment.getPropertySources();
//			try(FileInputStream fis = new FileInputStream(customPropertiesPath))
//			{
//				customProperties.load(fis);
//				LOGGER.info("Loaded custom.properties from: " + customPropertiesPath);
//			}

//			Map<String, Object> propertiesMap = new HashMap<>();
//			for(String key : customProperties.stringPropertyNames())
//			{
//				propertiesMap.put(key, customProperties.getProperty(key));
//			}
//
//			MapPropertySource customPropertySource = new MapPropertySource("customProperties", propertiesMap);
//
//			propertySources.addFirst(customPropertySource);
//
//			LOGGER.info("Custom properties loaded successfully. Total properties: " + customProperties.size());

			coldStart(environment);

		}
		catch(IOException e)
		{
			LOGGER.warning("Failed to load custom.properties: " + e.getMessage());
		}
	}

	public static void coldStart(Environment environment) throws IOException
	{
		boolean coldStart = Boolean.parseBoolean(System.getProperty("coldstart", "false"));
		if(!coldStart)
		{
			return;
		}

		String jdbcUrl = environment.getProperty("spring.datasource.url");
		String dbSchema = environment.getProperty("db.server.schema", "tomcatserver");
		String dbUser = environment.getProperty("spring.datasource.username");
		String dbPassword = environment.getProperty("spring.datasource.password");

		try(Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword))
		{
			LOGGER.info("Cold start is set to true");

			connection.createStatement().execute("SET FOREIGN_KEY_CHECKS = 0");

			List<String> tables = new ArrayList<>();
			try(ResultSet rs = connection.createStatement().executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = '" + dbSchema + "'"))
			{
				while(rs.next())
				{
					tables.add(rs.getString("table_name"));
				}
			}

			for(String table : tables)
			{
				connection.createStatement().execute("DROP TABLE IF EXISTS " + table);
			}

			connection.createStatement().execute("SET FOREIGN_KEY_CHECKS = 1");

			LOGGER.info("All tables dropped.");
		}
		catch(SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed during cold start database operations", e);
		}
	}
}
