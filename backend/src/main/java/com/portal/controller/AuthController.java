package com.portal.controller;

import com.portal.dto.LoginRequest;
import com.portal.dto.LoginResponse;
import com.portal.dto.RegisterRequest;
import com.portal.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for portal-specific authentication and registration.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Dean Portal Login.
     */
    @PostMapping("/dean/login")
    public ResponseEntity<LoginResponse> deanLogin(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login("DEAN", request);
        return ResponseEntity.ok(response);
    }

    /**
     * Employee Portal Login.
     */
    @PostMapping("/employee/login")
    public ResponseEntity<LoginResponse> employeeLogin(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login("EMPLOYEE", request);
        return ResponseEntity.ok(response);
    }

    /**
     * Dean Portal Registration (requires valid dean access code).
     */
    @PostMapping("/dean/register")
    public ResponseEntity<Map<String, String>> deanRegister(@RequestBody RegisterRequest request) {
        authService.registerDean(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful"));
    }

    /**
     * Employee Portal Registration.
     */
    @PostMapping("/employee/register")
    public ResponseEntity<Map<String, String>> employeeRegister(@RequestBody RegisterRequest request) {
        authService.registerEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful"));
    }
}
