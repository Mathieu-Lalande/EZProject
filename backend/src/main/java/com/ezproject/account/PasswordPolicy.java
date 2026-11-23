package com.ezproject.account;

import java.security.SecureRandom;

import org.springframework.util.StringUtils;

import com.ezproject.common.BusinessRuleException;

/** Règles de mot de passe (longueur, à la manière d'OWASP ASVS : la longueur prime sur la complexité). */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordPolicy() {
    }

    public static void check(String password, String email) {
        if (!StringUtils.hasText(password) || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BusinessRuleException("Le mot de passe doit contenir entre " + MIN_LENGTH + " et " + MAX_LENGTH
                    + " caractères");
        }
        if (email != null && password.equalsIgnoreCase(email)) {
            throw new BusinessRuleException("Le mot de passe ne doit pas être votre email");
        }
    }

    /** Mot de passe provisoire aléatoire (cryptographiquement sûr, sans caractères ambigus). */
    public static String temporary() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
