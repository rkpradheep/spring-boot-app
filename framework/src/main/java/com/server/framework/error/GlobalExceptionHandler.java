package com.server.framework.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.server.framework.builder.ApiResponseBuilder;
import com.server.framework.common.AppProperties;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

    private boolean isRestApi(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/api/");
    }

    private String renderErrorPage(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message) {
        try {
            response.setStatus(status.value());
            response.setContentType("text/html; charset=UTF-8");
            try (InputStream is = request.getServletContext().getResourceAsStream("/static/errorPage.html")) {
                if (is == null) return null;
                String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                html = html.replace("${STATUS}", status.value() + " " + status.getReasonPhrase())
                        .replace("${MESSAGE}", escape(message))
                        .replace("${PATH}", escape(request.getRequestURI()))
                        .replace("${BUILD_LABEL}", escape(AppProperties.getProperty("build.label", "dev")))
                        .replace("${TIMESTAMP}", String.valueOf(System.currentTimeMillis()));
                return html;
            }
        } catch (Exception ignore) {
            return null;
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("<", "&lt;").replace(">", "&gt;");
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.log(Level.SEVERE, "Unhandled exception for request: " + request.getRequestURI(), ex);
        if (!isRestApi(request)) {
            String html = renderErrorPage(request, response, HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred");
            if (html != null) return html;
            try {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().print("<html><body><h1>500 Internal Server Error</h1><p>An internal server error occurred</p></body></html>");
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to write error response", e);
                return null;
            }
        }

        Map<String, Object> errorResponse = ApiResponseBuilder.create()
            .errorCode("internal_error")
            .message(ex.getMessage())
            .success(false)
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(AppException.class)
    public Object handleAppException(AppException ex, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.log(Level.SEVERE, "Application exception for request: " + request.getRequestURI(), ex);

        if (!isRestApi(request)) {
            HttpStatus status = determineHttpStatus(ex.getErrorCode());
            String html = renderErrorPage(request, response, status, ex.getMessage());
            if (html != null) return html;
            try {
                response.setStatus(status.value());
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().print("<html><body><h1>" + status.value() + " " + status.getReasonPhrase() + "</h1><p>" + ex.getMessage() + "</p></body></html>");
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to write error response", e);
                return null;
            }
        }
        Map<String, Object> errorResponse = ApiResponseBuilder.create()
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .data(ex.getData())
            .success(false)
            .build();
        return ResponseEntity.status(determineHttpStatus(ex.getErrorCode())).body(errorResponse);
    }


    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        switch (errorCode.toLowerCase()) {
            case "reauth_required":
            case "unauthorized":
                return HttpStatus.UNAUTHORIZED;
            case "forbidden":
            case "access_denied":
                return HttpStatus.FORBIDDEN;
            case "not_found":
                return HttpStatus.NOT_FOUND;
            case "bad_request":
            case "invalid_input":
            case "key_expired":
                return HttpStatus.BAD_REQUEST;
            case "rate_limit_exceeded":
                return HttpStatus.TOO_MANY_REQUESTS;
            default:
                return HttpStatus.BAD_REQUEST;
        }
    }
}
