package com.ezproject.mfa;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ezproject.common.BusinessRuleException;

/**
 * Chiffrement des secrets TOTP au repos (AES-256-GCM, IV aléatoire de 96 bits).
 * Clé : {@code app.mfa.encryption-key} = 32 octets encodés en Base64 (variable MFA_ENCRYPTION_KEY),
 * jamais stockée en base : une fuite de la base seule ne permet pas de générer des codes.
 */
@Component
public class SecretCipher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec key;

    public SecretCipher(@Value("${app.mfa.encryption-key:}") String base64Key) {
        SecretKeySpec k = null;
        if (base64Key != null && !base64Key.isBlank()) {
            byte[] raw = Base64.getDecoder().decode(base64Key.trim());
            if (raw.length != 32) {
                throw new IllegalStateException("MFA_ENCRYPTION_KEY doit faire 32 octets (Base64)");
            }
            k = new SecretKeySpec(raw, "AES");
        }
        this.key = k;
    }

    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(String plain) {
        requireKey();
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement impossible", e);
        }
    }

    public String decrypt(String encoded) {
        requireKey();
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
            return new String(cipher.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Déchiffrement impossible (clé MFA changée ?)", e);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new BusinessRuleException("Double authentification non configurée sur ce serveur (MFA_ENCRYPTION_KEY)");
        }
    }
}
