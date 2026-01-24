package com.divinecorner.service;

import com.divinecorner.dto.*;
import com.divinecorner.dto.LoginRequest;
import com.divinecorner.dto.RegisterRequest;
import com.divinecorner.dto.response.AuthResponse;
import com.divinecorner.dto.response.UserResponse;
import com.divinecorner.entity.User;
import com.divinecorner.enums.UserRole;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.repository.UserRepository;
import com.divinecorner.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.cookie.name.access}")
    private String accessTokenCookie;

    @Value("${jwt.cookie.name.refresh}")
    private String refreshTokenCookie;

    @Value("${jwt.cookie.domain}")
    private String cookieDomain;

    @Value("${jwt.cookie.secure}")
    private boolean cookieSecure;

    @Value("${jwt.expiration.access}")
    private Long accessExpiration;

    @Value("${jwt.expiration.refresh}")
    private Long refreshExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(UserRole.USER)
                .active(true)
                .build();

        user = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Set cookies (for browsers)
        setAuthCookies(accessToken, refreshToken, response);

        // Return tokens in response body as well
        return AuthResponse.builder()
                .message("Registration successful")
                .user(mapToUserResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (!user.getActive()) {
            throw new BadRequestException("Account is deactivated");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Set cookies (for browsers)
        setAuthCookies(accessToken, refreshToken, response);

        // Return tokens in response body as well
        return AuthResponse.builder()
                .message("Login successful")
                .user(mapToUserResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    public void logout(HttpServletResponse response) {
        clearAuthCookies(response);
    }

    private void setAuthCookies(String accessToken, String refreshToken, HttpServletResponse response) {
        Cookie accessCookie = createCookie(accessTokenCookie, accessToken, accessExpiration.intValue() / 1000);
        Cookie refreshCookie = createCookie(refreshTokenCookie, refreshToken, refreshExpiration.intValue() / 1000);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = createCookie(accessTokenCookie, "", 0);
        Cookie refreshCookie = createCookie(refreshTokenCookie, "", 0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    private Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setDomain(cookieDomain);
        return cookie;
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .zipCode(user.getZipCode())
                .country(user.getCountry())
                .role(user.getRole().name())
                .active(user.getActive())
                .build();
    }
}