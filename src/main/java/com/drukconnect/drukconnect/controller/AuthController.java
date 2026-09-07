package com.drukconnect.drukconnect.controller;

import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.*;
import com.drukconnect.drukconnect.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        return authService.signup(request, RequestMetadata.from(httpRequest));
    }

    @PostMapping("/verify-otp")
    public VerifyOtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {
        return authService.verifyOtp(request, RequestMetadata.from(httpRequest));
    }

    @PostMapping("/resend-otp")
    public MessageResponse resendOtp(@Valid @RequestBody ResendOtpRequest request, HttpServletRequest httpRequest) {
        return authService.resendOtp(request, RequestMetadata.from(httpRequest));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, RequestMetadata.from(httpRequest));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request, RequestMetadata.from(httpRequest));
    }

    @PostMapping("/logout")
    public MessageResponse logout(@AuthenticationPrincipal Jwt jwt, HttpServletRequest httpRequest) {
        return authService.logout(jwt, RequestMetadata.from(httpRequest));
    }
}
