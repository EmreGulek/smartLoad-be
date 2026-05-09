package com.smartload.dto;

/**
 * Login endpoint response.
 * Returned by POST /api/auth/login after successful authentication.
 */
public record LoginResponse(
    String token,
    long expiresIn,
    UserResponse user
) {}
