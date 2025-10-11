package com.server.framework.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.server.framework.common.AppProperties;
import com.server.framework.entity.UserEntity;
import com.server.framework.security.throttle.ThrottleNewHandler;
import com.server.framework.service.HttpLogService;
import com.server.framework.service.UserService;

@Component
public class SecurityFilter implements Filter
{
	private static final String IAM_TOKEN_COOKIE = "iam_token";
	private static final String LOGIN_PAGE = "/login";
	private static final String ROOT_PATH = "/";
	private static final String ZOHO_PATH = "/zoho";

	@Autowired
	private ApplicationContext applicationContext;

	static final ThreadLocal<UserEntity> CURRENT_USER_TL = new InheritableThreadLocal<>();
	static final ThreadLocal<HttpServletRequest> CURRENT_REQUEST_TL = new InheritableThreadLocal<>();
	static final ThreadLocal<ServletContext> SERVLET_CONTEXT_TL = new InheritableThreadLocal<>();
	static final ThreadLocal<Boolean> DISABLE_HTTP_LOG = new InheritableThreadLocal<>();

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
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		String oldThreadName = Thread.currentThread().getName();

		try
		{
			HttpServletResponse httpResponse = (HttpServletResponse) response;

			initializeThreadLocals(request);
			setResponseHeaders(httpResponse);

			if(SecurityUtil.isHealthCheckRequest())
			{
				chain.doFilter(request, response);
				return;
			}

			logRequest();

			if(!validateEndpoint(httpResponse))
				return;

			if(!handleRateLimit(httpResponse))
				return;

			UserEntity userEntity = authenticateUser();
			CURRENT_USER_TL.set(userEntity);

			setThreadName();

			if(handleEnvironmentRedirects(httpResponse))
				return;

			if(shouldSkipAuthentication())
			{
				handleSkipAuthentication(httpResponse, chain);
				return;
			}

			handleAuthentication(httpResponse, chain);
		}
		finally
		{
			cleanupThreadLocals(oldThreadName);
		}
	}

	private void initializeThreadLocals(ServletRequest request)
	{
		ServletContext servletContext = request.getServletContext();
		HttpServletRequest httpRequest = (HttpServletRequest) request;

		SERVLET_CONTEXT_TL.set(servletContext);
		CURRENT_REQUEST_TL.set(httpRequest);
	}

	private void setResponseHeaders(HttpServletResponse httpResponse)
	{
		httpResponse.setHeader("Server_Build_Label", AppProperties.getProperty("build.label", "dev"));
	}

	private void logRequest()
	{
		if(!SecurityUtil.isResourceFetchRequest())
		{
			LOGGER.log(Level.INFO, "Request {0} from {1}", new Object[] {SecurityUtil.getCurrentRequest().getRequestURI(), SecurityUtil.getOriginatingUserIP()});
		}
	}

	private boolean validateEndpoint(HttpServletResponse httpResponse) throws IOException
	{
		String requestURI = SecurityUtil.getCurrentRequest().getRequestURI();
		boolean isH2ConsoleRequest = AppProperties.getBoolean("spring.h2.console.enabled") && requestURI.matches(AppProperties.getProperty("spring.h2.console.path") + "(/.*)?");
		boolean isValidEndpoint = SecurityUtil.isValidEndPoint(resourceLoader) || SecurityUtil.isValidWebSocketEndPoint(applicationContext, requestURI) || isH2ConsoleRequest;

		if(!isValidEndpoint)
		{
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return false;
		}
		return true;
	}

	private boolean handleRateLimit(HttpServletResponse httpResponse) throws IOException
	{
		if(!SecurityUtil.isResourceFetchRequest() && !ThrottleNewHandler.tryAcquire(httpResponse))
		{
			httpResponse.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
			return false;
		}
		return true;
	}

	private boolean handleEnvironmentRedirects(HttpServletResponse httpResponse) throws IOException
	{
		String requestURI = SecurityUtil.getCurrentRequest().getRequestURI();
		if(AppProperties.getProperty("environment").equals("production") && requestURI.startsWith(ZOHO_PATH))
		{
			httpResponse.sendRedirect(ROOT_PATH);
			return true;
		}
		else if(!SecurityUtil.isLoginRequest() && !SecurityUtil.isAdminCall() && !SecurityUtil.isAdminUser() && !SecurityUtil.isResourceFetchRequest() && !requestURI.startsWith("/api") && AppProperties.getProperty("environment", "development").equals("zoho") && !requestURI.startsWith(ZOHO_PATH))
		{
			httpResponse.sendError(HttpStatus.FORBIDDEN.value());
			return true;
		}
		return false;
	}

	private UserEntity authenticateUser()
	{
		String bearer = SecurityUtil.extractBearer();
		if(bearer != null)
		{
			return userService.findByToken(bearer).orElse(null);
		}
		else
		{
			String sessionId = SecurityUtil.getCookieValue(IAM_TOKEN_COOKIE);
			return sessionId != null ? userService.findBySession(sessionId).orElse(null) : null;
		}
	}

	private void setThreadName()
	{
		if(!SecurityUtil.isResourceFetchRequest() && SecurityUtil.isLoggedIn())
		{
			String currentUserName = SecurityUtil.getCurrentUser().getName();
			String currentUserId = SecurityUtil.getCurrentUser().getId().toString();
			String newThreadName = Thread.currentThread().getName() + "/" + currentUserName + "-" + currentUserId;
			Thread.currentThread().setName(newThreadName);
		}
	}

	private boolean shouldSkipAuthentication()
	{
		return AppProperties.getBooleanProperty("skip.authentication", false) ||
			SecurityUtil.canSkipAuthentication() || (!SecurityUtil.isAdminCall() && AppProperties.getProperty("environment").equals("zoho"));
	}

	private void handleSkipAuthentication(HttpServletResponse httpResponse, FilterChain chain) throws IOException, ServletException
	{
		if(SecurityUtil.getCurrentRequestURI().equals(LOGIN_PAGE) && SecurityUtil.isLoggedIn())
		{
			httpResponse.sendRedirect(ROOT_PATH);
			return;
		}

		logHttpRequest();
		chain.doFilter(SecurityUtil.getCurrentRequest(), httpResponse);
	}

	private void handleAuthentication(HttpServletResponse httpResponse, FilterChain chain) throws IOException, ServletException
	{
		if(SecurityUtil.isLoggedIn())
		{
			if(!SecurityUtil.isAdminUser() && SecurityUtil.isAdminCall())
			{
				httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			logHttpRequest();
			chain.doFilter(SecurityUtil.getCurrentRequest(), httpResponse);
		}
		else
		{
			handleUnauthenticatedRequest(httpResponse);
		}
	}

	private void handleUnauthenticatedRequest(HttpServletResponse httpResponse) throws IOException
	{
		HttpServletRequest httpRequest = SecurityUtil.getCurrentRequest();
		String requestURL = httpRequest.getRequestURL().toString();
		if(SecurityUtil.isRestApi())
		{
			httpResponse.sendError(HttpStatus.UNAUTHORIZED.value());
		}
		else
		{
			String loginPage = httpRequest.getContextPath() + LOGIN_PAGE + "?redirect_uri=" + URLEncoder.encode(requestURL, StandardCharsets.UTF_8);
			httpResponse.sendRedirect(loginPage);
		}
	}

	private void logHttpRequest()
	{
		if(!SecurityUtil.isResourceFetchRequest() && !SecurityUtil.getCurrentRequest().getRequestURI().equals("/api/v1/admin/db/execute"))
		{
			try
			{
				httpLogService.logIncoming(SecurityUtil.getCurrentRequest());
			}
			catch(Exception ignore)
			{
			}
		}
	}

	private void cleanupThreadLocals(String oldThreadName)
	{
		CURRENT_USER_TL.remove();
		SERVLET_CONTEXT_TL.remove();
		CURRENT_REQUEST_TL.remove();
		DISABLE_HTTP_LOG.remove();
		Thread.currentThread().setName(oldThreadName);
	}
}


