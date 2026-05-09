package com.smartload.dto;

import com.smartload.entity.User;

/**
 * Frontend-facing representation of a User.
 * Excludes sensitive fields (password hash, verification code, expiration).
 *
 * Always return UserResponse from controllers, never the User entity directly.
 */
public record UserResponse(
    Long id,
    String username,
    String email,
    boolean enabled
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.isEnabled()
        );
    }
}
