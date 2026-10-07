package com.portal.config;

import com.portal.model.User;
import com.portal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds initial demo accounts (DEAN and EMPLOYEE) into the database on first run if they don't already exist.
 * On subsequent runs, existing user accounts and passwords are strictly preserved without modification or re-seeding.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed default Dean user only if missing
        if (userRepository.findByUsername("dean").isEmpty() && userRepository.findByEmail("dean@portal.com").isEmpty()) {
            User dean = new User();
            dean.setFullName("Dean Administrator");
            dean.setUsername("dean");
            dean.setEmail("dean@portal.com");
            dean.setPassword(passwordEncoder.encode("dean123"));
            dean.setRole("DEAN");
            userRepository.save(dean);
            log.info("Initialized default Dean account (username: dean)");
        } else {
            log.info("Dean account already exists in database - preserving credentials.");
        }

        // Seed default Employee user only if missing
        if (userRepository.findByUsername("employee").isEmpty() && userRepository.findByEmail("employee@portal.com").isEmpty()) {
            User employee = new User();
            employee.setFullName("Staff Employee");
            employee.setUsername("employee");
            employee.setEmail("employee@portal.com");
            employee.setPassword(passwordEncoder.encode("emp123"));
            employee.setRole("EMPLOYEE");
            userRepository.save(employee);
            log.info("Initialized default Employee account (username: employee)");
        } else {
            log.info("Employee account already exists in database - preserving credentials.");
        }
    }
}
