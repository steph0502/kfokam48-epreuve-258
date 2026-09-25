package com.kfokam48.web;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration (B6) de EF11 sur le GET /api/etudiants du contrat :
 * liste triée par nom pour choisir le sien (Q1), 404 promotion inconnue.
 * Sur H2, sans base locale.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EtudiantControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void liste_200_triee_par_nom_pour_choisir_son_nom_Q1() throws Exception {
        jdbc.update("INSERT INTO promotion (nom) VALUES ('Promo test etudiants')");
        long promotionId = jdbc.queryForObject(
                "SELECT id FROM promotion WHERE nom = 'Promo test etudiants'", Long.class);
        jdbc.update("INSERT INTO etudiant (nom, promotion_id) VALUES ('Zoé Exemple', ?)", promotionId);
        jdbc.update("INSERT INTO etudiant (nom, promotion_id) VALUES ('Adam Exemple', ?)", promotionId);

        mockMvc.perform(get("/api/etudiants").param("promotionId", String.valueOf(promotionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].nom").value("Adam Exemple")) // tri par nom
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[1].nom").value("Zoé Exemple"));
    }

    @Test
    void promotion_inconnue_404_PROMOTION_INCONNUE() throws Exception {
        mockMvc.perform(get("/api/etudiants").param("promotionId", "-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }
}
