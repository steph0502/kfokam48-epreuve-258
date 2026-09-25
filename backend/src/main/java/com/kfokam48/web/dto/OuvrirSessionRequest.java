package com.kfokam48.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps du POST /api/sessions — le contrat exige {titre, promotionId}. */
public record OuvrirSessionRequest(
        @NotBlank String titre,
        @NotNull Long promotionId) {
}
