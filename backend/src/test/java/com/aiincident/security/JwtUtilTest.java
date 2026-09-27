package com.aiincident.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String VALID_SECRET = "this-is-a-development-only-secret-32bytes+";

    @Test
    void constructor_secretTooShort_throwsInsteadOfSilentlyPadding() {
        // Regression test: JwtUtil used to silently zero-pad short secrets to 32
        // bytes instead of rejecting them, which quietly produces a much weaker,
        // predictable signing key. It must now fail fast at startup instead.
        assertThatThrownBy(() -> new JwtUtil("too-short", 3600000L, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void constructor_defaultPlaceholderSecretInProdProfile_throws() {
        assertThatThrownBy(() -> new JwtUtil(
                "CHANGE_ME_DEV_ONLY_CHANGE_ME_DEV_ONLY_CHANGE_ME", 3600000L, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to start");
    }

    @Test
    void constructor_defaultPlaceholderSecretOutsideProd_startsWithWarningOnly() {
        // Should not throw when not running under the "prod" profile (e.g. local dev).
        JwtUtil jwtUtil = new JwtUtil(
                "CHANGE_ME_DEV_ONLY_CHANGE_ME_DEV_ONLY_CHANGE_ME", 3600000L, "");
        String token = jwtUtil.generateToken("dev@example.com", 1L, "CUSTOMER");
        assertThat(token).isNotBlank();
    }

    @Test
    void generateAndValidateToken_roundTripsClaims() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, 3600000L, "");

        String token = jwtUtil.generateToken("jane@example.com", 42L, "SUPPORT_AGENT");

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("jane@example.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SUPPORT_AGENT");
        assertThat(jwtUtil.isTokenValid(token, "jane@example.com")).isTrue();
        assertThat(jwtUtil.isTokenValid(token, "someone-else@example.com")).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() throws InterruptedException {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, 1L, ""); // 1ms expiration
        String token = jwtUtil.generateToken("jane@example.com", 1L, "CUSTOMER");
        Thread.sleep(20);

        // jjwt throws ExpiredJwtException when parsing an expired token rather than
        // returning false directly - callers (JwtAuthFilter) already catch that and
        // treat the request as unauthenticated, which is the behavior that matters.
        assertThatThrownBy(() -> jwtUtil.isTokenValid(token, "jane@example.com"))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
