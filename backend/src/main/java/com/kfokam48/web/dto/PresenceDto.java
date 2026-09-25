package com.kfokam48.web.dto;

import com.kfokam48.domain.PresenceJpa;

/** Réponse 201 du POST /api/presences — exactement {id, sessionId, etudiantId, source} (contrat). */
public record PresenceDto(Long id, Long sessionId, Long etudiantId, String source) {

    public static PresenceDto de(PresenceJpa presence) {
        return new PresenceDto(
                presence.getId(),
                presence.getSessionId(),
                presence.getEtudiantId(),
                presence.getSource());
    }
}
