package com.server.framework.service;

import com.server.framework.http.HttpService;
import com.server.framework.http.HttpContext;
import com.server.framework.common.CommonService;
import com.server.framework.security.SecurityUtil;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.http.client.utils.URIBuilder;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class OAuthService {
    
    private static final Logger LOGGER = Logger.getLogger(OAuthService.class.getName());
    
    @Autowired
    private AuthTokenService authTokenService;

    public Map<String, Object> generateAuthUrl(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String scope = (String) request.get("scope");
            String authURL = (String) request.get("url");
            if (scope == null || scope.isEmpty()) {
                response.put("success", false);
                response.put("message", "Scope is required");
                return response;
            }
            String dc = getDataCenter(authURL);
            JSONObject credentials = CommonService.getZohoSecrets(dc);
            String clientId = credentials.getString("client_id");
            String redirectUri = credentials.getString("redirect_uri");
            String currentDomain = SecurityUtil.getCurrentRequestDomain();
            String authUrl;
            try {
                URIBuilder builder = new URIBuilder(authURL);
                builder.addParameter("scope", scope);
                builder.addParameter("client_id", clientId);
                builder.addParameter("response_type", "code");
                builder.addParameter("redirect_uri", redirectUri);
                builder.addParameter("prompt", "consent");
                builder.addParameter("state", currentDomain + "/zoho/oauth-tool");
                builder.addParameter("access_type", "offline");
                authUrl = builder.build().toString();
            } catch (URISyntaxException e) {
                response.put("success", false);
                response.put("message", "Invalid authorization URL");
                return response;
            }
            response.put("success", true);
            response.put("redirect_uri", authUrl);
            response.put("client_id", clientId);
            response.put("scope", scope);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating auth URL", e);
            response.put("success", false);
            response.put("message", "Failed to generate authorization URL");
        }
        return response;
    }

    public Map<String, Object> generateTokens(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String clientId = (String) request.get("client_id");
            String clientSecret = (String) request.get("client_secret");
            String tokenUrl = (String) request.get("url");
            String code = (String) request.get("code");
            String redirectURI = (String) request.get("redirect_uri");

            if (clientId == null) {
                String dc = getDataCenter(tokenUrl);
                JSONObject credentials = CommonService.getZohoSecrets(dc);
                clientId = credentials.getString( "client_id");
                clientSecret = credentials.getString("client_secret");
                redirectURI = credentials.getString("redirect_uri");
            }

            HttpContext context = new HttpContext(tokenUrl, "POST");
            context.setParam("client_id", clientId);
            context.setParam("client_secret", clientSecret);

            if(StringUtils.isNotEmpty(code))
            {
                context.setParam("code", code);
                context.setParam("grant_type",  "authorization_code");
                context.setParam("redirect_uri",  redirectURI);
            }
            else
            {
                String scope = (String) request.get("scope");
                if(StringUtils.isNotEmpty(scope))
                {
                    context.setParam("scope", scope);
                }
                context.setParam("grant_type",  "refresh_token");
                context.setParam("refresh_token",  request.get("refresh_token"));
            }


            JSONObject tokenResponse = HttpService.makeNetworkCallStatic(context).getJSONResponse();

            if (tokenResponse.has("access_token")) {
                response.put("success", true);
                response.put("access_token", tokenResponse.getString("access_token"));
                response.put("token_type", tokenResponse.optString("token_type", "Bearer"));
                response.put("expires_in", tokenResponse.optInt("expires_in", 3600));

                if (tokenResponse.has("refresh_token")) {
                    response.put("refresh_token", tokenResponse.getString("refresh_token"));
                }

            } else {
                response.put("success", false);
                response.put("error", tokenResponse.optString("error", "unknown_error"));
                response.put("error_description", tokenResponse.optString("error_description", "Failed to generate tokens"));
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating client credentials tokens", e);
            response.put("success", false);
            response.put("message", "Failed to generate tokens using client credentials");
        }

        return response;
    }

    private String getDataCenter(String url) {
        if (url.contains("localzoho.com")) return "local";
        if (url.contains("csez.zohocorpin.com")) return "dev";
        if (url.contains("zoho.in")) return "in";
        if (url.contains("zoho.com")) return "us";
        if (url.contains("zoho.eu")) return "eu";
        if (url.contains("zoho.com.au")) return "au";
        if (url.contains("zoho.com.cn")) return "cn";

        return "us";
    }
}
