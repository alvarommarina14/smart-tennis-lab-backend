package com.smarttennislab.auth.service;

import com.smarttennislab.auth.dto.AuthResponse;
import com.smarttennislab.auth.dto.ChangePasswordRequest;
import com.smarttennislab.auth.dto.CoachResponse;
import com.smarttennislab.auth.dto.LoginRequest;
import com.smarttennislab.auth.dto.RegisterRequest;
import com.smarttennislab.auth.dto.UpdateProfileRequest;
import com.smarttennislab.auth.model.RefreshToken;
import com.smarttennislab.auth.model.User;
import com.smarttennislab.auth.repository.RefreshTokenRepository;
import com.smarttennislab.auth.repository.UserRepository;
import com.smarttennislab.shared.ApiException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Ya hay una cuenta con ese email");
        }

        User user = userRepository.save(
                new User(email, passwordEncoder.encode(request.password()), request.fullName().trim()));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCase(normalize(request.email()))
                .orElseThrow(AuthService::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issueTokens(user);
    }

    // Rotación: el refresh usado se revoca y se entrega uno nuevo. Si alguien reusa uno viejo,
    // ya no sirve.
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(jwtService.hashRefreshToken(rawRefreshToken))
                .orElseThrow(() -> ApiException.unauthorized("La sesión expiró, entrá de nuevo"));

        if (!stored.isUsable()) {
            throw ApiException.unauthorized("La sesión expiró, entrá de nuevo");
        }

        stored.revoke();
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository
                .findByTokenHash(jwtService.hashRefreshToken(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public CoachResponse updateProfile(UUID coachId, UpdateProfileRequest request) {
        User user = require(coachId);
        String email = normalize(request.email());

        if (!email.equals(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Ya hay una cuenta con ese email");
        }

        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        return CoachResponse.from(user);
    }

    // Cambiar la contraseña cierra la sesión en todos los dispositivos y devuelve tokens nuevos
    // para el que la cambió: si alguien te robó la cuenta, se queda afuera.
    @Transactional
    public AuthResponse changePassword(UUID coachId, ChangePasswordRequest request) {
        User user = require(coachId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("La contraseña actual no coincide");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(coachId, Instant.now());
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public CoachResponse currentCoach(UUID coachId) {
        return userRepository
                .findById(coachId)
                .map(CoachResponse::from)
                .orElseThrow(() -> ApiException.unauthorized("La sesión ya no es válida"));
    }

    private User require(UUID coachId) {
        return userRepository
                .findById(coachId)
                .orElseThrow(() -> ApiException.unauthorized("La sesión ya no es válida"));
    }

    private AuthResponse issueTokens(User user) {
        String rawRefreshToken = jwtService.generateRefreshToken();
        refreshTokenRepository.save(new RefreshToken(
                user, jwtService.hashRefreshToken(rawRefreshToken), jwtService.refreshTokenExpiry()));

        return new AuthResponse(
                jwtService.generateAccessToken(user),
                rawRefreshToken,
                jwtService.accessTokenTtlSeconds(),
                CoachResponse.from(user));
    }

    // Mismo mensaje si el email no existe o si la contraseña está mal: no se le confirma a nadie
    // qué emails hay registrados.
    private static ApiException invalidCredentials() {
        return ApiException.unauthorized("Email o contraseña incorrectos");
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void revokeAllSessions(UUID coachId) {
        refreshTokenRepository.revokeAllForUser(coachId, Instant.now());
    }
}
