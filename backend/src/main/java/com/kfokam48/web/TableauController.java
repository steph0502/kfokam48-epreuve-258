package com.kfokam48.web;

import com.kfokam48.service.TableauService;
import com.kfokam48.web.dto.TableauLigneDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * EF6 — GET /api/tableau?promotionId= → 200 [{etudiantId, nom, presences,
 * exercicesDeposes, moyenne, relecturesEnAttente}] (schéma contrat exact).
 * promotion inconnue → 404 PROMOTION_INCONNUE. Aucune logique métier ici (B3),
 * et la moyenne vient de l'API — jamais recalculée côté front (F3).
 */
@RestController
public class TableauController {

    private final TableauService tableauService;

    public TableauController(TableauService tableauService) {
        this.tableauService = tableauService;
    }

    @GetMapping("/api/tableau")
    public List<TableauLigneDto> tableau(@RequestParam("promotionId") Long promotionId) {
        return tableauService.tableau(promotionId);
    }
}
