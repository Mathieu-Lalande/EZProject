package com.ezproject.mfa;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TotpTest {

    /** Vecteur de test de la RFC 6238 (annexe B) : secret ASCII "12345678901234567890", T = 59 s. */
    @Test
    void matchesRfc6238TestVector() {
        String secret = Totp.base32("12345678901234567890".getBytes(StandardCharsets.US_ASCII));
        assertThat(secret).isEqualTo("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        // 94287082 sur 8 chiffres -> 287082 sur 6
        assertThat(Totp.code(secret, Totp.stepAt(59))).isEqualTo("287082");
    }

    @Test
    void acceptsClockDriftButRejectsReplay() {
        String secret = Totp.newSecret();
        long now = 1_000_000;
        assertThat(Totp.verify(secret, Totp.code(secret, now - 1), now, null)).hasValue(now - 1);
        assertThat(Totp.verify(secret, Totp.code(secret, now + 2), now, null)).isEmpty();
        assertThat(Totp.verify(secret, Totp.code(secret, now), now, now)).isEmpty();
        assertThat(Totp.verify(secret, "12345", now, null)).isEmpty();
    }

    @Test
    void secretCipherRoundTripUsesRandomIv() {
        SecretCipher cipher = new SecretCipher("ZGV2LW9ubHktbWZhLWtleS1ub3QtZm9yLXByb2QtISE=");
        String a = cipher.encrypt("SECRET");
        assertThat(a).isNotEqualTo(cipher.encrypt("SECRET"));
        assertThat(cipher.decrypt(a)).isEqualTo("SECRET");
    }
}
