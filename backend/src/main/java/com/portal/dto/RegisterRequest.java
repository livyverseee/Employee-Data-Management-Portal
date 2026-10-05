package com.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for portal registration requests.
 * 'accessCode' is mandatory for Dean registration.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String accessCode; // For Dean portal registration
}
