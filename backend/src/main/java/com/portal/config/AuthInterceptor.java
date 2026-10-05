package com.portal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portal.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Spring MVC HandlerInterceptor that intercepts all /api/employees/** requests.
 * 
 * Flow:
 * 1. Allows HTTP OPTIONS requests (CORS preflight).
 * 2. Extracts and validates the lightweight token from Authorization header.
 * 3. Enforces Role-Based Access Control (RBAC):
 *    - POST /api/employees/upload -> DEAN only (403 if EMPLOYEE).
 *    - DELETE /api/employees/*    -> DEAN only (403 if EMPLOYEE).
 *    - GET /api/employees/**      -> DEAN and EMPLOYEE allowed.
 * 4. Returns structured JSON error responses on 401 / 403.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    private final ObjectMapper objectMapper;

    public AuthInterceptor() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. Let CORS preflight requests through without checking token
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. Extract Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            if (authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim();
            } else {
                token = authHeader.trim();
            }
        }

        // 3. Validate token presence & cryptographic integrity
        if (token == null || !tokenService.validateToken(token)) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token");
            return false;
        }

        // 4. Extract role and username
        String role = tokenService.extractRole(token);
        String username = tokenService.extractUsername(token);
        request.setAttribute("currentUser", username);
        request.setAttribute("currentRole", role);

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 5. Role checks:
        // DEAN-only operations:
        // - POST /api/employees/upload
        // - DELETE /api/employees/{id}
        boolean isUpload = "POST".equalsIgnoreCase(method) && uri.endsWith("/upload");
        boolean isDelete = "DELETE".equalsIgnoreCase(method);

        if (isUpload || isDelete) {
            if (!"DEAN".equalsIgnoreCase(role)) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN, "You don't have permission");
                return false;
            }
        } else {
            // General employee endpoints (GET list, GET by id, GET export) allow DEAN and EMPLOYEE
            if (!"DEAN".equalsIgnoreCase(role) && !"EMPLOYEE".equalsIgnoreCase(role)) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN, "You don't have permission");
                return false;
            }
        }

        return true;
    }

    /**
     * Helper to write consistent JSON error responses directly to the HttpServletResponse.
     */
    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), status.value(), message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
