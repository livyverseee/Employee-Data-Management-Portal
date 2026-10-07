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
 * Spring MVC HandlerInterceptor that intercepts protected endpoints under
 * /api/dataset/**, /api/datasets/**, and /api/employees/**.
 * 
 * Rules:
 * 1. Skips preflight OPTIONS requests.
 * 2. Validates Bearer token presence and HMAC cryptographic integrity.
 * 3. Enforces Role-Based Access Control (RBAC):
 *    - Upload & Replace datasets -> DEAN only
 *    - Delete / Edit records -> DEAN only
 *    - View records, filter options, active status, schema, Excel export -> DEAN and EMPLOYEE allowed
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
        // 1. Allow CORS preflight requests
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

        // 3. Validate token presence & integrity
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

        // 5. Role restrictions:
        // DEAN-only operations:
        // - Upload / Replace dataset
        // - Download dataset XML (/api/dataset/xml)
        // - Add / Edit / Delete records (POST, PUT, DELETE)
        boolean isDeanOnly = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || uri.contains("/upload")
                || uri.contains("/replace")
                || uri.contains("/xml");

        if (isDeanOnly) {
            if (!"DEAN".equalsIgnoreCase(role)) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN, "You don't have permission");
                return false;
            }
        } else {
            // General read endpoints allow both DEAN and EMPLOYEE
            if (!"DEAN".equalsIgnoreCase(role) && !"EMPLOYEE".equalsIgnoreCase(role)) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN, "You don't have permission");
                return false;
            }
        }

        return true;
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), status.value(), message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
