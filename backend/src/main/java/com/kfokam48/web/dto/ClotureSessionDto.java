package com.kfokam48.web.dto;

import com.kfokam48.domain.SessionJpa;

import java.time.Instant;

/** Réponse 200 du POST /api/sessions/{id}/cloture — {id, clotureAt} (contrat, opération ajoutée). */
public record ClotureSessionDto(Long id, Instant clotureAt) {

    public static ClotureSessionDto de(SessionJpa session) {
        return new ClotureSessionDto(session.getId(), session.getClotureAt());
    }
}
