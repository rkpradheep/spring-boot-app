package com.server.framework.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import sun.net.www.http.PosterOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.websocket.server.WsServerContainer;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.server.PathContainer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import com.server.framework.common.AppContextHolder;
import com.server.framework.service.HttpLogService;

import com.server.framework.common.CommonService;
import com.server.framework.entity.UserEntity;
import com.server.framework.http.FormData;

public class SecurityUtil
{
	private static final Logger LOGGER = Logger.getLogger(SecurityUtil.class.getName());

	static final Set<String> VALID_ENDPOINTS = new HashSet<>();

	public static String getUploadsPath()
	{
		return CommonService.HOME_PATH + "/uploads";
	}

	public static UserEntity getCurrentUser()
	{
		return SecurityFilter.CURRENT_USER_TL.get();
	}

	public static HttpServletRequest getCurrentRequest()
	{
		return SecurityFilter.CURRENT_REQUEST_TL.get();
	}

	public static boolean isRequestFromLoopBackAddress()
	{
		try
		{
			return java.net.InetAddress.getByName(getCurrentRequest().getRemoteAddr()).isLoopbackAddress();
		}
		catch(Exception e)
		{
			return false;
		}
	}

	public static String getOriginatingUserIP()
	{
		HttpServletRequest request = getCurrentRequest();
		if(request == null)
			return "127.0.0.1";

		String ip = request.getHeader("X-Forwarded-For");
		if(StringUtils.isNotBlank(ip))
			return ip.split(",")[0].trim();

		ip = request.getHeader("X-Real-IP");
		return StringUtils.defaultIfBlank(ip, request.getRemoteAddr());
	}

	public static boolean isLoggedIn()
	{
		return getCurrentUser() != null;
	}

	public static boolean canSkipAuthentication(String requestURI)
	{
		return SecurityFilter.IS_SKIP_AUTHENTICATION_ENDPOINTS.apply(requestURI);
	}

	public static boolean isAdminCall(String requestURI)
	{
		return requestURI != null && requestURI.startsWith("/api/v1/admin") || requestURI.startsWith("/admin");
	}

	public static String getAuthToken()
	{
		HttpServletRequest request = getCurrentRequest();
		if(request == null)
			return "";

		String token = request.getParameter("token");
		if(StringUtils.isNotBlank(token))
		{
			return token;
		}

		String authorizationHeader = request.getHeader("Authorization");
		if(StringUtils.isNotEmpty(authorizationHeader) && authorizationHeader.startsWith("Bearer "))
		{
			return authorizationHeader.substring(7).trim();
		}

		return "";
	}

	public static String getSessionId()
	{
		return getCookieValue("iam_token");
	}

	public static String getCookieValue(String cookieName)
	{
		HttpServletRequest request = getCurrentRequest();
		if(request == null || request.getCookies() == null)
			return "";

		for(jakarta.servlet.http.Cookie cookie : request.getCookies())
		{
			if(cookieName.equals(cookie.getName()))
			{
				return cookie.getValue();
			}
		}
		return "";
	}

	public static void writeJSONResponse(HttpServletResponse response, Object responseObject) throws IOException
	{
		response.setContentType("application/json;charset=UTF-8");

		if(responseObject instanceof String)
		{
			response.getWriter().print(responseObject.toString());
		}
		else
		{
			com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
			response.getWriter().print(objectMapper.writeValueAsString(responseObject));
		}
	}

	public static void writerErrorResponse(HttpServletResponse response, String message) throws IOException
	{
		writerErrorResponse(response, 400, "error", message);
	}

	public static void writerErrorResponse(HttpServletResponse response, int statusCode, String code, String message) throws IOException
	{
		response.setStatus(statusCode);

		Map<String, String> responseMap = new HashMap<>();
		responseMap.put("error", message);
		responseMap.put("code", code);

		writeJSONResponse(response, responseMap);
	}

