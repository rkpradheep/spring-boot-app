package com.server.oauth;

import com.server.framework.service.OAuthService;
import com.server.framework.builder.ApiResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {

    @Autowired
    private OAuthService oauthService;

    @PostMapping("/tokens")
    public ResponseEntity<Map<String, Object>> getTokens(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> tokenData = oauthService.generateTokens(request);
            Map<String, Object> response = ApiResponseBuilder.success("Tokens generated successfully", tokenData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = ApiResponseBuilder.error("Failed to generate tokens: " + e.getMessage(), 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/code")
    public ResponseEntity<Map<String, Object>> generateAuthUrlForCodeGeneration(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> authUrlData = oauthService.generateAuthUrl(request);
            Map<String, Object> response = ApiResponseBuilder.success("Auth URL generated successfully", authUrlData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = ApiResponseBuilder.error("Failed to generate auth URL: " + e.getMessage(), 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
