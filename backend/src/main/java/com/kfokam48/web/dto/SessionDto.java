package com.kfokam48.web.dto;

import com.kfokam48.domain.SessionJpa;

import java.time.Instant;

/**
 * Réponse du POST /api/sessions — exactement le schéma du contrat :
 * { id, code, ouvertureAt, expirationAt }. Aucune entité JPA exposée en JSON (B3).
 */
public record SessionDto(Long id, String code, Instant ouvertureAt, Instant expirationAt) {

    public static SessionDto de(SessionJpa session) {
        return new SessionDto(
                session.getId(),
                session.getCode(),
                session.getOuvertureAt(),
                session.getExpirationAt());
    }
}
