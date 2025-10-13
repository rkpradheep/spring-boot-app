package com.server.zoho;

import com.server.framework.common.AppContextHolder;
import com.server.framework.common.AppProperties;
import com.server.framework.common.DateUtil;
import com.server.framework.common.CommonService;
import com.server.framework.error.AppException;
import com.server.framework.http.HttpService;
import com.server.framework.http.HttpContext;
import com.server.framework.security.SecurityUtil;
import com.server.framework.service.ConfigurationService;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ZohoService
{
	private static final Logger LOGGER = Logger.getLogger(ZohoService.class.getName());
	private static final Map<String, String> DC_DOMAIN_MAPPING;
	private static final Map<String, Long> TOKEN_HASH_EXPIRY_TIME = new ConcurrentHashMap<>();
	private static final Map<String, String> TOKEN_HASH_EMAIL_CACHE = new ConcurrentHashMap<>()
	{
		@Override public String get(Object tokenHash)
		{
			String email = super.get(tokenHash);
			if(StringUtils.isEmpty(email))
			{
				return null;
			}
			if(TOKEN_HASH_EXPIRY_TIME.get(tokenHash) < DateUtil.getCurrentTimeInMillis())
			{
				remove(tokenHash);
				TOKEN_HASH_EXPIRY_TIME.remove((String) tokenHash);
				return null;
			}
			return email;
		}

		@Override public String put(String tokenHash, String email)
		{
			TOKEN_HASH_EXPIRY_TIME.put(tokenHash, DateUtil.getCurrentTime().plusMinutes(30).toInstant().toEpochMilli());
			return super.put(tokenHash, email);
		}

		@Override public String computeIfAbsent(String tokenHash, Function<? super String, ? extends String> mappingFunction)
		{
			if(StringUtils.isNotEmpty(get(tokenHash)))
			{
				return get(tokenHash);
			}
			TOKEN_HASH_EXPIRY_TIME.put(tokenHash, DateUtil.getCurrentTime().plusMinutes(30).toInstant().toEpochMilli());
			return super.computeIfAbsent(tokenHash, mappingFunction);
		}
	};

	static
	{

		Map<String, String> dcDomainMappingTmp = new HashMap<>();
		dcDomainMappingTmp.put("dev", "csez.zohocorpin.com");
		dcDomainMappingTmp.put("csez", "csez.zohocorpin.com");
		dcDomainMappingTmp.put("local", "localzoho.com");
		dcDomainMappingTmp.put("us", "zoho.com");
		dcDomainMappingTmp.put("in", "zoho.in");
		dcDomainMappingTmp.put("eu", "zoho.eu");
		dcDomainMappingTmp.put("au", "zoho.com.au");
		dcDomainMappingTmp.put("jp", "zoho.jp");
		dcDomainMappingTmp.put("ca", "zohocloud.ca");
		dcDomainMappingTmp.put("uk", "zoho.uk");

		DC_DOMAIN_MAPPING = Collections.unmodifiableMap(dcDomainMappingTmp);

	}

	public static void doAuthentication() throws Exception
	{
		if(SecurityUtil.getCurrentRequestURI().equals("/api/v1/zoho/mark-as-test-org") || SecurityUtil.getCurrentRequestURI().equals("/api/v1/zoho/mark-as-paid-org"))
		{
			return;
		}
		String currentUserEmail = getCurrentUserEmail();
		if(StringUtils.isNotEmpty(currentUserEmail))
		{
			String allowedUsers = AppContextHolder.getBean(ConfigurationService.class).getValue("zoho.critical.operation.allowed.users").orElse(AppProperties.getProperty("zoho.critical.operation.allowed.users"));
			boolean isValidUser = Arrays.stream(allowedUsers.split(",")).map(String::trim).anyMatch(currentUserEmail::equals);
			if(!isValidUser)
			{
				LOGGER.log(Level.SEVERE, "User " + currentUserEmail + " does not have access to perform this operation");
				throw new AppException("access_denied", "User " + currentUserEmail + " does not have access to perform this operation");
			}
			return;
		}

		String clientId = AppProperties.getProperty("zoho.auth.client.id");
		String redirectUri = AppProperties.getProperty("zoho.auth.redirect.uri");
		String scopes = AppProperties.getProperty("zoho.auth.scopes");

		URIBuilder builder = new URIBuilder(getDomainUrl("accounts", "/oauth/v2/auth", "in"));
		builder.addParameter("scope", scopes);
		builder.addParameter("client_id", clientId);
		builder.addParameter("response_type", "code");
		builder.addParameter("redirect_uri", redirectUri);
		builder.addParameter("prompt", "consent");
		builder.addParameter("access_type", "online");
		throw new AppException("reauth_required", "Authentication required", Map.of("auth_uri", builder.toString()));
	}

	public static Map<String, String> exchangeCodeForTokens(String authCode)
	{
		try
		{
			String clientId = AppProperties.getProperty("zoho.auth.client.id");
			String clientSecret = AppProperties.getProperty("zoho.auth.client.secret");
			String redirectUri = AppProperties.getProperty("zoho.auth.redirect.uri");

			if(authCode == null || authCode.isEmpty())
			{
				throw new IllegalArgumentException("Authorization code is required");
			}

			HttpContext context = new HttpContext("https://accounts.zoho.com/oauth/v2/token", "POST");
			context.setParam("grant_type", "authorization_code");
			context.setParam("client_id", clientId);
			context.setParam("client_secret", clientSecret);
			context.setParam("redirect_uri", redirectUri);
			context.setParam("code", authCode);

			JSONObject jsonResponse = HttpService.makeNetworkCallStatic(context).getJSONResponse();

			Map<String, String> tokens = new HashMap<>();
			tokens.put("access_token", jsonResponse.optString("access_token"));
			tokens.put("refresh_token", jsonResponse.optString("refresh_token"));
			tokens.put("expires_in", String.valueOf(jsonResponse.optInt("expires_in")));
			tokens.put("token_type", jsonResponse.optString("token_type"));
			tokens.put("status", "success");

			return tokens;

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error exchanging code for tokens", e);
		}

		Map<String, String> errorResponse = new HashMap<>();
		errorResponse.put("status", "error");
		errorResponse.put("message", "Failed to exchange code for tokens");
		return errorResponse;
	}

	public static String getCurrentUserEmail() throws Exception
	{
		String zohoToken = SecurityUtil.getCookieValue("zoho_authenticated_token");
		if(StringUtils.isEmpty(zohoToken))
		{
			return null;
		}

		String zohoTokenDecrypted = CommonService.getAESDecryptedValue(zohoToken);
		String tokenHash = DigestUtils.sha256Hex(zohoTokenDecrypted);
		TOKEN_HASH_EMAIL_CACHE.computeIfAbsent(tokenHash, (k) -> {
			String url = ZohoService.getDomainUrl("accounts", "/oauth/user/info", "in");
			try
			{
				JSONObject response = HttpService.makeNetworkCallStatic(new HttpContext(url, HttpGet.METHOD_NAME).setHeadersMap(Map.of("Authorization", "Bearer ".concat(zohoTokenDecrypted)))).getJSONResponse();
				return response.optString("Email");
			}
			catch(IOException e)
			{
				throw new RuntimeException(e);
			}
		});

		return TOKEN_HASH_EMAIL_CACHE.get(tokenHash);
	}

	public static String getDomainUrl(String subDomain, String resourceUri, String dc)
	{
		return "https://" + subDomain + "." + DC_DOMAIN_MAPPING.get(dc) + resourceUri;
	}
}

