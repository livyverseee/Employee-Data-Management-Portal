package com.portal.service;

import com.portal.config.TokenService;
import com.portal.dto.LoginRequest;
import com.portal.dto.LoginResponse;
import com.portal.dto.RegisterRequest;
import com.portal.exception.BadRequestException;
import com.portal.exception.ConflictException;
import com.portal.exception.ForbiddenException;
import com.portal.exception.UnauthorizedException;
import com.portal.model.User;
import com.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Service managing user authentication, portal role verification, and registration.
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Value("${app.dean.access-code:DEAN2026}")
    private String deanAccessCode;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Authenticates a user into a specific portal (DEAN or EMPLOYEE).
     *
     * @param portalRole portal being accessed ("DEAN" or "EMPLOYEE")
     * @param request    login credentials (identifier can be username or email)
     * @return LoginResponse with token, username, fullName, role
     */
    public LoginResponse login(String portalRole, LoginRequest request) {
        if (request == null || request.getIdentifier() == null || request.getIdentifier().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new UnauthorizedException("Username/email and password must not be empty");
        }

        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        // Validate password using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        // Validate that user belongs to the requested portal
        if (!user.getRole().equalsIgnoreCase(portalRole)) {
            String portalName = "DEAN".equalsIgnoreCase(portalRole) ? "Dean Portal" : "Employee Portal";
            throw new UnauthorizedException("These credentials do not belong to the " + portalName);
        }

        // Generate signed token
        String token = tokenService.generateToken(user.getUsername(), user.getRole());

        return new LoginResponse(token, user.getUsername(), user.getFullName(), user.getRole());
    }

    /**
     * Registers a new Dean user. Requires valid dean accessCode.
     */
    @Transactional
    public void registerDean(RegisterRequest request) {
        validateRegistrationFields(request);

        // Verify Dean access code
        if (request.getAccessCode() == null || !request.getAccessCode().trim().equals(deanAccessCode)) {
            throw new ForbiddenException("Invalid dean access code");
        }

        saveNewUser(request, "DEAN");
    }

    /**
     * Registers a new Employee user.
     */
    @Transactional
    public void registerEmployee(RegisterRequest request) {
        validateRegistrationFields(request);
        saveNewUser(request, "EMPLOYEE");
    }

    private void validateRegistrationFields(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration request cannot be empty");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new BadRequestException("Full name is required");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Username is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new BadRequestException("Invalid email format");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters long");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
    }

    private void saveNewUser(RegisterRequest request, String role) {
        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        userRepository.save(user);
    }
}
