package com.server.framework.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class HttpService
{
	private static HttpService instance;

	@PostConstruct
	public void init() {
		instance = this;
	}

	public Map<String, String> convertRawHeadersToMap(String rawHeaders)
	{
		String[] lines = rawHeaders.split("\n");

		return Arrays.stream(lines)
			.filter(line -> line.contains(":") && !line.toLowerCase().contains("content-type") && !line.toLowerCase().contains("content-length"))
			.collect(Collectors.toMap(header -> header.split(":")[0], header -> header.split(":")[1]));
	}

	public void copyInputStreamToOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException
	{
		byte[] bytes = new byte[1024];
		int len = -1;

		while((len = inputStream.read(bytes)) != -1)
		{
			outputStream.write(bytes, 0, len);
		}
		outputStream.flush();
	}

	public String getEncodedQueryString(Map<String, ?> dataMap)
	{
		List<NameValuePair> nameValuePairList = new ArrayList<>();
		for(String key : dataMap.keySet())
		{
			nameValuePairList.add(new BasicNameValuePair(key, ObjectUtils.defaultIfNull(dataMap.get(key), StringUtils.EMPTY).toString()));
		}

		return URLEncodedUtils.format(nameValuePairList, StandardCharsets.UTF_8);
	}

	public HttpResponse makeNetworkCall(String url, String method, Map<String, String> headersMap, JSONObject jsonObject) throws IOException
	{
		return makeNetworkCall(new HttpContext(url, method).setHeadersMap(headersMap).setBody(jsonObject));
	}

	public HttpResponse makeNetworkCall(HttpContext httpContext) throws IOException
	{
		String url = httpContext.getUrl();
		String method = httpContext.getMethod();


		String queryString = getEncodedQueryString(httpContext.getParametersMap());

		Map<String, Object> headersMap = httpContext.getHeadersMap();
		InputStream inputStream = httpContext.getInputStream();
		Proxy proxy = httpContext.getProxy();
		SSLSocketFactory sslSocketFactory = httpContext.getSslSocketFactory();

		if(StringUtils.isNotEmpty(queryString))
		{
			url = !StringUtils.contains(url, "?") ? url.concat("?").concat(queryString) : StringUtils.endsWith(url, "&") ? url.concat(queryString) : url.concat("&").concat(queryString);
		}

		HttpURLConnection httpURLConnection = (HttpURLConnection) (Objects.nonNull(proxy) ? new URL(url).openConnection(proxy) : new URL(url).openConnection());
		httpURLConnection.setRequestMethod(method);
		httpURLConnection.setConnectTimeout(httpContext.getConnectionTimeoutInMillis());
		httpURLConnection.setReadTimeout(httpContext.getReadTimeoutInMillis());
		if(Objects.nonNull(sslSocketFactory))
		{
			((HttpsURLConnection) httpURLConnection).setSSLSocketFactory(sslSocketFactory);
		}

		if(Objects.nonNull(headersMap))
		{
			for(Map.Entry<String, Object> headers : headersMap.entrySet())
			{
				String headerValue = Objects.nonNull(headers.getValue())  ? headers.getValue().toString() : null;
				httpURLConnection.setRequestProperty(headers.getKey(),  headerValue);
			}
		}

		httpURLConnection.setDoInput(true);

		if(Objects.nonNull(inputStream) && inputStream.available() > 0 && !method.equals(HttpGet.METHOD_NAME))
		{
			httpURLConnection.setDoOutput(true);
			copyInputStreamToOutputStream(inputStream, httpURLConnection.getOutputStream());
		}

		Map<String, String> responseHeadersMap = new TreeMap<>();
		Set<String> keySet = httpURLConnection.getHeaderFields().keySet();
		for(String headerName : keySet)
		{
			if(Objects.nonNull(headerName))
			{
				responseHeadersMap.put(headerName, httpURLConnection.getHeaderField(headerName));
			}
		}

		HttpResponse httpResponse = new HttpResponse();
		httpResponse.setResponseHeaders(responseHeadersMap);
		httpResponse.setContentType(httpURLConnection.getContentType());
		httpResponse.setStatus(httpURLConnection.getResponseCode());
		httpResponse.setInputStream(getStream(httpURLConnection));

		return httpResponse;
	}

	private InputStream getStream(HttpURLConnection httpURLConnection)
	{
		try
		{
			return httpURLConnection.getInputStream();
		}
		catch(IOException e)
		{
			return httpURLConnection.getErrorStream();
		}
	}

	public boolean isValidURL(String url)
	{
		try
		{
			URL urlObject = new URL(url);
			String host = urlObject.getHost();
			int port = urlObject.getPort() != -1 ? urlObject.getPort() : urlObject.getDefaultPort();
			try(Socket socket = new Socket())
			{
				SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
				socket.connect(socketAddress);
			}

			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}

	public static Map<String, String> convertRawHeadersToMapStatic(String rawHeaders) {
		if (instance == null) {
			throw new IllegalStateException("HttpService instance not initialized");
		}
		return instance.convertRawHeadersToMap(rawHeaders);
	}

	public static void copyInputStreamToOutputStreamStatic(InputStream inputStream, OutputStream outputStream) throws IOException {
		if (instance == null) {
			throw new IllegalStateException("HttpService instance not initialized");
		}
		instance.copyInputStreamToOutputStream(inputStream, outputStream);
	}

	public static String getEncodedQueryStringStatic(Map<String, ?> dataMap) {
		if (instance == null) {
			throw new IllegalStateException("HttpService instance not initialized");
		}
		return instance.getEncodedQueryString(dataMap);
	}

	public static HttpResponse makeNetworkCallStatic(String url, String method, Map<String, String> headersMap, JSONObject jsonObject) throws IOException {
		if (instance == null) {
			throw new IllegalStateException("HttpService instance not initialized");
		}
		return instance.makeNetworkCall(url, method, headersMap, jsonObject);
	}

	public static HttpResponse makeNetworkCallStatic(HttpContext httpContext) throws IOException {
		if (instance == null) {
			throw new IllegalStateException("HttpService instance not initialized");
		}
		return instance.makeNetworkCall(httpContext);
	}

	public static boolean isValidURLStatic(String url) {
		if (instance == null) {
			throw new IllegalStateException("HttpService instance not initialized");
		}
		return instance.isValidURL(url);
	}
}
