package com.server.webrtc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.server.framework.common.AppProperties;
import com.server.framework.builder.ApiResponseBuilder;

@RestController
public class WebRTCController
{
	@GetMapping("/api/v1/webrtc/iceservers")
	public ResponseEntity<Map<String, Object>> doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try {
			List<Map<String, String>> iceServers = new ArrayList<>();

			Map<String, String> iceServer = new HashMap<>();
			iceServer.put("urls", "stun:stun.relay.metered.ca:80");
			iceServers.add(iceServer);

			String userName = AppProperties.getProperty("webrtc.iceserver.username");
			String credential = AppProperties.getProperty("webrtc.iceserver.credential");

			for(String iceserverUrl : AppProperties.getProperty("webrtc.icecserver.urls").split(","))
			{
				iceServer = new HashMap<>();
				iceServer.put("urls", iceserverUrl);
				iceServer.put("username", userName);
				iceServer.put("credential", credential);
				iceServers.add(iceServer);
			}

			Map<String, Object> apiResponse = ApiResponseBuilder.success("ICE servers retrieved successfully", iceServers);
			return ResponseEntity.ok(apiResponse);
		} catch (Exception e) {
			Map<String, Object> apiResponse = ApiResponseBuilder.error("Failed to retrieve ICE servers: " + e.getMessage(), 500);
			return ResponseEntity.internalServerError().body(apiResponse);
		}
	}
}