	public static boolean isValidJSON(String data)
	{
		try
		{
			com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
			objectMapper.readTree(data);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}

	public static boolean isValidEndPoint(ResourceLoader resourceLoader)
	{
		String path = getCurrentRequest().getRequestURI();

		PathPatternParser parser = new PathPatternParser();
		boolean isValid = VALID_ENDPOINTS.stream().anyMatch(pattern -> {
			PathPattern parsedPattern = parser.parse(pattern);
			return parsedPattern.matches(PathContainer.parsePath(path));
		});

		return isValid || resourceLoader.getResource("classpath:/static" + path).exists() || (path.matches("/uploads/.*") && resourceLoader.getResource("file:" + SecurityUtil.getUploadsPath() + "/" + path.split("/")[2]).exists());
	}

	public static boolean isValidTomcatWebSocketEndPoint(String endPoint)
	{
		Object mappingResult = ((WsServerContainer) SecurityFilter.SERVLET_CONTEXT_TL.get().getAttribute("jakarta.websocket.server.ServerContainer")).findMapping(endPoint);
		return Objects.nonNull(mappingResult);
	}

	public static boolean isValidWebSocketEndPoint(ApplicationContext applicationContext, String path)
	{
		try
		{
			if(isValidTomcatWebSocketEndPoint(path))
			{
				return true;
			}

			ConfigurableApplicationContext cac = (applicationContext instanceof ConfigurableApplicationContext) ? (ConfigurableApplicationContext) applicationContext : null;
			if(cac == null)
				return false;

			Map<String, SimpleUrlHandlerMapping> mappings = cac.getBeansOfType(SimpleUrlHandlerMapping.class);
			for(SimpleUrlHandlerMapping mapping : mappings.values())
			{
				Map<String, ?> urlMap = mapping.getUrlMap();
				if(urlMap.containsKey(path))
				{
					Object handler = urlMap.get(path);
					if(handler instanceof WebSocketHttpRequestHandler)
					{
						return true;
					}
				}
			}
		}
		catch(Exception ignore)
		{
		}
		return false;
	}

	public static boolean isResourceUri(String endPoint)
	{
		return endPoint.matches("(/(((resources|css|js|uploads)/.*)|favicon.ico))");
	}

	public static boolean setDisableHttpLog(boolean disableHttpLog)
	{
		boolean oldValue = getDisableHttpLog();
		SecurityFilter.DISABLE_HTTP_LOG.set(disableHttpLog);
		return oldValue;
	}

	public static boolean getDisableHttpLog()
	{
		Boolean value = SecurityFilter.DISABLE_HTTP_LOG.get();
		return value != null && value;
	}

	public static Map<String, FormData> parseMultiPartFormData(HttpServletRequest request) throws IOException
	{
		Map<String, FormData> formDataMap = new HashMap<>();

		try
		{
			// Use Jakarta Servlet API for multipart parsing
			for(Part part : request.getParts())
			{
				String fieldName = part.getName();

				if(isFormField(part))
				{
					// Handle form field
					String fieldValue = readPartAsString(part);

					FormData formData = new FormData();
					formData.setValue(fieldValue);
					formData.setContentType(part.getContentType());
					formDataMap.put(fieldName, formData);
				}
				else
				{
					// Handle file field
					FormData formData = formDataMap.getOrDefault(fieldName, new FormData());
					formData.setIsFile(true);

					String fileName = getFileName(part);
					byte[] fileBytes = readAllBytes(part.getInputStream());

					FormData.FileData fileData = new FormData.FileData(fileName, fileBytes, part.getContentType());
					formData.addFileData(fileData);

					formDataMap.put(fieldName, formData);
				}
			}
		}
		catch(Exception e)
		{
			// Return empty map if parsing fails
			return new HashMap<>();
		}

		return formDataMap;
	}

	private static boolean isFormField(Part part)
	{
		return getFileName(part) == null;
	}

	private static String getFileName(Part part)
	{
		String contentDisposition = part.getHeader("content-disposition");
		if(contentDisposition == null)
			return null;

		for(String token : contentDisposition.split(";"))
		{
			if(token.trim().startsWith("filename"))
			{
				String fileName = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
				return fileName.isEmpty() ? null : fileName;
			}
		}
		return null;
	}

	private static String readPartAsString(Part part) throws IOException
	{
		try(InputStream inputStream = part.getInputStream())
		{
			return new String(readAllBytes(inputStream));
		}
	}

	public static byte[] readAllBytes(InputStream inputStream) throws IOException
	{
		byte[] bytes = new byte[inputStream.available()];
		inputStream.read(bytes);
		return bytes;
	}

	public static String getCurrentRequestDomain()
	{
		try
		{
			URL url = new URL(getCurrentRequest().getRequestURL().toString());
			return url.getProtocol() + "://" + url.getHost();
		}
		catch(Exception e)
		{
			return getCurrentRequest().getRequestURL().toString();
		}
	}

	public static JSONObject getCurrentRequestJSONObject() throws IOException
	{
		return getJSONObject(getCurrentRequest());
	}

	public static JSONObject getJSONObject(HttpServletRequest request) throws IOException
	{
		if(!StringUtils.equals(request.getContentType(), "application/json"))
		{
			return null;
		}
		if(Objects.nonNull(request.getAttribute("JSON_PAYLOAD")))
		{
			return new JSONObject(((JSONObject) request.getAttribute("JSON_PAYLOAD")).toMap());
		}
		try
		{
			JSONObject jsonObject = new JSONObject(request.getReader().lines().collect(Collectors.joining()));
			request.setAttribute("JSON_PAYLOAD", jsonObject);
			return new JSONObject(jsonObject.toMap());
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public static void writeSuccessJSONResponse(HttpServletResponse response, String responseMessage, Object data) throws IOException
	{
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("message", responseMessage);
		responseMap.put("data", data);
		writeJSONResponse(response, responseMap);
	}

	public static String getURLString(URL url)
	{
		int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
		return url.getProtocol() + "://" + url.getHost().concat(":").concat(String.valueOf(port)).concat(url.getPath());
	}

	public static Long addHttpLog(HttpURLConnection connection)
	{
		try
		{
			HttpLogService httpLogService = AppContextHolder.getBean(HttpLogService.class);
			PosterOutputStream outputStream = StringUtils.equals(connection.getRequestProperty("Content-Type"), "application/json") ? (PosterOutputStream) connection.getOutputStream() : null;
			String requestJSON = Objects.nonNull(outputStream) ? outputStream.toString() : null;
			return httpLogService.logOutgoing(connection, requestJSON);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.FINE, "Exception occurred while adding HTTP log: " + e.getMessage());
			return null;
		}
	}

	public static void updateHttpLog(long httpLogId, HttpURLConnection connection)
	{
		try
		{
			HttpLogService httpLogService = AppContextHolder.getBean(HttpLogService.class);
			httpLogService.updateWithResponse(httpLogId, connection);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.FINE, "Exception occurred while updating HTTP log: " + e.getMessage());
		}
	}
}
