package com.smarttennislab.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttennislab.auth.model.CoachPrincipal;
import com.smarttennislab.auth.model.User;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "secreto-de-test-que-tiene-mas-de-32-bytes-de-largo";
    private static final String OTRO_SECRET = "otro-secreto-distinto-igual-de-largo-para-firmar!!";

    private final JwtService service =
            new JwtService(SECRET, Duration.ofMinutes(15), Duration.ofDays(30));

    private User coach() {
        User user = new User("profe@test.com", "hash", "Profe Test");
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void elAccessTokenViajaConElIdYElEmailDelProfe() {
        User user = coach();

        CoachPrincipal principal =
                service.parseAccessToken(service.generateAccessToken(user)).orElseThrow();

        assertThat(principal.id()).isEqualTo(user.getId());
        assertThat(principal.email()).isEqualTo("profe@test.com");
    }

    @Test
    void unTokenFirmadoConOtraClaveNoSeAcepta() {
        JwtService otroServicio =
                new JwtService(OTRO_SECRET, Duration.ofMinutes(15), Duration.ofDays(30));

        String tokenAjeno = otroServicio.generateAccessToken(coach());

        assertThat(service.parseAccessToken(tokenAjeno)).isEmpty();
    }

    @Test
    void unTokenVencidoNoSeAcepta() {
        JwtService vencido =
                new JwtService(SECRET, Duration.ofSeconds(-30), Duration.ofDays(30));

        String token = vencido.generateAccessToken(coach());

        assertThat(service.parseAccessToken(token)).isEmpty();
    }

    @Test
    void unTokenBasuraNoRompeNada() {
        assertThat(service.parseAccessToken("esto-no-es-un-jwt")).isEmpty();
        assertThat(service.parseAccessToken("")).isEmpty();
    }

    @Test
    void cadaRefreshTokenEsDistintoPeroSuHashEsEstable() {
        String uno = service.generateRefreshToken();
        String otro = service.generateRefreshToken();

        assertThat(uno).isNotEqualTo(otro);
        assertThat(service.hashRefreshToken(uno)).isEqualTo(service.hashRefreshToken(uno));
        assertThat(service.hashRefreshToken(uno)).isNotEqualTo(service.hashRefreshToken(otro));
    }

    @Test
    void elHashDelRefreshNoDejaVerElTokenOriginal() {
        String raw = service.generateRefreshToken();

        String hash = service.hashRefreshToken(raw);

        assertThat(hash).hasSize(64).doesNotContain(raw);
    }

    @Test
    void unSecretCortoNoDejaArrancarLaApp() {
        assertThatThrownBy(() -> new JwtService("corto", Duration.ofMinutes(15), Duration.ofDays(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
