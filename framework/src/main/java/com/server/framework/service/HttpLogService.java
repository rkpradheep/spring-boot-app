package com.server.framework.service;

import com.server.framework.entity.HttpLogEntity;
import com.server.framework.repository.HttpLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.zip.GZIPInputStream;

@Service
@Transactional
public class HttpLogService {

	@Autowired
	private HttpLogRepository httpLogRepository;

	public Long logIncoming(HttpServletRequest request) {
		HttpLogEntity log = new HttpLogEntity();
		log.setUrl(request.getRequestURL().toString());
		log.setMethod(request.getMethod());
		log.setIp(request.getRemoteAddr());
		log.setParameters(buildQueryJson(request.getQueryString()));
		log.setRequestHeaders(buildHeadersJson(request));
		log.setThreadName(Thread.currentThread().getName());
		log.setCreatedTime(System.currentTimeMillis());
		log.setEntityType(0);
		log.setIsOutgoing(false);
		return httpLogRepository.save(log).getId();
	}

	public Long logOutgoing(HttpURLConnection connection, String requestJson) {
		HttpLogEntity log = new HttpLogEntity();
		log.setUrl(getURLString(connection));
		log.setMethod(connection.getRequestMethod());
		log.setIp(null);
		log.setParameters(getQuery(connection));
		log.setRequestHeaders(buildHeadersJson(connection));
		log.setRequestData(requestJson);
		log.setThreadName(Thread.currentThread().getName());
		log.setCreatedTime(System.currentTimeMillis());
		log.setEntityType(0);
		log.setIsOutgoing(true);
		return httpLogRepository.save(log).getId();
	}

	public void updateWithResponse(Long httpLogId, HttpURLConnection connection) {
		Optional<HttpLogEntity> opt = httpLogRepository.findById(httpLogId);
		if (opt.isEmpty()) return;
		HttpLogEntity log = opt.get();
		try {
			log.setStatusCode(connection.getResponseCode());
			InputStream is = connection.getErrorStream();
			if (is == null) is = connection.getInputStream();
			String encoding = connection.getHeaderField("Content-Encoding");
			if ("gzip".equalsIgnoreCase(encoding)) {
				is = new GZIPInputStream(is);
			}
			byte[] bytes = is == null ? new byte[0] : is.readAllBytes();
			log.setResponseData(bytes.length == 0 ? null : new String(bytes));
			log.setResponseHeaders(buildResponseHeadersJson(connection));
			httpLogRepository.save(log);
		} catch (Exception ignore) {
		}
	}

	private String buildQueryJson(String query) {
		if (query == null || query.isEmpty()) return null;
		Map<String, String> map = new HashMap<>();
		for (String part : query.split("&")) {
			String[] kv = part.split("=", 2);
			String k = kv.length > 0 ? kv[0] : "";
			String v = kv.length > 1 ? kv[1] : "";
			map.put(k, v);
		}
		return map.isEmpty() ? null : new JSONObject(map).toString();
	}

	private String buildHeadersJson(HttpServletRequest request) {
		Map<String, String> headers = new HashMap<>();
		Enumeration<String> names = request.getHeaderNames();
		while (names != null && names.hasMoreElements()) {
			String name = names.nextElement();
			headers.put(name, request.getHeader(name));
		}
		return headers.isEmpty() ? null : new JSONObject(headers).toString();
	}

	private String buildHeadersJson(HttpURLConnection connection) {
		Map<String, String> headers = new HashMap<>();
		for (Map.Entry<String, java.util.List<String>> e : connection.getRequestProperties().entrySet()) {
			if (e.getKey() == null) continue;
			headers.put(e.getKey(), String.join("", e.getValue()));
		}
		return headers.isEmpty() ? null : new JSONObject(headers).toString();
	}

	private String buildResponseHeadersJson(HttpURLConnection connection) {
		Map<String, String> headers = new HashMap<>();
		for (Map.Entry<String, java.util.List<String>> e : connection.getHeaderFields().entrySet()) {
			if (e.getKey() == null) continue;
			headers.put(e.getKey(), String.join("", e.getValue()));
		}
		return headers.isEmpty() ? null : new JSONObject(headers).toString();
	}

	private String getURLString(HttpURLConnection connection) {
		return connection.getURL().getProtocol() + "://" + connection.getURL().getHost() + (connection.getURL().getPort() == -1 ? "" : (":" + connection.getURL().getPort())) + connection.getURL().getPath();
	}

	private String getQuery(HttpURLConnection connection) {
		return connection.getURL().getQuery();
	}

}
