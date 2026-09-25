package com.kfokam48.codegenerator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Codes de présence : 6 caractères alphanumériques majuscules (hypothèse H6, RG18),
 * hors caractères ambigus (O/0, I/1) pour rester lisibles sur un téléphone.
 */
@Component
public class CodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LONGUEUR = 6;

    private final SecureRandom random = new SecureRandom();

    public String nouveauCode() {
        StringBuilder sb = new StringBuilder(LONGUEUR);
        for (int i = 0; i < LONGUEUR; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
