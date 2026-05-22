package com.divinecorner.service;

import com.divinecorner.dto.*;
import com.divinecorner.dto.LoginRequest;
import com.divinecorner.dto.RegisterRequest;
import com.divinecorner.dto.SendOtpRequest;
import com.divinecorner.dto.response.AuthResponse;
import com.divinecorner.dto.response.UserResponse;
import com.divinecorner.entity.User;
import com.divinecorner.enums.UserRole;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.repository.UserRepository;
import com.divinecorner.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${jwt.cookie.name.access}")
    private String accessTokenCookie;

    @Value("${jwt.cookie.name.refresh}")
    private String refreshTokenCookie;

    @Value("${jwt.cookie.domain}")
    private String cookieDomain;

    @Value("${jwt.cookie.secure}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:None}")
    private String cookieSameSite;

    @Value("${jwt.expiration.access}")
    private Long accessExpiration;

    @Value("${jwt.expiration.refresh}")
    private Long refreshExpiration;

    public void sendOtp(SendOtpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        String otp = otpService.generateAndStore(request.getEmail());
        emailService.sendOtp(request.getEmail(), otp);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Mobile number already registered");
        }

        if (!otpService.verifyAndConsume(request.getEmail(), request.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP");
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

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        setAuthCookies(accessToken, refreshToken, response);

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

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        setAuthCookies(accessToken, refreshToken, response);

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

    @Transactional(readOnly = true)
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (refreshTokenCookie.equals(c.getName())) {
                    refreshToken = c.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null) {
            refreshToken = request.getHeader("X-Refresh-Token");
        }
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken, true)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
        Claims claims = jwtUtil.parseRefreshToken(refreshToken);
        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        String newAccess = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefresh = jwtUtil.generateRefreshToken(user.getId());
        setAuthCookies(newAccess, newRefresh, response);
        return AuthResponse.builder()
                .message("Token refreshed")
                .user(mapToUserResponse(user))
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .build();
    }

    private void setAuthCookies(String accessToken, String refreshToken, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookieString(accessTokenCookie, accessToken, accessExpiration.intValue() / 1000));
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookieString(refreshTokenCookie, refreshToken, refreshExpiration.intValue() / 1000));
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookieString(accessTokenCookie, "", 0));
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookieString(refreshTokenCookie, "", 0));
    }

    private String buildCookieString(String name, String value, int maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .domain(cookieDomain)
                .sameSite(cookieSameSite)
                .build()
                .toString();
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