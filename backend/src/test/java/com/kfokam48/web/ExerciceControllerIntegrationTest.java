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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration (B6) de EF3/EF4 sur le POST /api/exercices du contrat.
 * Scénario réel complet : ouverture de session (EF1) → présences (EF2) → dépôt.
 * Prouve RG14 de bout en bout : sans autre présent l'exercice reste EN_ATTENTE,
 * la présence suivante déclenche l'assignation (ASSIGNE en base). Sur H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExerciceControllerIntegrationTest {

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

    /** Ouvre une session (EF1) et fait entrer un étudiant (EF2). Renvoie {id, code}. */
    private String[] ouvrirSessionEtFaireEntrer(long etudiantId) throws Exception {
        MvcResult session = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Cours\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        String corps = session.getResponse().getContentAsString();
        String id = valeurJson(corps, "id");
        String code = valeurJson(corps, "code");
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}"))
                .andExpect(status().isCreated());
        return new String[]{id, code};
    }

    private String deposer(long sessionId, long etudiantId, String lien) throws Exception {
        return mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId
                                + ",\"lien\":\"" + lien + "\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void depot_201_EN_ATTENTE_sans_autre_present_puis_ASSIGNE_apres_nouvelle_presence_RG14() throws Exception {
        String[] session = ouvrirSessionEtFaireEntrer(1L);
        long sessionId = Long.parseLong(session[0]);

        // L'étudiant 1, seul présent, dépose : aucun pair → EN_ATTENTE (RG14).
        String corps = deposer(sessionId, 1L, "https://gitlab.com/a/projet");
        assertThat(valeurJson(corps, "statut")).isEqualTo("EN_ATTENTE");
        long exerciceId = Long.parseLong(valeurJson(corps, "id"));

        // L'étudiant 2 marque sa présence (EF2) : l'assignation est retentée (RG14).
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + session[1] + "\",\"etudiantId\":2}"))
                .andExpect(status().isCreated());

        String statut = jdbc.queryForObject(
                "SELECT statut FROM exercice WHERE id = ?", String.class, exerciceId);
        assertThat(statut).isEqualTo("ASSIGNE");
    }

    @Test
    void depot_201_ASSIGNE_quand_un_pair_est_deja_present_EF4() throws Exception {
        String[] session = ouvrirSessionEtFaireEntrer(1L);

        // Un pair (3) entre avant le dépôt → assignation immédiate, l'auteur 2 est exclu (RG4).
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + session[1] + "\",\"etudiantId\":3}"))
                .andExpect(status().isCreated());

        String corps = deposer(Long.parseLong(session[0]), 2L, "https://gitlab.com/b/projet");
        assertThat(valeurJson(corps, "statut")).isEqualTo("ASSIGNE");
    }

    @Test
    void second_depot_409_EXERCICE_DEJA_DEPOSE_RG17() throws Exception {
        String[] session = ouvrirSessionEtFaireEntrer(1L);

        deposer(Long.parseLong(session[0]), 1L, "https://gitlab.com/a/projet");

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + session[0] + ",\"etudiantId\":1,\"lien\":\"https://gitlab.com/a/autre\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void lien_invalide_400_LIEN_INVALIDE() throws Exception {
        String[] session = ouvrirSessionEtFaireEntrer(1L);

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + session[0] + ",\"etudiantId\":1,\"lien\":\"pas-une-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"))
                .andExpect(jsonPath("$.message").isString());
    }
}
