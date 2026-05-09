package com.smartload.controller;

import com.smartload.dto.LoginResponse;
import com.smartload.dto.LoginUserDto;
import com.smartload.dto.RegisterUserDto;
import com.smartload.dto.UserResponse;
import com.smartload.dto.VerifyUserDto;
import com.smartload.entity.User;
import com.smartload.service.AuthenticationService;
import com.smartload.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints — public (no JWT required).
 *
 * POST /api/auth/signup    register a new loadmaster account
 * POST /api/auth/login     authenticate and receive a JWT token
 * POST /api/auth/verify    verify email with code (Faz 6 wires real SMTP)
 * POST /api/auth/resend    resend verification code
 */
@RequestMapping("/api/auth")
@RestController
public class AuthenticationController {

    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    public AuthenticationController(
        JwtService jwtService,
        AuthenticationService authenticationService
    ) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserDto dto) {
        User user = authenticationService.signup(dto);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginUserDto dto) {
        User user = authenticationService.authenticate(dto);
        String token = jwtService.generateToken(user);
        LoginResponse response = new LoginResponse(
            token,
            jwtService.getExpirationTime(),
            UserResponse.from(user)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@Valid @RequestBody VerifyUserDto dto) {
        try {
            authenticationService.verifyUser(dto);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<String> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
