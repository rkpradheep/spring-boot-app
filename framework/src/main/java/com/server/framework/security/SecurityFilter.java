package com.server.framework.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.server.framework.common.AppProperties;
import com.server.framework.entity.UserEntity;
import com.server.framework.security.throttle.ThrottleNewHandler;
import com.server.framework.service.HttpLogService;
import com.server.framework.service.UserService;
import com.server.framework.user.RoleEnum;

@Component
public class SecurityFilter implements Filter
{
	@Autowired
	private ApplicationContext applicationContext;

	static final ThreadLocal<UserEntity> CURRENT_USER_TL = new InheritableThreadLocal<>();
	static final ThreadLocal<HttpServletRequest> CURRENT_REQUEST_TL = new InheritableThreadLocal<>();
	static final ThreadLocal<ServletContext> SERVLET_CONTEXT_TL = new InheritableThreadLocal<>();
	static final ThreadLocal<Boolean> DISABLE_HTTP_LOG = new InheritableThreadLocal<>();

	public static final List<String> SKIP_AUTHENTICATION_ENDPOINTS = Arrays.asList(
		"/_app/health", "/api/v1/(admin/)?authenticate", "/login(\\.html)?",
		"((/(resources|css|js|uploads)/.*)|/favicon.ico)", "/api/v1/jobs", "/payoutlogs",
		"/api/v1/payout/httplogs", "/api/v1/admin/live/logs", "/.well-known/.*",
		"(/dbtool(.(html|jsp))?|/sasstats|/api/v1/(sas|zoho)/.*)", "/csv", "(/api/v1/)?/zoho/.*",
		"/hotswap", "/network", "/livelogs", "/freessl", "/snakegame", "/stats", "(/api/v1/patterns/.*|design_patterns-uri.html)"
	);
	public static final Function<String, Boolean> IS_SKIP_AUTHENTICATION_ENDPOINTS = requestURI ->
		SKIP_AUTHENTICATION_ENDPOINTS.stream().anyMatch(requestURI::matches);

	@Autowired
	private ResourceLoader resourceLoader;
	private static final Logger LOGGER = Logger.getLogger(SecurityFilter.class.getName());

	@Autowired
	private HttpLogService httpLogService;

	@Autowired
	private UserService userService;

