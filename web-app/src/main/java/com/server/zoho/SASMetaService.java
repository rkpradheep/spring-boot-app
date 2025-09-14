package com.server.zoho;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

import com.server.framework.common.AppProperties;

@Service
public class SASMetaService
{
	private static final Logger LOGGER = Logger.getLogger(SASMetaService.class.getName());

	public Map<String, Object> getSasMeta()
	{

		Map<String, Object> map = new HashMap<>();

		Map<String, Object> dbMeta = new LinkedHashMap<>();
		for(String service : AppProperties.getProperty("db.services").split(","))
		{
			Map<String, String> serviceInfo = new HashMap<>();
			serviceInfo.put("server", AppProperties.getProperty("db." + service + ".server"));
			serviceInfo.put("ip", AppProperties.getProperty("db." + service + ".ip"));
			serviceInfo.put("user", AppProperties.getProperty("db." + service + ".user"));
			serviceInfo.put("password", AppProperties.getProperty("db." + service + ".password"));
			dbMeta.put(service, serviceInfo);
		}
		map.put("db_meta", dbMeta);

		Map<String, String> securityMeta = new HashMap<>();
		for(String service : AppProperties.getProperty("security.services").split(","))
		{
			securityMeta.put(service, AppProperties.getProperty("zoho." + service + ".display.name"));
		}
		map.put("security_meta", securityMeta);

		Map<String, String> earMeta = new HashMap<>();
		for(String service : AppProperties.getProperty("ear.services").split(","))
		{
			earMeta.put(service, AppProperties.getProperty("zoho." + service + ".display.name"));
		}
		map.put("ear_meta", earMeta);

		Map<String, Object> taskEngineMeta = new HashMap<>();

		Map<String, String> products = new LinkedHashMap<>();
		for(String service : AppProperties.getProperty("taskengine.services").split(","))
		{
			products.put(service, AppProperties.getProperty("zoho." + service + ".display.name"));
		}
		taskEngineMeta.put("products", products);

		Map<String, List<String>> threadPoolMeta = new HashMap<>();
		for(String service : AppProperties.getProperty("taskengine.services").split(","))
		{
			String serviceType = service.split("-")[0];
			String threadPools = AppProperties.getProperty("taskengine." + serviceType + ".threadpools");
			if(threadPools != null && !threadPools.isEmpty())
			{
				threadPoolMeta.put(serviceType, Arrays.asList(threadPools.split(",")));
			}
		}
		taskEngineMeta.put("thread_pools", threadPoolMeta);

		map.put("taskengine_meta", taskEngineMeta);

		Map<String, String> zohoMeta = new HashMap<>();
		for(String service : AppProperties.getProperty("db.services").split(","))
		{
			zohoMeta.put(service, AppProperties.getProperty("zoho." + service + ".display.name"));
		}
		map.put("zoho_meta", zohoMeta);

		Map<String, Object> config = new HashMap<>();
		config.put("db_services", Arrays.asList(AppProperties.getProperty("db.services").split(",")));
		config.put("security_services", Arrays.asList(AppProperties.getProperty("security.services").split(",")));
		config.put("ear_services", Arrays.asList(AppProperties.getProperty("ear.services").split(",")));
		config.put("taskengine_services", Arrays.asList(AppProperties.getProperty("taskengine.services").split(",")));
		config.put("production", false);
		config.put("skip_authentication", false);
		config.put("need_http_logs", true);
		map.put("config", config);

		return map;
	}
}
