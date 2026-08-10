package com.smarttennislab.auth.controller;

import com.smarttennislab.auth.dto.AuthResponse;
import com.smarttennislab.auth.dto.ChangePasswordRequest;
import com.smarttennislab.auth.dto.CoachResponse;
import com.smarttennislab.auth.dto.LoginRequest;
import com.smarttennislab.auth.dto.RefreshRequest;
import com.smarttennislab.auth.dto.RegisterRequest;
import com.smarttennislab.auth.dto.UpdateProfileRequest;
import com.smarttennislab.auth.model.CoachPrincipal;
import com.smarttennislab.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registro, login y renovación de sesión del profe")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Crear una cuenta de profe")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar el access token",
            description = "El refresh token usado se revoca y se devuelve uno nuevo.")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar la sesión de este dispositivo")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del profe logueado")
    public CoachResponse me(@AuthenticationPrincipal CoachPrincipal coach) {
        return authService.currentCoach(coach.id());
    }

    @PutMapping("/me")
    @Operation(summary = "Editar el nombre y el email del profe")
    public CoachResponse updateProfile(
            @AuthenticationPrincipal CoachPrincipal coach,
            @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(coach.id(), request);
    }

    @PostMapping("/me/password")
    @Operation(
            summary = "Cambiar la contraseña",
            description = "Cierra la sesión en los demás dispositivos y devuelve tokens nuevos.")
    public AuthResponse changePassword(
            @AuthenticationPrincipal CoachPrincipal coach,
            @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(coach.id(), request);
    }
}
