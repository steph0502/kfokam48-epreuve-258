package com.kfokam48.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps du POST /api/exercices — le contrat exige {sessionId, etudiantId, lien}. */
public record DeposerExerciceRequest(
        @NotNull Long sessionId,
        @NotNull Long etudiantId,
        @NotBlank String lien) {
}
