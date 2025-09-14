package com.server.stats.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class RequestMeta
{
	private String jsonPayload;
	private Map<String, String> paramsMap = new TreeMap<>();
	private final List<Map<String, String>> requestRowList = new ArrayList<>();
	private String connectionUrl;

	public void addParam(String paramName, String paramValue)
	{
		paramsMap.put(paramName, paramValue);
	}

	public Map<String, String> getParamsMap()
	{
		return paramsMap;
	}

	public void addRequest(Map<String,String> request)
	{
		requestRowList.add(request);
	}

	public List<Map<String,String>> getRequestRowList()
	{
		return requestRowList;
	}

	public JSONObject getJsonPayload()
	{
		return StringUtils.isEmpty(jsonPayload) ? null : new JSONObject(jsonPayload);
	}

	public void setJsonPayload(String jsonBody)
	{
		this.jsonPayload= jsonBody;
	}

	public String getConnectionUrl()
	{
		return connectionUrl;
	}

	public void setConnectionUrl(String connectionUrl)
	{
		this.connectionUrl = connectionUrl;
	}
}
