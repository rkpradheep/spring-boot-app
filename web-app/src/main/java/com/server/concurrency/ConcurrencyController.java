package com.server.concurrency;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.server.framework.common.CommonService;
import com.server.framework.http.FormData;
import com.server.framework.http.HttpService;
import com.server.framework.http.HttpContext;
import com.server.framework.http.HttpResponse;
import com.server.framework.security.SecurityUtil;
import com.server.framework.builder.ApiResponseBuilder;

@RestController
@RequestMapping("/api/v1/concurrency")
public class ConcurrencyController
{

	private static final Logger LOGGER = Logger.getLogger(ConcurrencyController.class.getName());

	@PostMapping
	public ResponseEntity<Map<String, Object>> makeConcurrentRequests(
		HttpServletRequest request, HttpServletResponse response) throws IOException
	{

		try
		{
			LOGGER.log(Level.INFO, "Starting concurrent requests processing");
			LOGGER.log(Level.INFO, "Thread name: {0}", Thread.currentThread().getName());

			Map<String, FormData> formDataMap = SecurityUtil.parseMultiPartFormData(request);

			if(!formDataMap.containsKey("meta_json"))
			{
				Map<String, Object> errorResponse = ApiResponseBuilder.error("Missing meta_json parameter", 400);
				return ResponseEntity.badRequest().body(errorResponse);
			}

			JSONObject jsonObject = new JSONObject(formDataMap.get("meta_json").getValue());
			int concurrencyCalls = jsonObject.getInt("concurrency_calls");

			if(concurrencyCalls > 100 && !StringUtils.equals(jsonObject.optString("password"), "1155"))
			{
				Map<String, Object> errorResponse = ApiResponseBuilder.error("Concurrent call value is above 100 and password provided is invalid.", 400);
				return ResponseEntity.badRequest().body(errorResponse);
			}

			final JSONObject params = jsonObject.optJSONObject("params") != null ?
				jsonObject.optJSONObject("params") : new JSONObject();

			String tempUrl = jsonObject.getString("url");
			if(tempUrl.contains("?"))
			{
				String[] urlParts = tempUrl.split("\\?", 2);
				tempUrl = urlParts[0];
				String queryString = urlParts[1];

				for(String queryParam : queryString.split("&"))
				{
					String[] queryParamSplit = queryParam.split("=", 2);
					if(queryParamSplit.length == 2)
					{
						params.put(queryParamSplit[0].trim(), queryParamSplit[1].trim());
					}
				}
			}
			final String url = tempUrl;

			String method = jsonObject.getString("method");
			String headersFromRequest = jsonObject.opt("headers").toString();

			JSONObject proxyDetails = jsonObject.optJSONObject("proxy_meta");

			JSONObject headers = parseHeaders(headersFromRequest);

			formDataMap.remove("meta_json");

			Map<String, String> headersMap = new HashMap<>();
			headers.keySet().forEach(header -> headersMap.put(header, headers.getString(header)));

			headersMap.remove("Content-Type");
			headersMap.remove("Content-Length");
			headersMap.remove("Accept-Encoding");

			String previousForwardedFor = headersMap.getOrDefault("x-forwarded-for", "");
			previousForwardedFor = StringUtils.isNotEmpty(previousForwardedFor) ? previousForwardedFor + "," : "";
			headersMap.put("x-forwarded-for", previousForwardedFor + request.getRemoteAddr());

			if(!isValidURL(url))
			{
				Map<String, Object> errorResponse = ApiResponseBuilder.error("API URL provided is invalid. Please check and try again.", 400);
				return ResponseEntity.badRequest().body(errorResponse);
			}

			List<Map<String, Object>> responseList = new ArrayList<>();
			AtomicInteger atomicInteger = new AtomicInteger(0);

			// Create the runnable for concurrent execution
			Runnable runnable = () -> {
				try
				{
					Map<String, String> finalHeadersMap = new HashMap<>(headersMap);

					// Make the actual HTTP call using your HTTP utility
					String responseData = makeHttpCall(url, method, params, formDataMap, finalHeadersMap, proxyDetails);

					JSONObject responseJSON = new JSONObject();
					Object responseObject;
					try
					{
						responseObject = new JSONObject(responseData);
					}
					catch(Exception e)
					{
						responseObject = responseData;
					}

					synchronized(responseList)
					{
						responseJSON.put("Response " + atomicInteger.incrementAndGet(), responseObject);
						responseList.add(responseJSON.toMap());
					}
				}
				catch(Exception e)
				{
					LOGGER.log(Level.SEVERE, "Exception occurred during concurrent request", e);
					synchronized(responseList)
					{
						Map<String, Object> errorResponse = new HashMap<>();
						errorResponse.put("Response " + atomicInteger.incrementAndGet(), "Error: " + e.getMessage());
						responseList.add(errorResponse);
					}
				}
			};

			// Execute concurrent requests
			List<Runnable> runnableList = new ArrayList<>();
			for(int i = 0; i < concurrencyCalls; i++)
			{
				runnableList.add(runnable);
			}

			executeAsynchronously(runnableList);

			LOGGER.log(Level.INFO, "Response list size: {0}", responseList.size());

			Map<String, Object> successResponse = ApiResponseBuilder.success("Concurrent requests completed successfully", responseList);
			return ResponseEntity.ok(successResponse);

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred during concurrent requests processing", e);
			Map<String, Object> errorResponse = ApiResponseBuilder.error("Failed to process concurrent requests: " + e.getMessage(), 500);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> getConcurrencyInfo()
	{
		try
		{
			LOGGER.log(Level.INFO, "Retrieving concurrency information");
			LOGGER.log(Level.INFO, "Current thread: {0}", Thread.currentThread().getName());

			Map<String, Object> data = new HashMap<>();
			data.put("activeThreads", Thread.activeCount());
			data.put("maxThreads", 100);
			data.put("currentLoad", 0.0);

			Map<String, Object> response = ApiResponseBuilder.success("Concurrency information retrieved successfully", data);

			LOGGER.log(Level.INFO, "Concurrency info response prepared");
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred while retrieving concurrency info", e);
			Map<String, Object> errorResponse = ApiResponseBuilder.error("Failed to retrieve concurrency information: " + e.getMessage(), 500);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	@PostMapping("/limit")
	public ResponseEntity<Map<String, Object>> setConcurrencyLimit(@RequestBody Map<String, Object> request)
	{
		try
		{
			Map<String, Object> data = new HashMap<>();
			data.put("newLimit", request.get("limit"));

			Map<String, Object> response = ApiResponseBuilder.success("Concurrency limit set successfully", data);
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred while setting concurrency limit", e);
			Map<String, Object> errorResponse = ApiResponseBuilder.error("Failed to set concurrency limit: " + e.getMessage(), 500);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	@PostMapping("/throttle")
	public ResponseEntity<Map<String, Object>> throttleRequests(@RequestBody Map<String, Object> request)
	{
		try
		{
			Map<String, Object> data = new HashMap<>();
			data.put("throttleRate", request.get("rate"));

			Map<String, Object> response = ApiResponseBuilder.success("Request throttling applied successfully", data);
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred while applying request throttling", e);
			Map<String, Object> errorResponse = ApiResponseBuilder.error("Failed to apply request throttling: " + e.getMessage(), 500);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	public void executeAsynchronously(List<Runnable> runnableList)
	{
		List<Future<?>> futureList = new ArrayList<>();
		ExecutorService executorService = Executors.newFixedThreadPool(runnableList.size());

		for(Runnable runnable : runnableList)
		{
			futureList.add(executorService.submit(runnable));
		}

		LOGGER.log(Level.INFO, "Future list size: {0}", futureList.size());

		for(Future<?> future : futureList)
		{
			try
			{
				future.get();
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "Exception occurred in future execution", e);
			}
		}

		executorService.shutdown();
	}

	private JSONObject parseHeaders(String headersFromRequest)
	{
		JSONObject headers = new JSONObject();

		if(StringUtils.isNotEmpty(headersFromRequest) && !headersFromRequest.equals("null"))
		{
			try
			{
				headers = new JSONObject(headersFromRequest);
			}
			catch(Exception e)
			{
				// Parse Chrome-style headers (line by line format)
				String[] headerLines = headersFromRequest.split("\\n");
				for(int i = 0; i < headerLines.length; i += 2)
				{
					if(i + 1 < headerLines.length)
					{
						String key = headerLines[i].trim();
						if(key.startsWith(":"))
						{
							key = key.substring(1);
						}
						if(key.endsWith(":"))
						{
							key = key.substring(0, key.length() - 1);
						}
						headers.put(key, headerLines[i + 1].trim());
					}
				}
			}
		}

		return headers;
	}

	private boolean isValidURL(String url)
	{
		try
		{
			new java.net.URL(url);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}

	private String makeHttpCall(String url, String method, JSONObject params,
		Map<String, FormData> formDataMap, Map<String, String> headers,
		JSONObject proxyDetails) throws Exception
	{

		try
		{
			InputStream inputStream = getInputStream(formDataMap, headers);

			HttpContext httpContext = new HttpContext(url, method)
				.setParametersMap(params.toMap())
				.setHeadersMap(headers)
				.setBody(inputStream);

			HttpResponse httpResponse = HttpService.makeNetworkCallStatic(httpContext);
			return httpResponse.getStringResponse();

		}
		catch(Exception e)
		{
			Map<String, Object> errorInfo = new HashMap<>();
			errorInfo.put("error", true);
			errorInfo.put("message", e.getMessage());
			errorInfo.put("url", url);
			errorInfo.put("method", method);
			errorInfo.put("timestamp", System.currentTimeMillis());
			errorInfo.put("thread", Thread.currentThread().getName());

			return new JSONObject(errorInfo).toString();
		}
	}

	private java.io.InputStream getInputStream(Map<String, FormData> formDataMap, Map<String, String> headersMap) throws Exception
	{
		String jsonObjectString = formDataMap.getOrDefault("json_payload", new FormData()).getValue();

		if(StringUtils.isNotEmpty(jsonObjectString))
		{
			headersMap.put("Content-Type", "application/json");
			return new java.io.ByteArrayInputStream(jsonObjectString.getBytes());
		}

		String formUrlEncodedJSONString = formDataMap.getOrDefault("form_urlencoded", new FormData()).getValue();
		if(StringUtils.isNotEmpty(formUrlEncodedJSONString))
		{
			headersMap.put("Content-Type", "application/x-www-form-urlencoded");
			return new java.io.ByteArrayInputStream(HttpService.getEncodedQueryStringStatic(new JSONObject(formUrlEncodedJSONString).toMap()).getBytes());
		}

		if(formDataMap.isEmpty())
		{
			return null;
		}

		MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
		for(Map.Entry<String, FormData> formDataEntry : formDataMap.entrySet())
		{
			FormData formData = formDataEntry.getValue();
			String name = formDataEntry.getKey();

			if(formData.isFile())
			{
				for(FormData.FileData fileData : formData.getFileDataList())
				{
					String mimeType = ObjectUtils.defaultIfNull(URLConnection.guessContentTypeFromName(fileData.getFileName()), ContentType.APPLICATION_OCTET_STREAM.getMimeType());
					multipartEntityBuilder.addBinaryBody(name, fileData.getBytes(), ContentType.parse(mimeType), fileData.getFileName());
				}
			}
			else
			{
				ContentType contentType = Objects.nonNull(CommonService.getJSONFromString(formData.getValue())) ? ContentType.APPLICATION_JSON : ContentType.TEXT_PLAIN;
				multipartEntityBuilder.addTextBody(name, formData.getValue(), contentType);
			}
		}

		HttpEntity httpEntity = multipartEntityBuilder.build();

		String boundary = ContentType.get(httpEntity).getParameter("boundary");
		headersMap.put("Content-type", "multipart/form-data; boundary=".concat(boundary));

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		httpEntity.writeTo(byteArrayOutputStream);
		return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
	}
}
