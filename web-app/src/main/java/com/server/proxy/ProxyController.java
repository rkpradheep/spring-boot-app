package com.server.proxy;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import com.server.framework.builder.ApiResponseBuilder;

@RestController
@RequestMapping("/api/v1")
public class ProxyController
{
    @PostMapping("/proxy")
    public ResponseEntity<Map<String, Object>> forwardRequest(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("forwardedTo", request.get("targetUrl"));
            data.put("response", "Forwarded response placeholder");
            
            Map<String, Object> response = ApiResponseBuilder.success("Request forwarded successfully", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = ApiResponseBuilder.error("Failed to forward request: " + e.getMessage(), 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
