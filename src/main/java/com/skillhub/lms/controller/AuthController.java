package com.skillhub.lms.controller;

import com.skillhub.lms.dto.*;
import com.skillhub.lms.entity.User;
import com.skillhub.lms.repository.UserRepository;
import com.skillhub.lms.security.JwtService;
import com.skillhub.lms.security.UserPrincipal;
import com.skillhub.lms.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .username(request.getEmail())
                .phone(request.getPhone() != null ? request.getPhone() : "")
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.STUDENT)
                .build();
        user = userRepository.save(user);
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = refreshTokenService.createAndSave(user);
        setRefreshCookie(response, refreshToken, (int) (jwtService.getRefreshTokenExpirationMs() / 1000));
        return AuthResponse.builder()
                .message("Registered")
                .accessToken(accessToken)
                .user(UserResponse.from(user))
                .build();
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        return register(request, response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = refreshTokenService.createAndSave(user);
        setRefreshCookie(response, refreshToken, (int) (jwtService.getRefreshTokenExpirationMs() / 1000));
        return AuthResponse.builder()
                .message("Login successful")
                .accessToken(accessToken)
                .user(UserResponse.from(user))
                .build();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefresh = getRefreshTokenFromCookie(request);
        if (rawRefresh == null || rawRefresh.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token missing");
        }
        return refreshTokenService.validate(rawRefresh)
                .map(rt -> {
                    User user = rt.getUser();
                    String newAccess = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
                    return AuthResponse.builder()
                            .message("Refreshed")
                            .accessToken(newAccess)
                            .user(UserResponse.from(user))
                            .build();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefresh = getRefreshTokenFromCookie(request);
        if (rawRefresh != null && !rawRefresh.isBlank()) {
            refreshTokenService.revokeByToken(rawRefresh);
        }
        clearRefreshCookie(response);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserResponse.from(user);
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(jwtService.getRefreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtService.getRefreshCookieName(), "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (jwtService.getRefreshCookieName().equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
