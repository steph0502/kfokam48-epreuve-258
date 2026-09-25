package com.kfokam48.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration (B6) de EF2 sur le POST /api/presences du contrat.
 * La session est ouverte via l'API (EF1) pour obtenir un vrai code — les deux
 * endpoints du contrat s'enchaînent comme dans le cas d'usage réel. Sur H2, sans base locale.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PresenceControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    /** Extrait une valeur (nombre ou chaîne) d'un petit corps JSON plat. */
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

    /** Ouvre une session via EF1 et renvoie {id, code}. */
    private String[] ouvrirSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Cours de test\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        String corps = result.getResponse().getContentAsString();
        return new String[]{valeurJson(corps, "id"), valeurJson(corps, "code")};
    }

    @Test
    void presence_201_au_schema_du_contrat() throws Exception {
        String[] session = ouvrirSession();

        MvcResult result = mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + session[1] + "\",\"etudiantId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.etudiantId").value(1))
                .andExpect(jsonPath("$.source").value("ETUDIANT"))
                .andReturn();

        assertThat(valeurJson(result.getResponse().getContentAsString(), "sessionId"))
                .isEqualTo(session[0]);
    }

    @Test
    void seconde_presence_409_DEJA_PRESENT_RG16() throws Exception {
        String[] session = ouvrirSession();
        String corps = "{\"code\":\"" + session[1] + "\",\"etudiantId\":2}";

        mockMvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void code_inconnu_400_CODE_INCONNU() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ZZZZZZ\",\"etudiantId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void code_expire_410_CODE_EXPIRE_RG1() throws Exception {
        String[] session = ouvrirSession();
        long sessionId = Long.parseLong(session[0]);

        // Le code est repoussé dans le passé (RG1) — écriture committée immédiatement.
        new TransactionTemplate(transactionManager).executeWithoutResult(etat ->
                jdbc.update("UPDATE session SET expiration_at = ? WHERE id = ?",
                        Instant.now().minusSeconds(2), sessionId));

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + session[1] + "\",\"etudiantId\":3}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"))
                .andExpect(jsonPath("$.message").isString());
    }
}
