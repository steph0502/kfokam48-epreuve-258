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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration (B6) de EF7 : la clôture active les gardes en aval,
 * prouvées via l'API — présence refusée (RG2), dépôt refusé (RG10), correction
 * de note refusée (Q15) ; la soumission initiale d'une relecture assignée
 * avant la clôture reste possible (H10). Sur H2, sans base locale.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClotureControllerIntegrationTest {

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

    private record RelectureCible(long relectureId, long relecteurId) {
    }

    /**
     * Scénario : session, 3 présents, un exercice relu (note rendue) et un
     * second exercice assigné mais pas encore relu (la cible H10).
     */
    private record Contexte(long sessionId, String code, long dejaPresent,
                            RelectureCible rendue, RelectureCible enAttente) {
    }

    private Contexte scenarioComplet() throws Exception {
        jdbc.update("INSERT INTO promotion (nom) VALUES ('Promo test cloture')");
        long promotionId = jdbc.queryForObject(
                "SELECT id FROM promotion WHERE nom = 'Promo test cloture'", Long.class);
        for (String nom : new String[]{"ClosA Auteur", "ClosB Pair", "ClosC Troisieme"}) {
            jdbc.update("INSERT INTO etudiant (nom, promotion_id) VALUES (?, ?)", nom, promotionId);
        }
        long auteur = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, "ClosA Auteur");
        long pair = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, "ClosB Pair");
        long troisieme = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, "ClosC Troisieme");

        MvcResult session = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Cours\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        String corpsSession = session.getResponse().getContentAsString();
        long sessionId = Long.parseLong(valeurJson(corpsSession, "id"));
        String code = valeurJson(corpsSession, "code");

        for (long etudiant : new long[]{auteur, pair, troisieme}) {
            mockMvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant + "}"))
                    .andExpect(status().isCreated());
        }

        // Premier exercice : relu (la note est rendue avant la clôture).
        long relectureRendue = deposerEtAssigner(sessionId, auteur);
        long relecteur1 = relecteurDe(relectureRendue);
        mockMvc.perform(post("/api/relectures/" + relectureRendue + "?etudiantId=" + relecteur1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":14,\"commentaire\":\"Rendue.\"}"))
                .andExpect(status().isOk());

        // Second exercice : assigné mais pas encore relu — cible H10.
        long relectureEnAttente = deposerEtAssigner(sessionId, pair);

        return new Contexte(sessionId, code, troisieme,
                new RelectureCible(relectureRendue, relecteur1),
                new RelectureCible(relectureEnAttente, relecteurDe(relectureEnAttente)));
    }

    private long deposerEtAssigner(long sessionId, long auteurId) throws Exception {
        MvcResult depot = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + auteurId
                                + ",\"lien\":\"https://gitlab.com/x/projet\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long exerciceId = Long.parseLong(valeurJson(depot.getResponse().getContentAsString(), "id"));
        return jdbc.queryForObject("SELECT id FROM relecture WHERE exercice_id = ?", Long.class, exerciceId);
    }

    private long relecteurDe(long relectureId) {
        return jdbc.queryForObject("SELECT relecteur_id FROM relecture WHERE id = ?", Long.class, relectureId);
    }

    @Test
    void cloture_200_puis_gardes_RG2_RG10_Q15_actives_H10_toujours_ouvert() throws Exception {
        Contexte contexte = scenarioComplet();

        mockMvc.perform(post("/api/sessions/" + contexte.sessionId() + "/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contexte.sessionId()))
                .andExpect(jsonPath("$.clotureAt").isString());

        // Double clôture refusée.
        mockMvc.perform(post("/api/sessions/" + contexte.sessionId() + "/cloture"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEJA_CLOTUREE"));

        // RG2 : plus de présence, même avec le code encore non expiré.
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + contexte.code() + "\",\"etudiantId\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));

        // RG10 : plus de dépôt.
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + contexte.sessionId()
                                + ",\"etudiantId\":4,\"lien\":\"https://gitlab.com/d/projet\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));

        // Q15 : la correction de la note rendue est fermée.
        mockMvc.perform(put("/api/relectures/" + contexte.rendue().relectureId()
                        + "?etudiantId=" + contexte.rendue().relecteurId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":18,\"commentaire\":\"Trop tard.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CORRECTION_INTERDITE"));

        // H10 : la soumission initiale d'une relecture assignée avant clôture reste possible.
        mockMvc.perform(post("/api/relectures/" + contexte.enAttente().relectureId()
                        + "?etudiantId=" + contexte.enAttente().relecteurId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":11,\"commentaire\":\"Tardive mais recevable.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(11));
    }

    @Test
    void cloture_session_inconnue_404() throws Exception {
        mockMvc.perform(post("/api/sessions/-12345/cloture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }
}