	public SecurityFilter(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping requestMappingHandlerMapping)
	{
		Map<RequestMappingInfo, ?> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

		for(RequestMappingInfo info : handlerMethods.keySet())
		{
			SecurityUtil.VALID_ENDPOINTS.addAll(info.getPatternValues());
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException
	{
		String oldThreadName = Thread.currentThread().getName();

		try
		{
			ServletContext servletContext = request.getServletContext();
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			HttpServletResponse httpResponse = (HttpServletResponse) response;

			// Set ThreadLocal variables like in the original tomcat-app
			SERVLET_CONTEXT_TL.set(servletContext);
			CURRENT_REQUEST_TL.set(httpRequest);

			httpResponse.setHeader("Server_Build_Label", AppProperties.getProperty("build.label", "dev"));

			String requestURI = httpRequest.getRequestURI().replaceFirst(httpRequest.getContextPath(), StringUtils.EMPTY);
			String requestURL = httpRequest.getRequestURL().toString();

			boolean isResource = SecurityUtil.isResourceUri(requestURI);

			if(!isResource)
			{
				LOGGER.log(Level.INFO, "Request {0} from {1}", new Object[] {requestURI, SecurityUtil.getOriginatingUserIP()});
			}

			boolean isValidEndpoint = SecurityUtil.isValidEndPoint(resourceLoader) || SecurityUtil.isValidWebSocketEndPoint(applicationContext, requestURI) || AppProperties.getBoolean("spring.h2.console.enabled") && requestURI.matches(AppProperties.getProperty("spring.h2.console.path") + "/?.*");
			if(!isValidEndpoint)
			{
				httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			// Rate limiting using throttle handler
			if(!isResource && !ThrottleNewHandler.tryAcquire(httpResponse))
			{
				httpResponse.sendError(429);
				return;
			}

			// Production hardening: restrict direct access to legacy pages
			if(AppProperties.getBooleanProperty("production", false) && (requestURI.equals("/dbtool") || requestURI.equals("/zoho")))
			{
				httpResponse.sendRedirect("/");
				return;
			}

			UserEntity userEntity;
			String bearer = extractBearer(httpRequest.getHeader("Authorization"));
			if(bearer != null)
				userEntity = userService.findByToken(bearer).orElse(null);
			else
			{
				String sessionId = getCookie(httpRequest, "iam_token");
				userEntity = sessionId != null ? userService.findBySession(sessionId).orElse(null) : null;
			}

			CURRENT_USER_TL.set(userEntity);

			if(!isResource && SecurityUtil.isLoggedIn())
			{
				Thread.currentThread().setName(Thread.currentThread().getName().concat("/".concat(SecurityUtil.getCurrentUser().getName()).concat("-").concat(SecurityUtil.getCurrentUser().getId().toString())));
			}

			// Skip authentication if configured or public endpoint
			if(AppProperties.getBooleanProperty("skip.authentication", false) || SecurityUtil.canSkipAuthentication(requestURI))
			{
				if(requestURI.equals("/login") && SecurityUtil.isLoggedIn())
				{
					httpResponse.sendRedirect("/");
					return;
				}
				if(!isResource && !requestURI.equals("/api/v1/admin/db/execute"))
				{
					try
					{
						httpLogService.logIncoming(httpRequest);
					}
					catch(Exception ignore)
					{
					}
				}
				chain.doFilter(request, response);
				return;
			}

			// Enforce authentication for protected endpoints
			if(SecurityUtil.isLoggedIn())
			{
				if(!isAdmin() && SecurityUtil.isAdminCall(requestURI))
				{
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
				if(!isResource && !requestURI.equals("/api/v1/admin/db/execute"))
				{
					try
					{
						httpLogService.logIncoming(httpRequest);
					}
					catch(Exception ignore)
					{
					}
				}
				chain.doFilter(request, response);
			}
			else
			{
				if(isRestApi(requestURI))
				{
					String errorMessage = StringUtils.isNotEmpty(httpRequest.getHeader("Authorization"))
						? "Invalid value passed for Authorization header" : "Session expired. Please login again and try.";
					Map<String, String> data = new HashMap<>();
					data.put("redirect_uri", getRedirectURI(httpRequest));
					writeErrorJson(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "authentication_needed", errorMessage, data);
				}
				else
				{
					String loginPage = httpRequest.getContextPath() + "/login?redirect_uri=" + URLEncoder.encode(requestURL, StandardCharsets.UTF_8);
					httpResponse.sendRedirect(loginPage);
				}
			}
		}
		finally

		{
			CURRENT_USER_TL.remove();
			SERVLET_CONTEXT_TL.remove();
			CURRENT_REQUEST_TL.remove();
			DISABLE_HTTP_LOG.remove();
			Thread.currentThread().setName(oldThreadName);
		}
	}

	private boolean isRestApi(String uri)
	{
		return uri != null && uri.startsWith("/api/");
	}

	private boolean isAdmin()
	{
		return SecurityUtil.isLoggedIn() && SecurityUtil.getCurrentUser().getRoleType() == RoleEnum.ADMIN.getType();
	}

	private String getRedirectURI(HttpServletRequest req)
	{
		return req.getRequestURL() == null ? "/" : req.getRequestURL().toString();
	}

	private String extractBearer(String auth)
	{
		if(auth == null)
			return null;
		String prefix = "Bearer ";
		return auth.startsWith(prefix) ? auth.substring(prefix.length()).trim() : null;
	}

	private String getCookie(HttpServletRequest req, String name)
	{
		if(req.getCookies() == null)
			return null;
		for(jakarta.servlet.http.Cookie c : req.getCookies())
		{
			if(name.equals(c.getName()))
				return c.getValue();
		}
		return null;
	}

	private void writeErrorJson(HttpServletResponse resp, int status, String code, String message, Map<String, String> data) throws IOException
	{
		resp.setStatus(status);
		resp.setContentType("application/json;charset=UTF-8");
		StringBuilder sb = new StringBuilder();
		sb.append('{')
			.append("\"error\":\"").append(escape(code)).append("\",")
			.append("\"message\":\"").append(escape(message)).append("\"");
		if(data != null && !data.isEmpty())
		{
			sb.append(",\"data\":{");
			boolean first = true;
			for(Map.Entry<String, String> e : data.entrySet())
			{
				if(!first)
					sb.append(',');
				first = false;
				sb.append("\"").append(escape(e.getKey())).append("\":\"").append(escape(e.getValue())).append("\"");
			}
			sb.append('}');
		}
		sb.append('}');
		try(PrintWriter out = resp.getWriter())
		{
			out.write(sb.toString());
		}
	}

	private String escape(String s)
	{
		return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}


