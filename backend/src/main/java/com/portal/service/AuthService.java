package com.portal.service;

import com.portal.config.TokenService;
import com.portal.dto.LoginRequest;
import com.portal.dto.LoginResponse;
import com.portal.exception.UnauthorizedException;
import com.portal.model.User;
import com.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service managing user authentication and token creation.
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    /**
     * Authenticates user credentials and generates a signed token carrying user role.
     *
     * @param request login payload containing username and password
     * @return LoginResponse with token, username, and role
     */
    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new UnauthorizedException("Username and password must not be empty");
        }

        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        // Validate password using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        // Generate signed token
        String token = tokenService.generateToken(user.getUsername(), user.getRole());

        return new LoginResponse(token, user.getUsername(), user.getRole());
    }
}
