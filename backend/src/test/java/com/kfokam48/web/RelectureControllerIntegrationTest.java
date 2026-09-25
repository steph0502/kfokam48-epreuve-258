package com.kfokam48.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration (B6) de EF5 sur le POST /api/relectures/{id} du contrat,
 * dans un scénario réel complet : session (EF1) → présences (EF2) → dépôt (EF3,
 * assignation aléatoire lue en base) → relecture. Prouve aussi RG8, la correction
 * Q10/H9 (PUT), RG4, RG7 et H10. Sur H2, sans base locale.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RelectureControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

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

    /** Ouvre une session (EF1), fait entrer les étudiants donnés (EF2), renvoie l'id. */
    private long sessionAvecPresences(long... etudiantIds) throws Exception {
        MvcResult session = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Cours\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        long sessionId = Long.parseLong(valeurJson(session.getResponse().getContentAsString(), "id"));
        String code = valeurJson(session.getResponse().getContentAsString(), "code");
        for (long id : etudiantIds) {
            mockMvc.perform(post("/api/presences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"" + code + "\",\"etudiantId\":" + id + "}"))
                    .andExpect(status().isCreated());
        }
        return sessionId;
    }

    /** Dépose l'exercice de l'étudiant (EF3) et renvoie l'id de la relecture créée. */
    private long deposerEtLireRelecture(long sessionId, long etudiantId, String lien) throws Exception {
        MvcResult depot = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId
                                + ",\"lien\":\"" + lien + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long exerciceId = Long.parseLong(valeurJson(depot.getResponse().getContentAsString(), "id"));
        return jdbc.queryForObject("SELECT id FROM relecture WHERE exercice_id = ? ORDER BY numero_relecteur LIMIT 1", Long.class, exerciceId);
    }

    private long relecteurDe(long relectureId) {
        return jdbc.queryForObject("SELECT relecteur_id FROM relecture WHERE id = ?", Long.class, relectureId);
    }

    @Test
    void deux_relecteurs_distincts_rendent_avant_statut_RELU_et_doublon_409() throws Exception {
        long sessionId = sessionAvecPresences(1L, 2L, 3L);
        long premiere = deposerEtLireRelecture(sessionId, 1L, "https://gitlab.com/a/projet");
        long exerciceId = jdbc.queryForObject("SELECT exercice_id FROM relecture WHERE id = ?", Long.class, premiere);
        long seconde = jdbc.queryForObject("SELECT id FROM relecture WHERE exercice_id = ? AND numero_relecteur = 2", Long.class, exerciceId);
        long relecteur1 = relecteurDe(premiere);
        long relecteur2 = relecteurDe(seconde);
        assertThat(relecteur1).isNotEqualTo(relecteur2).isNotEqualTo(1L);

        mockMvc.perform(post("/api/relectures/" + premiere + "?etudiantId=" + relecteur1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":15,\"commentaire\":\"Bon travail.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(premiere))
                .andExpect(jsonPath("$.note").value(15))
                .andExpect(jsonPath("$.commentaire").value("Bon travail."))
                .andExpect(jsonPath("$.rendueAt").isString())
                .andExpect(jsonPath("$.[?(@.relecteurId)]").isEmpty());
        assertThat(jdbc.queryForObject("SELECT statut FROM exercice WHERE id = ?", String.class, exerciceId))
                .isEqualTo("ASSIGNE");

        mockMvc.perform(post("/api/relectures/" + seconde + "?etudiantId=" + relecteur2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":17,\"commentaire\":\"Bien réalisé.\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT statut FROM exercice WHERE id = ?", String.class, exerciceId))
                .isEqualTo("RELU");

        mockMvc.perform(post("/api/relectures/" + premiere + "?etudiantId=" + relecteur1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":18,\"commentaire\":\"Seconde tentative.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));
    }

    @Test
    void avis_deuxieme_provisoire_puis_moyenne_et_listes_anonymisees_RG19() throws Exception {
        long sessionId = sessionAvecPresences(1L, 2L, 3L);
        long premiere = deposerEtLireRelecture(sessionId, 1L, "https://gitlab.com/a/projet");
        long exerciceId = jdbc.queryForObject("SELECT exercice_id FROM relecture WHERE id = ?", Long.class, premiere);
        long seconde = jdbc.queryForObject("SELECT id FROM relecture WHERE exercice_id = ? AND numero_relecteur = 2", Long.class, exerciceId);
        long relecteur1 = relecteurDe(premiere);
        long relecteur2 = relecteurDe(seconde);

        mockMvc.perform(get("/api/relectures").param("etudiantId", String.valueOf(relecteur1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + premiere + ")]").isNotEmpty());
        mockMvc.perform(get("/api/etudiants/1/exercices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + exerciceId + ")]").isNotEmpty());

        mockMvc.perform(post("/api/relectures/" + premiere + "?etudiantId=" + relecteur1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":15,\"commentaire\":\"Avis 1.\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/relectures").param("etudiantId", String.valueOf(relecteur1)).param("rendue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + premiere + ")]").isNotEmpty());
        mockMvc.perform(get("/api/etudiants/1/exercices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + exerciceId + ")].noteRetenue").value(org.hamcrest.Matchers.hasItem(15.0)))
                .andExpect(jsonPath("$[?(@.id == " + exerciceId + ")].noteProvisoire").value(org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$[?(@.id == " + exerciceId + ")].evaluations[0].commentaire").value(org.hamcrest.Matchers.hasItem("Avis 1.")))
                .andExpect(jsonPath("$[?(@.id == " + exerciceId + ")].evaluations[0].relecteurId").doesNotExist());

        mockMvc.perform(post("/api/relectures/" + seconde + "?etudiantId=" + relecteur2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":17,\"commentaire\":\"Avis 2.\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/etudiants/1/exercices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + exerciceId + ")].noteRetenue").value(org.hamcrest.Matchers.hasItem(16.0)))
                .andExpect(jsonPath("$[?(@.id == " + exerciceId + ")].noteProvisoire").value(org.hamcrest.Matchers.hasItem(false)))
                .andExpect(jsonPath("$[?(@.id == " + exerciceId + ")].evaluations[1].note").value(org.hamcrest.Matchers.hasItem(17)));
    }

    @Test
    void correction_PUT_200_avant_cloture_Q10_H9() throws Exception {
        long sessionId = sessionAvecPresences(1L, 2L, 3L);
        long relectureId = deposerEtLireRelecture(sessionId, 1L, "https://gitlab.com/a/projet");
        long relecteur = relecteurDe(relectureId);

        mockMvc.perform(post("/api/relectures/" + relectureId + "?etudiantId=" + relecteur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":15,\"commentaire\":\"Première version.\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/relectures/" + relectureId + "?etudiantId=" + relecteur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":17,\"commentaire\":\"Version affinée.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(17));
    }

    @Test
    void auto_relecture_403_RG4_meme_en_etant_auteur() throws Exception {
        long sessionId = sessionAvecPresences(1L, 2L, 3L);
        long relectureId = deposerEtLireRelecture(sessionId, 1L, "https://gitlab.com/a/projet");

        // L'auteur (1) tente de rendre la relecture de son propre exercice.
        mockMvc.perform(post("/api/relectures/" + relectureId + "?etudiantId=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":10,\"commentaire\":\"Je me note moi-même.\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_RELECTURE"));
    }

    @Test
    void note_invalide_400_NOTE_INVALIDE_RG7() throws Exception {
        long sessionId = sessionAvecPresences(1L, 2L, 3L);
        long relectureId = deposerEtLireRelecture(sessionId, 1L, "https://gitlab.com/a/projet");
        long relecteur = relecteurDe(relectureId);

        mockMvc.perform(post("/api/relectures/" + relectureId + "?etudiantId=" + relecteur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":21,\"commentaire\":\"Hors bornes.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    void correction_apres_cloture_409_Q15_mais_soumission_initiale_200_H10() throws Exception {
        long sessionId = sessionAvecPresences(1L, 2L, 3L);
        long rendue = deposerEtLireRelecture(sessionId, 1L, "https://gitlab.com/a/projet");
        long enRetard = deposerEtLireRelecture(sessionId, 2L, "https://gitlab.com/b/projet");

        mockMvc.perform(post("/api/relectures/" + rendue + "?etudiantId=" + relecteurDe(rendue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":12,\"commentaire\":\"Rendue avant clôture.\"}"))
                .andExpect(status().isOk());

        // Clôture de la session (le endpoint EF7 arrive au ticket #6) : écriture directe.
        jdbc.update("UPDATE session SET cloture_at = CURRENT_TIMESTAMP WHERE id = ?", sessionId);

        mockMvc.perform(put("/api/relectures/" + rendue + "?etudiantId=" + relecteurDe(rendue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":20,\"commentaire\":\"Trop tard.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CORRECTION_INTERDITE"));

        // H10 : la soumission initiale d'une relecture assignée reste possible.
        mockMvc.perform(post("/api/relectures/" + enRetard + "?etudiantId=" + relecteurDe(enRetard))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":10,\"commentaire\":\"Tardive mais recevable.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(10));
    }
}
