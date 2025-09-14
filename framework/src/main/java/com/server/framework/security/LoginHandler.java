package com.server.framework.security;

import org.springframework.stereotype.Component;
import org.json.JSONObject;

@Component
public class LoginHandler {
    
    public boolean authenticate(String username, String password) {
        // Simple authentication - replace with your actual logic
        return "admin".equals(username) && "admin".equals(password);
    }
    
    public JSONObject createUserSession(String username) {
        JSONObject user = new JSONObject();
        user.put("username", username);
        user.put("role", "admin");
        user.put("timestamp", System.currentTimeMillis());
        return user;
    }
}
