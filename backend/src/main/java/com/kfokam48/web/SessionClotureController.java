package com.kfokam48.web;

import com.kfokam48.domain.SessionJpa;
import com.kfokam48.service.SessionService;
import com.kfokam48.web.dto.ClotureSessionDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EF7 — POST /api/sessions/{id}/cloture → 200 {id, clotureAt}.
 * Après clôture : présence refusée (RG2), dépôt refusé (RG10), correction
 * de note refusée (Q15) ; les exercices non relus restent visibles (RG9, Q11).
 * Aucune logique métier ici (B3).
 */
@RestController
public class SessionClotureController {

    private final SessionService sessionService;

    public SessionClotureController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/api/sessions/{id}/cloture")
    public ClotureSessionDto cloturer(@PathVariable Long id) {
        SessionJpa session = sessionService.cloturer(id);
        return ClotureSessionDto.de(session);
    }
}
