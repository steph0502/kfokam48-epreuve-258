package com.kfokam48.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps du POST /api/presences — le contrat exige {code, etudiantId}. */
public record MarquerPresenceRequest(
        @NotBlank String code,
        @NotNull Long etudiantId) {
}
