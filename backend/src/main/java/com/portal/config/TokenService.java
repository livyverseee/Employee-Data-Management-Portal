package com.portal.config;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Lightweight token utility.
 * 
 * Generates and validates tamper-proof signed tokens containing username, role, and timestamp.
 * Structure: Base64Url(username:role:timestamp:hmacSignature)
 * 
 * Why this approach?
 * - No complex Spring Security filter chains to debug.
 * - Simple for junior developers to understand and explain in an interview.
 * - Secure against tampering via HMAC-SHA256 signature verification.
 */
@Component
public class TokenService {

    private static final String SECRET_KEY = "EmployeePortalSecretKeyForTechnicalEvaluation2026";
    private static final long EXPIRATION_MILLIS = 24 * 60 * 60 * 1000L; // 24 hours

    /**
     * Generates a signed token for an authenticated user.
     *
     * @param username username of the user
     * @param role     user role (DEAN or EMPLOYEE)
     * @return Base64Url-encoded signed token
     */
    public String generateToken(String username, String role) {
        long timestamp = System.currentTimeMillis();
        String payload = username + ":" + role + ":" + timestamp;
        String signature = calculateHmac(payload);
        String fullToken = payload + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(fullToken.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates token integrity and expiration.
     *
     * @param token string from Authorization header
     * @return true if signature is valid and token is not expired
     */
    public boolean validateToken(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 4) {
                return false;
            }

            String username = parts[0];
            String role = parts[1];
            long timestamp = Long.parseLong(parts[2]);
            String providedSignature = parts[3];

            // Verify expiration
            if (System.currentTimeMillis() - timestamp > EXPIRATION_MILLIS) {
                return false;
            }

            // Verify signature
            String payload = username + ":" + role + ":" + timestamp;
            String expectedSignature = calculateHmac(payload);
            return MessageDigest.isEqual(providedSignature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts user role from the token payload.
     *
     * @param token valid token
     * @return role string (e.g. DEAN or EMPLOYEE)
     */
    public String extractRole(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return parts[1];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts username from the token payload.
     *
     * @param token valid token
     * @return username string
     */
    public String extractUsername(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return parts[0];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper to compute HMAC-SHA256 signature for payload string.
     */
    private String calculateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC signature", e);
        }
    }
}
