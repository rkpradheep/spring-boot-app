package com.server.framework.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.net.ssl.SSLSocketFactory;

import org.json.JSONObject;

public class HttpContext
{
	private String url;
	private String method;
	private Map<String, Object> parametersMap;
	private Map<String, Object> headersMap;
	private SSLSocketFactory sslSocketFactory;
	private InputStream inputStream;
	private Proxy proxy;

	public HttpContext(String url, String method)
	{
		this.url = url;
		this.method = method;
		this.parametersMap = new TreeMap<>();
		this.headersMap = new TreeMap<>();
	}

	public HttpContext setParametersMap(Map<String, ?> parametersMap)
	{
		this.parametersMap.putAll(parametersMap);
		return this;
	}

	public HttpContext setParam(String name, Object value)
	{
		this.parametersMap.put(name, value);
		return this;
	}

	public HttpContext setHeadersMap(Map<String, ?> headersMap)
	{
		this.headersMap.putAll(headersMap);
		return this;
	}

	public HttpContext setHeader(String name, Object value)
	{
		this.headersMap.put(name, value);
		return this;
	}

	public HttpContext setBody(JSONObject jsonObject)
	{
		if(Objects.isNull(jsonObject))
		{
			return this;
		}
		this.inputStream = new ByteArrayInputStream(jsonObject.toString().getBytes());
		setHeader("Content-Type", "application/json");
		return this;
	}

	public HttpContext setBody(InputStream inputStream)
	{
		this.inputStream = inputStream;
		return this;
	}

	public HttpContext setProxy(Proxy proxy)
	{
		this.proxy = proxy;
		return this;
	}

	public HttpContext setSslSocketFactory(SSLSocketFactory sslSocketFactory)
	{
		this.sslSocketFactory = new SSLSocketFactoryWrapper(sslSocketFactory);
		return this;
	}

	public HttpContext setSslSocketFactory(SSLSocketFactory sslSocketFactory, String enforcedSSLProtocol)
	{
		this.sslSocketFactory = new SSLSocketFactoryWrapper(sslSocketFactory, enforcedSSLProtocol);
		return this;
	}

	public String getUrl()
	{
		return url;
	}

	public String getMethod()
	{
		return method;
	}

	public Map<String, Object> getParametersMap()
	{
		return parametersMap;
	}

	public Map<String, Object> getHeadersMap()
	{
		return headersMap;
	}

	public SSLSocketFactory getSslSocketFactory()
	{
		return sslSocketFactory;
	}

	public InputStream getInputStream()
	{
		return inputStream;
	}

	public Proxy getProxy()
	{
		return proxy;
	}
}
