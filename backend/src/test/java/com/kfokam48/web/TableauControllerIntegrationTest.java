package com.kfokam48.web;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration (B6) de EF6 sur le GET /api/tableau du contrat, après un
 * scénario réel complet : session (EF1) → présences (EF2) → dépôt (EF3) →
 * relecture rendue (EF5). Le test crée sa propre promotion et ses propres
 * étudiants (neufs) : les compteurs du tableau sont cumulatifs par étudiant,
 * les assertions restent donc déterministes même si d'autres tests ont déjà
 * utilisé la promotion de démonstration. Prouve la moyenne (RG15), les
 * compteurs (Q16), le 404 PROMOTION_INCONNUE et RG13. Sur H2, sans base locale.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TableauControllerIntegrationTest {

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

    /** Promotion et étudiants neufs, dédiés à ce test. Renvoie leurs ids [auteur, pair, isolé]. */
    private long[] promotionEtudiantsNeufs() {
        jdbc.update("INSERT INTO promotion (nom) VALUES ('Promo test tableau')");
        Long promotionId = jdbc.queryForObject(
                "SELECT id FROM promotion WHERE nom = 'Promo test tableau'", Long.class);
        String[] noms = {"TestA Auteur", "TestB Pair", "TestC Isole"};
        for (String nom : noms) {
            jdbc.update("INSERT INTO etudiant (nom, promotion_id) VALUES (?, ?)", nom, promotionId);
        }
        long auteur = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, noms[0]);
        long pair = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, noms[1]);
        long isole = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, noms[2]);
        return new long[]{promotionId, auteur, pair, isole};
    }

    @Test
    void tableau_200_apres_scenario_complet_moyenne_RG15_et_compteurs_Q16() throws Exception {
        long[] donnees = promotionEtudiantsNeufs();
        long promotionId = donnees[0];
        long auteur = donnees[1];
        long pair = donnees[2];

        // EF1 + EF2 : l'auteur et le pair sont présents.
        MvcResult session = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Cours\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        String corpsSession = session.getResponse().getContentAsString();
        String code = valeurJson(corpsSession, "code");
        mockMvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"etudiantId\":" + auteur + "}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"etudiantId\":" + pair + "}"))
                .andExpect(status().isCreated());

        // EF3 : l'auteur dépose — le relecteur assigné est forcément le pair.
        MvcResult depot = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + valeurJson(corpsSession, "id")
                                + ",\"etudiantId\":" + auteur + ",\"lien\":\"https://gitlab.com/a/projet\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long exerciceId = Long.parseLong(valeurJson(depot.getResponse().getContentAsString(), "id"));
        long relectureId = jdbc.queryForObject("SELECT id FROM relecture WHERE exercice_id = ?", Long.class, exerciceId);

        // EF5 : le pair rend sa relecture (note 12).
        mockMvc.perform(post("/api/relectures/" + relectureId + "?etudiantId=" + pair)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":12,\"commentaire\":\"Bien.\"}"))
                .andExpect(status().isOk());

        // EF6 : le tableau reflète exactement ce scénario, et rien d'autre.
        String corpsTableau = mockMvc.perform(get("/api/tableau").param("promotionId", String.valueOf(promotionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(3)))
                .andExpect(jsonPath("$[?(@.etudiantId == " + auteur + ")].presences").value(Matchers.hasItem(1)))
                .andExpect(jsonPath("$[?(@.etudiantId == " + auteur + ")].exercicesDeposes").value(Matchers.hasItem(1)))
                .andExpect(jsonPath("$[?(@.etudiantId == " + auteur + ")].moyenne").value(Matchers.hasItem(12.0)))
                .andExpect(jsonPath("$[?(@.etudiantId == " + pair + ")].moyenne").value(Matchers.hasItem(Matchers.nullValue()))) // RG15
                .andExpect(jsonPath("$[?(@.etudiantId == " + pair + ")].relecturesEnAttente").value(Matchers.hasItem(0)))
                .andExpect(jsonPath("$[?(@.etudiantId == " + donnees[3] + ")].presences").value(Matchers.hasItem(0)))
                .andExpect(jsonPath("$[?(@.etudiantId == " + donnees[3] + ")].exercicesDeposes").value(Matchers.hasItem(0)))
                .andReturn().getResponse().getContentAsString();
        // RG13 : aucune identité de relecteur dans le tableau.
        assertThat(corpsTableau).doesNotContain("relecteurId");
    }

    @Test
    void promotion_inconnue_404_PROMOTION_INCONNUE() throws Exception {
        // Id négatif : jamais généré par IDENTITY, le test reste déterministe
        // même si d'autres tests ont créé des promotions entre-temps.
        mockMvc.perform(get("/api/tableau").param("promotionId", "-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").isString());
    }
}
