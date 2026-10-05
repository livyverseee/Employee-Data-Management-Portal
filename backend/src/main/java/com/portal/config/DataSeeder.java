package com.portal.config;

import com.portal.model.User;
import com.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds initial demo accounts (DEAN and EMPLOYEE) into the database on application startup if they don't already exist.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed default Dean user if missing
        if (userRepository.findByUsername("dean").isEmpty()) {
            User dean = new User();
            dean.setFullName("Dean Administrator");
            dean.setUsername("dean");
            dean.setEmail("dean@portal.com");
            dean.setPassword(passwordEncoder.encode("dean123"));
            dean.setRole("DEAN");
            userRepository.save(dean);
        }

        // Seed default Employee user if missing
        if (userRepository.findByUsername("employee").isEmpty()) {
            User employee = new User();
            employee.setFullName("Staff Employee");
            employee.setUsername("employee");
            employee.setEmail("employee@portal.com");
            employee.setPassword(passwordEncoder.encode("emp123"));
            employee.setRole("EMPLOYEE");
            userRepository.save(employee);
        }
    }
}
