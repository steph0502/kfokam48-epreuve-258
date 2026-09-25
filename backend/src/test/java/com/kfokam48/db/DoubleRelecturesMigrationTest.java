package com.kfokam48.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DoubleRelecturesMigrationTest {

    @Test
    void conserve_les_relectures_existantes_comme_premiere_affectation() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:migrationDoubleRelectures;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        source.setUser("sa");
        source.setPassword("");
        Flyway.configure().dataSource(source).target(MigrationVersion.fromVersion("2")).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(source);

        jdbc.update("""
                INSERT INTO session (id, titre, promotion_id, code, ouverture_at, expiration_at)
                VALUES (10000, 'Test migration', 1, 'MIG123', CURRENT_TIMESTAMP,
                        DATEADD('MINUTE', 15, CURRENT_TIMESTAMP))
                """);
        jdbc.update("""
                INSERT INTO exercice (id, session_id, etudiant_id, lien, statut, depose_at)
                VALUES (10000, 10000, 1, 'https://exemple.com/exercice', 'ASSIGNE', CURRENT_TIMESTAMP)
                """);
        jdbc.update("INSERT INTO relecture (id, exercice_id, relecteur_id) VALUES (10000, 10000, 2)");

        Flyway.configure().dataSource(source).load().migrate();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM relecture WHERE id = 10000", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT numero_relecteur FROM relecture WHERE id = 10000", Integer.class))
                .isEqualTo(1);
    }
}
