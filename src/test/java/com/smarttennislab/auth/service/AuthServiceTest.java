package com.smarttennislab.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final String SECRET = "secreto-de-test-que-tiene-mas-de-32-bytes-de-largo";

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService =
            new JwtService(SECRET, Duration.ofMinutes(15), Duration.ofDays(30));

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User usuarioExistente(String email, String password) {
        User user = new User(email, passwordEncoder.encode(password), "Profe Test");
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void registrarDevuelveLosDosTokensYLosDatosDelProfe() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        AuthResponse response =
                service.register(new RegisterRequest("profe@test.com", "contraseña123", "Profe Test"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresInSeconds()).isEqualTo(900);
        assertThat(response.coach().fullName()).isEqualTo("Profe Test");
    }

    @Test
    void registrarGuardaElEmailEnMinusculasYLaContraseñaHasheada() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        service.register(new RegisterRequest("  Profe@TEST.com  ", "contraseña123", "Profe Test"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User guardado = captor.getValue();

        assertThat(guardado.getEmail()).isEqualTo("profe@test.com");
        assertThat(guardado.getPasswordHash()).isNotEqualTo("contraseña123");
        assertThat(passwordEncoder.matches("contraseña123", guardado.getPasswordHash())).isTrue();
    }

    @Test
    void noSePuedeRegistrarDosVecesElMismoEmail() {
        when(userRepository.existsByEmailIgnoreCase("profe@test.com")).thenReturn(true);

        assertThatThrownBy(
                        () -> service.register(new RegisterRequest("profe@test.com", "contraseña123", "Profe")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginCorrectoDevuelveTokens() {
        when(userRepository.findByEmailIgnoreCase("profe@test.com"))
                .thenReturn(Optional.of(usuarioExistente("profe@test.com", "contraseña123")));

        AuthResponse response = service.login(new LoginRequest("profe@test.com", "contraseña123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void loginConContraseñaIncorrectaFalla() {
        when(userRepository.findByEmailIgnoreCase("profe@test.com"))
                .thenReturn(Optional.of(usuarioExistente("profe@test.com", "contraseña123")));

        assertThatThrownBy(() -> service.login(new LoginRequest("profe@test.com", "otra-cosa")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginConEmailInexistenteDaElMismoErrorQueLaContraseñaMal() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nadie@test.com", "contraseña123")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void refrescarRevocaElTokenViejoYEntregaUnoNuevo() {
        User user = usuarioExistente("profe@test.com", "contraseña123");
        String raw = jwtService.generateRefreshToken();
        RefreshToken guardado =
                new RefreshToken(user, jwtService.hashRefreshToken(raw), Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(raw)))
                .thenReturn(Optional.of(guardado));

        AuthResponse response = service.refresh(raw);

        assertThat(guardado.getRevokedAt()).isNotNull();
        assertThat(response.refreshToken()).isNotEqualTo(raw);
    }

    @Test
    void unRefreshYaUsadoNoSirveDeNuevo() {
        User user = usuarioExistente("profe@test.com", "contraseña123");
        String raw = jwtService.generateRefreshToken();
        RefreshToken revocado =
                new RefreshToken(user, jwtService.hashRefreshToken(raw), Instant.now().plusSeconds(3600));
        revocado.revoke();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revocado));

        assertThatThrownBy(() -> service.refresh(raw))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unRefreshVencidoNoSirve() {
        User user = usuarioExistente("profe@test.com", "contraseña123");
        String raw = jwtService.generateRefreshToken();
        RefreshToken vencido =
                new RefreshToken(user, jwtService.hashRefreshToken(raw), Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(vencido));

        assertThatThrownBy(() -> service.refresh(raw)).isInstanceOf(ApiException.class);
    }

    @Test
    void unRefreshDesconocidoNoSirve() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("inventado")).isInstanceOf(ApiException.class);
    }

    @Test
    void cerrarSesionRevocaElRefreshRecibido() {
        User user = usuarioExistente("profe@test.com", "contraseña123");
        String raw = jwtService.generateRefreshToken();
        RefreshToken guardado =
                new RefreshToken(user, jwtService.hashRefreshToken(raw), Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(guardado));

        service.logout(raw);

        assertThat(guardado.getRevokedAt()).isNotNull();
    }

    @Test
    void cerrarSesionConUnTokenDesconocidoNoRompe() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        service.logout("inventado");
    }

    @Test
    void editarElPerfilCambiaNombreYEmailNormalizado() {
        User user = usuarioExistente("profe@test.com", "contraseña123");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        CoachResponse response = service.updateProfile(
                user.getId(), new UpdateProfileRequest("  Nuevo@TEST.com  ", "  Alvi Marina  "));

        assertThat(response.email()).isEqualTo("nuevo@test.com");
        assertThat(response.fullName()).isEqualTo("Alvi Marina");
    }

    @Test
    void editarElPerfilDejandoElMismoEmailNoChocaConsigoMismo() {
        User user = usuarioExistente("profe@test.com", "contraseña123");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("profe@test.com")).thenReturn(true);

        CoachResponse response = service.updateProfile(
                user.getId(), new UpdateProfileRequest("profe@test.com", "Otro Nombre"));

        assertThat(response.fullName()).isEqualTo("Otro Nombre");
    }

    @Test
    void noSePuedeTomarElEmailDeOtroProfe() {
        User user = usuarioExistente("profe@test.com", "contraseña123");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("ocupado@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.updateProfile(
                        user.getId(), new UpdateProfileRequest("ocupado@test.com", "Profe")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(user.getEmail()).isEqualTo("profe@test.com");
    }

    @Test
    void cambiarLaContraseñaGuardaElHashNuevoYCierraLasDemasSesiones() {
        User user = usuarioExistente("profe@test.com", "contraseña123");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AuthResponse response = service.changePassword(
                user.getId(), new ChangePasswordRequest("contraseña123", "contraseña-nueva"));

        assertThat(passwordEncoder.matches("contraseña-nueva", user.getPasswordHash())).isTrue();
        assertThat(response.accessToken()).isNotBlank();
        verify(refreshTokenRepository).revokeAllForUser(eq(user.getId()), any(Instant.class));
    }

    @Test
    void cambiarLaContraseñaConLaActualMalNoLaToca() {
        User user = usuarioExistente("profe@test.com", "contraseña123");
        String hashOriginal = user.getPasswordHash();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(
                        user.getId(), new ChangePasswordRequest("me-la-olvide", "contraseña-nueva")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(user.getPasswordHash()).isEqualTo(hashOriginal);
        verify(refreshTokenRepository, never()).revokeAllForUser(any(UUID.class), any(Instant.class));
    }
}
