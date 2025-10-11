package com.server.framework.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.server.framework.builder.ApiResponseBuilder;
import com.server.framework.common.AppProperties;
import com.server.framework.security.SecurityUtil;

@Controller
public class AppErrorController implements ErrorController
{

	@RequestMapping("/error")
	@ResponseBody
	public Object handleError(HttpServletRequest request, HttpServletResponse response)
	{
		Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		String path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		HttpStatus status = statusCode != null ? resolveStatus(statusCode) : HttpStatus.INTERNAL_SERVER_ERROR;

		boolean isRest = path != null && path.startsWith("/api/");
		boolean wantsJson = acceptsJson(request);

		if(isRest || wantsJson)
		{
			String errorCode = status == HttpStatus.UNAUTHORIZED ? "authentication_needed" : null;
			Map<String, Object> body = ApiResponseBuilder.error(defaultMessage(status), errorCode, status.value());
			return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
		}

		int code = response.getStatus();

		response.setStatus(status.value());
		response.setContentType("text/html; charset=UTF-8");

		try(InputStream is = new ClassPathResource("static/errorPage.html").getInputStream())
		{
			String errorHtml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			errorHtml = errorHtml.replace("${code}", code + "");
			errorHtml = errorHtml.replace("${message}", getErrorMessage(request, code, response));
			errorHtml = errorHtml.replace("${canShowHomePage}", code == 404 && !AppProperties.getProperty("environment").equals("zoho") ? "inline-block" : "none");
			return errorHtml;
		}
		catch(Exception e)
		{
			return ("<html><body><h3>" + status.value() + " " + status.getReasonPhrase() + "</h3></body></html>");
		}
	}

	private boolean acceptsJson(HttpServletRequest request)
	{
		String accept = request.getHeader("Accept");
		if(accept == null)
			return false;
		accept = accept.toLowerCase(Locale.ROOT);
		return accept.contains("application/json") || accept.contains("application/problem+");
	}

	private HttpStatus resolveStatus(int code)
	{
		try
		{
			return HttpStatus.valueOf(code);
		}
		catch(Exception ignore)
		{
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
	}

	private String defaultMessage(HttpStatus status)
	{
		return switch(status)
		{
			case NOT_FOUND -> "The requested resource was not found";
			case FORBIDDEN -> "Access denied";
			case UNAUTHORIZED -> "Authentication required";
			default -> "An error occurred";
		};
	}

	private static String getErrorMessage(HttpServletRequest request, int code, HttpServletResponse response)
	{
		return switch(code)
		{
			case org.apache.http.HttpStatus.SC_NOT_FOUND -> SecurityUtil.isRestApi(request) ? "The requested resource [$1] is not available".replace("$1", request.getRequestURI()) : "Page not found";
			case org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS ->
				SecurityUtil.isRestApi(request) ? "Throttle limit exceeded" : "Throttle limit exceeded. Please try again " + (Objects.nonNull(response.getHeader("Retry-After")) ? "after " + response.getHeader("Retry-After") : "after sometime");
			case org.apache.http.HttpStatus.SC_FORBIDDEN -> "You are not authorized to access this page";
			default -> "Something went wrong. Please try again later";
		};
	}

}
