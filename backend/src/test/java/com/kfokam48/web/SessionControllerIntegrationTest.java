package com.kfokam48.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration (B6) de EF1 sur le POST /api/sessions du contrat :
 * 201 avec exactement {id, code, ouvertureAt, expirationAt} et expirationAt =
 * ouvertureAt + 15 min (RG1) ; 400 champ manquant. Tourne sur H2, sans base locale.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    /** Renvoie une valeur textuelle du corps JSON sans dépendre d'une librairie de parsing. */
    private static ResultMatcher champJson(String champ, StringBuilder sortie) {
        return result -> {
            String corps = result.getResponse().getContentAsString();
            String marqueur = "\"" + champ + "\":";
            int debut = corps.indexOf(marqueur) + marqueur.length();
            int fin = corps.indexOf("\"", debut + 1);
            sortie.append(corps, debut + 1, fin);
        };
    }

    @Test
    void ouverture_201_au_schema_du_contrat_RG1() throws Exception {
        StringBuilder ouvertureAt = new StringBuilder();
        StringBuilder expirationAt = new StringBuilder();

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Algorithmique\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.hasLength(6)))
                .andExpect(champJson("ouvertureAt", ouvertureAt))
                .andExpect(champJson("expirationAt", expirationAt));

        Instant ouverture = Instant.parse(ouvertureAt.toString());
        Instant expiration = Instant.parse(expirationAt.toString());
        assertThat(expiration).isEqualTo(ouverture.plus(Duration.ofMinutes(15)));
    }

    @Test
    void champ_manquant_400_au_format_du_contrat() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"promotionId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"))
                .andExpect(jsonPath("$.message").isString());
    }
}
