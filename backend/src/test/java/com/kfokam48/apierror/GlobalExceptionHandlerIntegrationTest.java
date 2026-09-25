package com.kfokam48.apierror;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration (B6) : le gestionnaire centralisé d'erreurs produit bien le
 * format imposé {code, message} — un endpoint 404 et un endpoint métier qui échoue.
 * Tourne sur H2 en mémoire, sans base locale (testé sur poste vierge).
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @TestConfiguration
    static class EndpointDeTest {
        @RestController
        static class Probe {
            @GetMapping("/api/test-erreur-metier")
            public ApiError erreurMetier() {
                throw new ApiException(
                        org.springframework.http.HttpStatus.GONE,
                        "CODE_EXPIRE",
                        "Le code de présence a expiré.");
            }
        }
    }

    @Test
    void erreurMetierAuFormatDuContrat() throws Exception {
        mockMvc.perform(get("/api/test-erreur-metier"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"))
                .andExpect(jsonPath("$.message").value("Le code de présence a expiré."));
    }

    @Test
    void routeInconnueAuFormatDuContrat() throws Exception {
        mockMvc.perform(get("/api/nimporte-quoi"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
