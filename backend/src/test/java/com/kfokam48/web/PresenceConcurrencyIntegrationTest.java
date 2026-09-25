package com.kfokam48.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reproduit le signalement de l’issue #25 : deux présences valides sont
 * enregistrées simultanément pendant que chaque requête retente plusieurs
 * assignations d’exercices en attente.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PresenceConcurrencyIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void deux_etudiants_concurrents_conservent_leurs_presences() throws Exception {
        MvcResult ouverture = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Cours concurrent\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        String corpsSession = ouverture.getResponse().getContentAsString();
        long sessionId = Long.parseLong(valeurJson(corpsSession, "id"));
        String code = valeurJson(corpsSession, "code");

        // Plusieurs exercices sans relecteur sont présents avant les deux appels.
        // Cela exerce la branche de réassignation exécutée après chaque présence.
        List<Long> auteurs = ajouterAuteurs(24);
        for (long auteurId : auteurs) {
            MvcResult depot = mockMvc.perform(post("/api/exercices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + auteurId
                                    + ",\"lien\":\"https://gitlab.com/auteur/projet\"}"))
                    .andExpect(status().isCreated())
                    .andReturn();
            assertThat(valeurJson(depot.getResponse().getContentAsString(), "statut"))
                    .isEqualTo("EN_ATTENTE");
        }

        CountDownLatch depart = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> etudiantDeux = pool.submit(() -> marquerApresDepart(depart, code, 2L));
            Future<MvcResult> etudiantTrois = pool.submit(() -> marquerApresDepart(depart, code, 3L));
            depart.countDown();

            MvcResult resultatDeux = etudiantDeux.get(15, TimeUnit.SECONDS);
            MvcResult resultatTrois = etudiantTrois.get(15, TimeUnit.SECONDS);

            assertThat(resultatDeux.getResponse().getStatus()).isEqualTo(201);
            assertThat(resultatTrois.getResponse().getStatus()).isEqualTo(201);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM presence WHERE session_id = ? AND etudiant_id IN (2, 3)",
                    Integer.class, sessionId)).isEqualTo(2);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private MvcResult marquerApresDepart(CountDownLatch depart, String code, long etudiantId) throws Exception {
        if (!depart.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Le départ simultané des requêtes n’a pas été déclenché.");
        }
        return mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}"))
                .andReturn();
    }

    private List<Long> ajouterAuteurs(int nombre) {
        List<Long> ids = new ArrayList<>();
        for (int index = 0; index < nombre; index++) {
            String nom = "Auteur concurrent " + index;
            jdbc.update("INSERT INTO etudiant (nom, promotion_id) VALUES (?, 1)", nom);
            ids.add(jdbc.queryForObject(
                    "SELECT id FROM etudiant WHERE nom = ?", Long.class, nom));
        }
        return ids;
    }

    private static String valeurJson(String corps, String champ) {
        String marqueur = "\"" + champ + "\":";
        int debut = corps.indexOf(marqueur);
        if (debut < 0) {
            return null;
        }
        debut += marqueur.length();
        if (corps.charAt(debut) == '"') {
            return corps.substring(debut + 1, corps.indexOf('"', debut + 1));
        }
        int fin = debut;
        while (fin < corps.length() && corps.charAt(fin) != ',' && corps.charAt(fin) != '}') {
            fin++;
        }
        return corps.substring(debut, fin).trim();
    }
}
