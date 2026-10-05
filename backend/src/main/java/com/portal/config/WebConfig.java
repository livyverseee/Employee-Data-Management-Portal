package com.portal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc configuration that registers AuthInterceptor for protected dataset and employee endpoints.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Enforce token validation and role checks on all /api/dataset and /api/employees endpoints
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/dataset/**", "/api/employees/**");
    }
}
