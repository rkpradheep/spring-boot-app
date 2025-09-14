package com.server.framework.security;

import com.server.framework.common.DateUtil;
import com.server.framework.entity.UserEntity;
import com.server.framework.service.LoginService;
import com.server.framework.user.RoleEnum;
import com.server.framework.builder.ApiResponseBuilder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class AuthController {

    @Autowired
    private LoginService loginService;

    public static class LoginRequest {
        public String username;
        public String name;
        public String password;
    }

    @PostMapping(value = "/api/v1/authenticate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> authenticate(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        try {
            String userName = req.username != null ? req.username : req.name;
            UserEntity userEntity = loginService.validateCredentials(userName, req.password);
            
            if (userEntity == null) {
                // Publish login failed event
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("username", userName);
                eventData.put("ipAddress", getClientIpAddress(request));
                eventData.put("userAgent", request.getHeader("User-Agent"));
                eventData.put("reason", "Invalid credentials");

                return ApiResponseBuilder.error("Invalid credentials", 401);
            }
            
            String sessionId = UUID.randomUUID().toString();
            boolean isAdminLogin = userEntity.getRoleType() != null && userEntity.getRoleType().equals(RoleEnum.ADMIN.getType());
            long allowedSessionDurationInMillis = isAdminLogin ? 24 * 60 * 60 * 1000L : 30 * 60 * 1000L;
            long expiryTimeInMillis = DateUtil.getCurrentTimeInMillis() + allowedSessionDurationInMillis;
            loginService.addSession(sessionId, userEntity.getId(), expiryTimeInMillis);
            
            // Publish session created event
            Map<String, Object> sessionEventData = new HashMap<>();
            sessionEventData.put("sessionId", sessionId);
            sessionEventData.put("userId", userEntity.getId());
            sessionEventData.put("username", userEntity.getName());
            sessionEventData.put("ipAddress", getClientIpAddress(request));
            sessionEventData.put("userAgent", request.getHeader("User-Agent"));
            sessionEventData.put("isAdmin", isAdminLogin);
            sessionEventData.put("expiryTime", expiryTimeInMillis);

            Cookie cookie = new Cookie("iam_token", sessionId);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge((int)allowedSessionDurationInMillis / 1000);
            response.addCookie(cookie);
            
            // Publish successful login event
            Map<String, Object> loginEventData = new HashMap<>();
            loginEventData.put("userId", userEntity.getId());
            loginEventData.put("username", userEntity.getName());
            loginEventData.put("ipAddress", getClientIpAddress(request));
            loginEventData.put("userAgent", request.getHeader("User-Agent"));
            loginEventData.put("isAdmin", isAdminLogin);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", userEntity.getId());
            userInfo.put("name", userEntity.getName());
            userInfo.put("roleType", userEntity.getRoleType());
            
            return ApiResponseBuilder.success("Login successful", userInfo);
        } catch (Exception e) {
            return ApiResponseBuilder.error("Authentication failed: " + e.getMessage(), 500);
        }
    }

    @RequestMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String maxAge = "Max-Age=0";
        StringBuilder header = new StringBuilder()
            .append("iam_token" + "=")
            .append(SecurityUtil.getSessionId())
            //.append("; Secure")
            //.append("; SameSite=None")
            .append("; Path=/;")
            .append(maxAge);

        response.setHeader("Set-Cookie", header.toString());
        response.sendRedirect("/");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
