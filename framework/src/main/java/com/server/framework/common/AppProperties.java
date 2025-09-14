package com.server.framework.common;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Component
public class AppProperties
{
	private static final Logger LOGGER = Logger.getLogger(AppProperties.class.getName());

	@Autowired
	private ConfigurableEnvironment env;

	private static ConfigurableEnvironment environment;

	private static final ConcurrentHashMap<String, String> updatedProperties = new ConcurrentHashMap<>();

	@PostConstruct
	public void init()
	{
		environment = this.env;
		LOGGER.info("AppProperties initialized");
	}

	public static List<String> getPropertyList()
	{
		return new ArrayList<>(getCustomPropertySource().getSource().keySet());
	}

	public static String getProperty(String key)
	{
		return _getProperty(key);
	}

	public static boolean getBoolean(String key)
	{
		return getProperty(key, Boolean.class, false);
	}

	public static String getProperty(String key, String defaultValue)
	{
		return getProperty(key, String.class, defaultValue);
	}

	public static void updateProperty(String key, String value)
	{
		updatedProperties.put(key, value);
	}

	public static boolean getBooleanProperty(String key, boolean defaultValue)
	{
		return getProperty(key, Boolean.class, defaultValue);
	}

	public static Integer getIntProperty(String key, Integer defaultValue)
	{
		return getProperty(key, Integer.class, defaultValue);
	}

	public static Long getLongProperty(String key, Long defaultValue)
	{
		return getProperty(key, Long.class, defaultValue);
	}

	private static MapPropertySource getCustomPropertySource()
	{
		MutablePropertySources propertySources = environment.getPropertySources();
		return (MapPropertySource) propertySources.stream().filter(propertySource -> propertySource.getName().contains("application-custom.properties")).findFirst().get();
	}

	private static <T> T getProperty(String key, Class<T> targetType, T defaultValue)
	{
		String value = _getProperty(key);
		return value != null ? targetType.cast(convert(value, targetType)) : defaultValue;
	}

	private static String _getProperty(String key)
	{
		if(updatedProperties.containsKey(key))
		{
			return updatedProperties.get(key);
		}
		return environment.getProperty(key);
	}

	private static Object convert(String value, Class<?> targetType)
	{
		if(value == null)
		{
			return null;
		}
		if(targetType == String.class)
		{
			return value;
		}
		else if(targetType == Integer.class)
		{
			return Integer.parseInt(value);
		}
		else if(targetType == Long.class)
		{
			return Long.parseLong(value);
		}
		else if(targetType == Boolean.class)
		{
			return Boolean.parseBoolean(value);
		}
		else if(targetType == Double.class)
		{
			return Double.parseDouble(value);
		}
		throw new IllegalArgumentException("Unsupported target type: " + targetType);
	}
}
