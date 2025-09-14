package com.server.framework.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class HttpResponse
{
	String contentType;
	int status;
	Map<String, String> responseHeaders;
	InputStream inputStream;
	String responseString;

	public String getContentType()
	{
		return contentType;
	}

	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public int getStatus()
	{
		return status;
	}

	public void setStatus(int status)
	{
		this.status = status;
	}

	public Map<String, String> getResponseHeaders()
	{
		return responseHeaders;
	}

	public void setResponseHeaders(Map<String, String> responseHeaders)
	{
		this.responseHeaders = responseHeaders;
	}

	public InputStream getInputStream() throws IOException
	{
		return StringUtils.equals(responseHeaders.get("Content-Encoding"), "gzip") ? new GZIPInputStream(inputStream) : inputStream;
	}

	public String getStringResponse() throws IOException
	{
		if(StringUtils.isNotEmpty(responseString))
		{
			return responseString;
		}
		InputStream inputStream = getInputStream();
		if(Objects.isNull(inputStream))
		{
			return StringUtils.EMPTY;
		}
		StringWriter stringWriter = new StringWriter();
		IOUtils.copy(inputStream, stringWriter, StandardCharsets.UTF_8);
		responseString = stringWriter.toString();

		return responseString;
	}

	public JSONObject getJSONResponse() throws IOException
	{
		return new JSONObject(getStringResponse());
	}

	public void setInputStream(InputStream inputStream)
	{
		this.inputStream = inputStream;
	}
}
