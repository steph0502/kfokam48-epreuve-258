package com.kfokam48.config;

import com.kfokam48.codegenerator.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Données de démonstration imposées par le sujet : le correcteur doit ouvrir
 * une application utilisable. V2 charge le référentiel (promotion, étudiants) ;
 * ici, une session de démonstration est créée au démarrage avec un code frais —
 * le code expirant au bout de 15 minutes (RG1), il ne peut pas être figé en migration.
 */
@Configuration
public class DonneesDemoConfig {

    private static final Logger log = LoggerFactory.getLogger(DonneesDemoConfig.class);
    private static final Duration DUREE_CODE = Duration.ofMinutes(15); // RG1

    @Bean
    ApplicationRunner sessionDemo(JdbcTemplate jdbc, CodeGenerator generateur) {
        return args -> {
            Integer sessions = jdbc.queryForObject("SELECT count(*) FROM session", Integer.class);
            if (sessions != null && sessions > 0) {
                return; // déjà créée à un démarrage précédent — pas de doublon
            }
            Instant ouverture = Instant.now();
            Instant expiration = ouverture.plus(DUREE_CODE);
            String code = genererCodeUnique(jdbc, generateur);
            jdbc.update(
                    "INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at) VALUES (?, ?, ?, ?, ?)",
                    "Session de démonstration", 1L, code,
                    Timestamp.from(ouverture), Timestamp.from(expiration));
            log.info("Session de démonstration créée — code de présence : {} (valable 15 minutes, RG1)", code);
        };
    }

    private String genererCodeUnique(JdbcTemplate jdbc, CodeGenerator generateur) {
        for (int i = 0; i < 20; i++) {
            String candidat = generateur.nouveauCode();
            Integer collisions = jdbc.queryForObject(
                    "SELECT count(*) FROM session WHERE code = ?", Integer.class, candidat);
            if (collisions != null && collisions == 0) {
                return candidat;
            }
        }
        throw new IllegalStateException("Impossible de générer un code de session unique (RG18)");
    }
}
